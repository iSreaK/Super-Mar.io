package fr.julie.mariosprint.data

import fr.julie.mariosprint.data.local.FriendsLocalStore
import fr.julie.mariosprint.data.local.SessionStore
import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.data.repository.*
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.ui.FakeAccounts
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class FriendsDataTest {
    private lateinit var server: MockWebServer
    private lateinit var local: MemoryFriendsStore
    private lateinit var accounts: FakeAccounts
    private lateinit var repository: CachedFriendsRepository
    private lateinit var source: SupabaseFriendsSource
    @Before fun prepare() {
        server = MockWebServer().apply { start() }
        local = MemoryFriendsStore()
        accounts = FakeAccounts().apply { value.value = PlayerAccount("alice", "Alice") }
        source = SupabaseFriendsSource(SupabaseClient.create(server.url("/").toString(), "sb_publishable_test"),
            object : AccessTokenProvider { override suspend fun tokenFor(uid: String, rejectedToken: String?) = "token-$uid" })
        repository = CachedFriendsRepository(local, source, object : FriendsRealtimeSource {
            override fun events(uid: String) = emptyFlow<FriendsLiveEvent>()
        }, accounts, nowMs = { 1_000 })
    }
    @After fun cleanup() { server.shutdown() }
    private fun response(body: String, status: Int = 200) = MockResponse().setResponseCode(status)
        .setHeader("Content-Type", "application/json").setBody(body)
    private fun snapshot(uid: String = "alice", relationStatus: String = "incoming", revision: Long = 1) =
        """{"viewer_uid":"$uid","friend_code":"ABCDEF123456","revision":$revision,"relations":[{"id":"request","uid":"bob","display_name":"Bob","skin_id":"toad","status":"$relationStatus"}]}"""
    private fun request() = checkNotNull(server.takeRequest(3, TimeUnit.SECONDS))

    @Test fun snapshotUsesTheJwtAndCountsNewIncomingRequestsOnlyOnce() = runBlocking {
        server.enqueue(response(snapshot())); server.enqueue(response(snapshot()))
        assertEquals(1, repository.refresh("alice")); assertEquals(0, repository.refresh("alice"))
        val request = request()
        assertEquals("/rest/v1/rpc/get_friend_snapshot", request.path)
        assertEquals("Bearer token-alice", request.getHeader("Authorization"))
        assertEquals("sb_publishable_test", request.getHeader("apikey"))
        assertEquals(0L, request.body.size)
        assertEquals(FriendshipStatus.INCOMING, repository.observe("alice").first().relations.single().status)
        assertTrue(repository.observe("bob").first().relations.isEmpty())
    }
    @Test fun friendCodeIsNormalizedAndNoCallerUidIsSent() = runBlocking {
        server.enqueue(response(snapshot(relationStatus = "outgoing")))
        repository.execute("alice", FriendCommand.SendByCode("abcd - ef12 - 3456"))
        val body = SupabaseClient.json.parseToJsonElement(request().body.readUtf8()).jsonObject
        assertEquals("ABCDEF123456", body["p_code"]?.jsonPrimitive?.content)
        assertEquals("send", body["p_action"]?.jsonPrimitive?.content)
        assertEquals(setOf("p_action", "p_target_uid", "p_code", "p_relation_id"), body.keys)
        assertEquals(FriendshipStatus.OUTGOING, repository.observe("alice").first().relations.single().status)
    }
    @Test fun everyCacheWriterEmitsNewRequestsOnlyAfterPersistence() = runBlocking {
        val notices = mutableListOf<IncomingFriendRequests>()
        val observer = launch(start = CoroutineStart.UNDISPATCHED) { repository.incomingRequests.collect { notice ->
            assertEquals(FriendshipStatus.INCOMING, repository.observe(notice.viewerUid).first().relations.single().status)
            notices += notice
        } }
        try {
            server.enqueue(response(snapshot()))
            repository.execute("alice", FriendCommand.SendToPlayer("charlie"))
            server.enqueue(response(snapshot()))
            repository.refresh("alice")
            yield()
            assertEquals(listOf(IncomingFriendRequests("alice", 1)), notices)
        } finally { observer.cancelAndJoin() }
    }
    @Test fun everyResponseActionUsesOnlyItsRelationshipId() = runBlocking {
        val commands = listOf(FriendCommand.Accept("r1"), FriendCommand.Decline("r1"), FriendCommand.Cancel("r1"), FriendCommand.Remove("r1"))
        for ((index, command) in commands.withIndex()) {
            server.enqueue(response(snapshot()))
            repository.execute("alice", command)
            val body = SupabaseClient.json.parseToJsonElement(request().body.readUtf8()).jsonObject
            assertEquals(listOf("accept", "decline", "cancel", "remove")[index], body["p_action"]?.jsonPrimitive?.content)
            assertEquals("r1", body["p_relation_id"]?.jsonPrimitive?.content)
            assertEquals(JsonNull, body["p_target_uid"])
        }
    }
    @Test fun invalidCodeAndSelfRequestAreRejectedBeforeAnyNetworkCall() = runBlocking {
        for (command in listOf(FriendCommand.SendByCode("invalid"), FriendCommand.SendToPlayer("alice"))) {
            try { repository.execute("alice", command); fail("Invalid invitation accepted") } catch (_: FriendsException) { }
        }
        assertEquals(0, server.requestCount)
    }
    @Test fun networkFailureRetainsTheCachedSnapshot() = runBlocking {
        server.enqueue(response(snapshot())); repository.refresh("alice")
        server.enqueue(response("{}", 503))
        try { repository.refresh("alice"); fail("Expected network failure") } catch (_: FriendsException) { }
        assertEquals("bob", repository.observe("alice").first().relations.single().player.uid)
        assertEquals(1, local.writes)
    }
    @Test fun switchingAccountsWhileTheReplyArrivesCannotWriteIntoEitherAccountsCache() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                accounts.value.value = PlayerAccount("charlie", "Charlie")
                return response(snapshot())
            }
        }
        try { repository.refresh("alice"); fail("Stale account response accepted") } catch (_: CancellationException) { }
        assertEquals(0, local.writes)
        assertTrue(repository.observe("charlie").first().relations.isEmpty())
    }
    @Test fun aReplyForAnotherViewerIsNeverPersisted() = runBlocking {
        server.enqueue(response(snapshot(uid = "charlie")))
        try { repository.refresh("alice"); fail("Wrong viewer accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(0, local.writes)
    }
    @Test fun malformedRelationshipsAreRejectedInsteadOfReassigned() = runBlocking {
        for (body in listOf(snapshot(relationStatus = "unknown"), snapshot().replace("\"uid\":\"bob\"", "\"uid\":\"alice\""))) {
            server.enqueue(response(body))
            try { repository.refresh("alice"); fail("Malformed relation accepted") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, local.writes)
    }
    @Test fun serverBusinessErrorsBecomeSafeUiReasons() = runBlocking {
        server.enqueue(response("""{"message":"friend_limit","code":"22023"}""", 400))
        try { source.execute("alice", FriendCommand.SendToPlayer("bob")); fail("Expected limit") }
        catch (error: FriendsException) { assertEquals(FriendsFailure.LIMIT, error.failure) }
    }
    @Test fun localWriteFailureIsReportedWithoutPretendingTheInvitationIsCached() = runBlocking {
        local.fail = true; server.enqueue(response(snapshot()))
        try { repository.refresh("alice"); fail("Expected local failure") }
        catch (error: FriendsException) { assertEquals(FriendsFailure.LOCAL, error.failure) }
        assertTrue(repository.observe("alice").first().relations.isEmpty())
    }
    @Test fun publicProfileIsCachedPerViewerAndNeverRetainsAnotherPlayersFriendCode() = runBlocking {
        server.enqueue(response("""{"uid":"bob","display_name":"Bob","skin_id":"toad","friend_code":"ABCDEF123456","records":[{"level_id":"prairie-1-v2","time_ms":42000}]}"""))
        repository.refreshProfile("alice", "bob")
        val profile = repository.observeProfile("alice", "bob").first()
        assertNotNull(profile); assertNull(profile?.friendCode)
        assertEquals(42_000L, profile?.records?.single()?.timeMs)
        assertNull(repository.observeProfile("charlie", "bob").first())
    }
    @Test fun friendRequestsRenewTheJwtOnceOn401() = runBlocking {
        val api = SupabaseClient.create(server.url("/").toString(), "sb_publishable_test")
        val store = object : SessionStore {
            var session = AuthSession("old-token", "refresh", 3600, 5000, AuthUser("alice"))
            override suspend fun read() = session
            override suspend fun save(session: AuthSession) { this.session = session }
            override suspend fun clear() = Unit
        }
        val auth = SupabaseAccountRepository(api, store, true, now = { 1000 })
        val renewed = AuthSession("new-token", "refresh-two", 3600, 6000, AuthUser("alice"))
        server.enqueue(response("{}", 401))
        server.enqueue(response(SupabaseClient.json.encodeToString(renewed)))
        server.enqueue(response(snapshot()))
        SupabaseFriendsSource(api, auth).snapshot("alice")
        assertEquals("Bearer old-token", request().getHeader("Authorization"))
        assertEquals("/auth/v1/token?grant_type=refresh_token", request().path)
        assertEquals("Bearer new-token", request().getHeader("Authorization"))
        assertEquals(3, server.requestCount)
    }
}

private class MemoryFriendsStore : FriendsLocalStore {
    val cache = mutableMapOf<String, MutableStateFlow<FriendsCacheWithRelations?>>()
    val profiles = mutableMapOf<Pair<String, String>, MutableStateFlow<List<SocialProfileRow>>>()
    var writes = 0
    var fail = false
    override fun observe(uid: String) = cache.getOrPut(uid) { MutableStateFlow(null) }
    override fun observeProfile(viewerUid: String, playerUid: String) = profiles.getOrPut(viewerUid to playerUid) { MutableStateFlow(emptyList()) }
    override suspend fun replace(cache: FriendsCacheEntity, relations: List<FriendRelationEntity>): Int {
        if (fail) throw java.io.IOException()
        val state = observe(cache.viewerUid)
        val old = state.value?.relations?.filter { it.status == "incoming" }?.map { it.relationId }.orEmpty()
        state.value = FriendsCacheWithRelations(cache, relations); writes++
        return relations.count { it.status == "incoming" && it.relationId !in old }
    }
    override suspend fun saveProfile(profile: SocialProfileEntity, records: List<SocialProfileRecordEntity>) {
        observeProfile(profile.viewerUid, profile.profileUid).value = if (records.isEmpty()) listOf(SocialProfileRow(profile, null, null))
            else records.map { SocialProfileRow(profile, it.levelId, it.timeMs) }
    }
}
