package fr.julie.mariosprint.data

import fr.julie.mariosprint.data.local.SessionStore
import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.data.repository.SupabaseAccountRepository
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.PlayerAccount
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class SupabaseTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemorySessionStore
    private lateinit var accounts: SupabaseAccountRepository
    private lateinit var api: SupabaseApi
    @Before fun prepare() {
        server = MockWebServer().apply { start() }
        store = MemorySessionStore()
        api = SupabaseClient.create(server.url("/").toString(), "sb_publishable_test")
        accounts = SupabaseAccountRepository(api, store, true, now = { 1000L })
    }
    @After fun cleanup() { server.shutdown() }
    private fun response(json: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(json)
    private fun session(token: String = "access-one", expiresAt: Long = 5000) = AuthSession(token, "refresh-one", 3600, expiresAt,
        AuthUser("alice", buildJsonObject { put("full_name", "Alice") }))
    private fun enqueueSession(token: String = "access-one") { server.enqueue(response(SupabaseClient.json.encodeToString(session(token)))) }
    private fun request() = checkNotNull(server.takeRequest(2, TimeUnit.SECONDS))
    @Test fun googleIdTokenExchangeUsesRawNonceAndPublishableKey() = runBlocking {
        enqueueSession(); accounts.signIn("google-jwt", "raw-nonce")
        val request = request(); val json = SupabaseClient.json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("/auth/v1/token?grant_type=id_token", request.path)
        assertEquals("sb_publishable_test", request.getHeader("apikey")); assertNull(request.getHeader("Authorization"))
        assertEquals("raw-nonce", json["nonce"]?.jsonPrimitive?.content)
        assertEquals("google", json["provider"]?.jsonPrimitive?.content)
        assertEquals("google-jwt", json["id_token"]?.jsonPrimitive?.content)
        assertEquals("alice", accounts.account.value?.uid); assertNotNull(store.value)
    }
    @Test fun sessionIsRestoredWithoutANetworkRequest() = runBlocking {
        store.value = session(); accounts.initialize()
        assertEquals("Alice", accounts.account.value?.name); assertEquals(0, server.requestCount)
        assertEquals("access-one", accounts.tokenFor("alice"))
    }
    @Test fun expiredSessionRefreshesAndPersistsRotatedTokens() = runBlocking {
        store.value = session(expiresAt = 900); enqueueSession("access-two")
        assertEquals("access-two", accounts.tokenFor("alice"))
        val request = request()
        assertEquals("/auth/v1/token?grant_type=refresh_token", request.path)
        assertEquals("refresh-one", SupabaseClient.json.parseToJsonElement(request.body.readUtf8()).jsonObject["refresh_token"]?.jsonPrimitive?.content)
        assertEquals("access-two", store.value?.accessToken)
        assertEquals("access-two", accounts.tokenFor("alice")); assertEquals(1, server.requestCount)
    }
    @Test fun invalidRefreshClearsTheLocalSession() = runBlocking {
        store.value = session(expiresAt = 900); server.enqueue(response("{}", 400))
        try { accounts.tokenFor("alice"); fail("Invalid token accepted") } catch (_: HttpException) { }
        assertNull(accounts.account.value); assertNull(store.value)
    }
    @Test fun invalidRefreshErasesStoredTokenBeforeNotifyingObservers() = runBlocking {
        store.value = session(expiresAt = 900); store.yieldBeforeClear = true; accounts.initialize()
        var erasedBeforeNotification: Boolean? = null
        val observer = launch(Dispatchers.Unconfined) {
            accounts.account.collect { if (it == null) erasedBeforeNotification = store.value == null }
        }
        server.enqueue(response("{}", 400))
        try { accounts.tokenFor("alice"); fail("Invalid token accepted") } catch (_: HttpException) { }
        assertEquals(true, erasedBeforeNotification)
        observer.cancelAndJoin()
    }
    @Test fun temporaryServerFailureKeepsSessionForRetry() = runBlocking {
        store.value = session(expiresAt = 900); server.enqueue(response("{}", 503))
        try { accounts.tokenFor("alice"); fail("Expected error") } catch (_: HttpException) { }
        assertEquals("alice", accounts.account.value?.uid); assertNotNull(store.value)
    }
    @Test fun anotherAccountCannotUseTheCurrentSession() = runBlocking {
        store.value = session()
        try { accounts.tokenFor("bob"); fail("Wrong UID accepted") } catch (_: IllegalStateException) { }
        assertEquals(0, server.requestCount)
    }
    @Test fun signingOutUsesLocalScopeAndRemovesPersistedSession() = runBlocking {
        store.value = session(); server.enqueue(MockResponse().setResponseCode(204))
        accounts.signOut()
        val request = request()
        assertEquals("/auth/v1/logout?scope=local", request.path)
        assertEquals("Bearer access-one", request.getHeader("Authorization"))
        assertNull(store.value); assertNull(accounts.account.value)
    }
    @Test fun submitContainsTheSelectedLevelVersionAndNeverAClientUid() = runBlocking {
        store.value = session(); server.enqueue(MockResponse().setResponseCode(204))
        val result = RunResult(42000, 7, true, LevelId.NIGHT.value)
        SupabaseRankingSource(api, accounts).submit(PlayerAccount("alice", "Untrusted name"), result)
        val request = request(); val body = SupabaseClient.json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("/rest/v1/rpc/submit_run", request.path)
        assertEquals("Bearer access-one", request.getHeader("Authorization"))
        assertEquals(LevelId.NIGHT.value, body["p_level_id"]?.jsonPrimitive?.content)
        assertEquals(1, body["p_level_version"]?.jsonPrimitive?.int)
        assertFalse(body.containsKey("uid")); assertFalse(body.containsKey("display_name"))
    }
    @Test fun practiceAndLegacyRunsNeverReachTheServer() = runBlocking {
        val source = SupabaseRankingSource(api, accounts)
        listOf(RunResult(42000, 1, false), RunResult(42000, 1, true, Level.LEGACY_ID)).forEach { result ->
            try { source.submit(PlayerAccount("alice", "Alice"), result); fail("Invalid run sent") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, server.requestCount)
    }
    @Test fun unauthorizedResponseRefreshesOnceThenRetries() = runBlocking {
        store.value = session(); server.enqueue(response("{}", 401)); enqueueSession("access-two"); server.enqueue(response("[]"))
        val rows = SupabaseRankingSource(api, accounts).load(Level.ID, "alice")
        assertTrue(rows.isEmpty()); assertEquals("Bearer access-one", request().getHeader("Authorization"))
        assertEquals("/auth/v1/token?grant_type=refresh_token", request().path)
        assertEquals("Bearer access-two", request().getHeader("Authorization")); assertEquals(3, server.requestCount)
    }
    @Test fun staleUnauthorizedResponseDoesNotRotateAnAlreadyRefreshedTokenAgain() = runBlocking {
        store.value = session(); accounts.initialize(); enqueueSession("access-two")
        assertEquals("access-two", accounts.tokenFor("alice", "access-one"))
        assertEquals("access-two", accounts.tokenFor("alice", "access-one")); assertEquals(1, server.requestCount)
    }
    @Test fun leaderboardIsValidatedAndSortedWithDeterministicTies() = runBlocking {
        store.value = session()
        server.enqueue(response("""[{"uid":"b","display_name":" Bob ","time_ms":42000,"coins":2},
            {"uid":"bad","display_name":"Bad","time_ms":1,"coins":2},
            {"uid":"a","display_name":"Alice","time_ms":42000,"coins":3}]"""))
        val rows = SupabaseRankingSource(api, accounts).load(LevelId.CANYON.value, "alice")
        assertEquals(listOf("a", "b"), rows.map { it.uid }); assertEquals("Bob", rows[1].displayName)
        assertTrue(request().body.readUtf8().contains(LevelId.CANYON.value))
    }
}
private class MemorySessionStore : SessionStore {
    var value: AuthSession? = null
    var yieldBeforeClear = false
    override suspend fun read() = value
    override suspend fun save(session: AuthSession) { value = session }
    override suspend fun clear() { if (yieldBeforeClear) yield(); value = null }
}
