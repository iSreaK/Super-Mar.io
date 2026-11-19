package fr.julie.mariosprint.ui

import androidx.lifecycle.ViewModelStore
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.FriendsRepository
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun accounts() = FakeAccounts().apply { value.value = PlayerAccount("alice", "Alice") }
    private fun keep(vm: FriendsViewModel) = vm.also { store.put("friends", it) }
    private fun incoming(id: String = "request", peer: String = "bob") = Friendship(id, SocialPlayer(peer, peer), FriendshipStatus.INCOMING)

    @Test fun realtimeExistsOnlyWhileTheAppIsVisibleAndAuthenticated() = runTest {
        val accounts = accounts(); val data = FakeFriendsRepository()
        val vm = keep(FriendsViewModel(accounts, data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        assertTrue(data.opened.isEmpty()); assertTrue(data.refreshes.isEmpty())
        vm.onAction(FriendsAction.VisibilityChanged(true))
        assertEquals(listOf("alice"), data.opened); assertEquals(listOf("alice"), data.refreshes)
        vm.onAction(FriendsAction.VisibilityChanged(false))
        assertEquals(listOf("alice"), data.closed)
        advanceTimeBy(90_000); runCurrent()
        assertEquals(1, data.refreshes.size)
        accounts.value.value = null
        vm.onAction(FriendsAction.VisibilityChanged(true))
        assertEquals(1, data.opened.size)
        assertNull(vm.state.value.viewerUid)
    }
    @Test fun reopeningRecoversMissedRequestsEvenWithoutAnAlertSubscriber() = runTest {
        val data = FakeFriendsRepository()
        val vm = keep(FriendsViewModel(accounts(), data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        vm.onAction(FriendsAction.VisibilityChanged(false))
        data.onRefresh = { uid -> data.values(uid).value = FriendsSnapshot(uid, "ABCDEF123456", listOf(incoming()), 100); 1 }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        assertEquals(1, vm.state.value.incoming.size)
        assertEquals("ABCDEF123456", vm.state.value.friendCode)
        assertEquals(2, data.refreshes.size)
    }
    @Test fun joinAndReconnectFetchAFullSnapshotAndEmitADiscreetAlert() = runTest {
        val data = FakeFriendsRepository()
        val vm = keep(FriendsViewModel(accounts(), data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        val alerts = mutableListOf<FriendsEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(alerts) }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        data.onRefresh = { uid -> data.values(uid).value = FriendsSnapshot(uid, "ABCDEF123456", listOf(incoming()), 100); 1 }
        data.events("alice").emit(FriendsLiveEvent.CONNECTED)
        assertTrue(vm.state.value.realtimeConnected)
        assertEquals(FriendsEvent.IncomingRequests("alice", 1), alerts.single())
        data.events("alice").emit(FriendsLiveEvent.DISCONNECTED)
        assertTrue(vm.state.value.cached)
        data.onRefresh = { 0 }
        data.events("alice").emit(FriendsLiveEvent.CONNECTED)
        assertEquals(3, data.refreshes.size)
        assertEquals(1, alerts.size)
    }
    @Test fun anInvalidationBurstIsCoalescedButStillRefreshesAfterTheInflightReply() = runTest {
        val data = FakeFriendsRepository()
        val vm = keep(FriendsViewModel(accounts(), data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        val release = CompletableDeferred<Unit>()
        data.onRefresh = { release.await(); 0 }
        data.events("alice").emit(FriendsLiveEvent.CHANGED)
        repeat(100) { data.events("alice").emit(FriendsLiveEvent.CHANGED) }
        assertEquals(2, data.refreshes.size)
        release.complete(Unit)
        runCurrent()
        assertEquals(3, data.refreshes.size)
    }
    @Test fun updatesFromAnotherScreenAlertOnlyWhileVisibleAndForTheCurrentAccount() = runTest {
        val data = FakeFriendsRepository()
        val vm = keep(FriendsViewModel(accounts(), data))
        val alerts = mutableListOf<FriendsEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(alerts) }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        data.notices.emit(IncomingFriendRequests("alice", 2))
        data.notices.emit(IncomingFriendRequests("charlie", 1))
        vm.onAction(FriendsAction.VisibilityChanged(false))
        data.notices.emit(IncomingFriendRequests("alice", 3))
        assertEquals(listOf(FriendsEvent.IncomingRequests("alice", 2)), alerts)
    }
    @Test fun switchingAccountsClosesTheOldChannelAndRemovesItsCachedRequests() = runTest {
        val accounts = accounts(); val data = FakeFriendsRepository()
        data.values("alice").value = FriendsSnapshot("alice", "ABCDEF123456", listOf(incoming()), 100)
        val vm = keep(FriendsViewModel(accounts, data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        assertEquals(1, vm.state.value.incoming.size)
        accounts.value.value = PlayerAccount("charlie", "Charlie")
        assertEquals("charlie", vm.state.value.viewerUid)
        assertTrue(vm.state.value.relations.isEmpty()); assertEquals("", vm.state.value.friendCode)
        assertEquals(listOf("alice"), data.closed)
        assertEquals(listOf("alice", "charlie"), data.opened)
        accounts.value.value = null
        assertEquals(listOf("alice", "charlie"), data.closed)
    }
    @Test fun aCanceledReplyCannotAlertAfterSigningOutAndBackIntoTheSameUid() = runTest {
        val accounts = accounts(); val data = FakeFriendsRepository()
        val release = CompletableDeferred<Unit>()
        data.onRefresh = { withContext(NonCancellable) { release.await() }; 1 }
        val vm = keep(FriendsViewModel(accounts, data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        val alerts = mutableListOf<FriendsEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(alerts) }
        vm.onAction(FriendsAction.VisibilityChanged(true))
        accounts.value.value = null
        accounts.value.value = PlayerAccount("alice", "Alice")
        data.onRefresh = { 0 }
        release.complete(Unit); runCurrent()
        assertTrue(alerts.isEmpty())
    }
    @Test fun failedMutationKeepsTheCachedFriendAndEnablesRetry() = runTest {
        val data = FakeFriendsRepository()
        data.values("alice").value = FriendsSnapshot("alice", "ABCDEF123456", listOf(
            incoming().copy(status = FriendshipStatus.FRIEND)), 100)
        data.onExecute = { _, _ -> throw FriendsException(FriendsFailure.NETWORK) }
        val vm = keep(FriendsViewModel(accounts(), data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(FriendsAction.Execute(FriendCommand.Remove("request")))
        assertEquals(1, vm.state.value.friends.size)
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.busy)
        data.onExecute = { _, _ -> }
        vm.onAction(FriendsAction.Execute(FriendCommand.Remove("request")))
        assertNull(vm.state.value.error); assertEquals(2, data.commands.size)
    }
    @Test fun aSecondMutationIsIgnoredWhileTheFirstOneIsPending() = runTest {
        val data = FakeFriendsRepository(); val release = CompletableDeferred<Unit>()
        data.onExecute = { _, _ -> release.await() }
        val vm = keep(FriendsViewModel(accounts(), data))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(FriendsAction.Execute(FriendCommand.SendByCode("ABCDEF123456")))
        vm.onAction(FriendsAction.Execute(FriendCommand.SendByCode("ABCDEF123456")))
        assertTrue(vm.state.value.busy); assertEquals(1, data.commands.size)
        release.complete(Unit); assertFalse(vm.state.value.busy)
    }
    @Test fun profilesUseTheCurrentViewersCacheAndIgnoreUnknownCourses() = runTest {
        val accounts = accounts(); val data = FakeFriendsRepository()
        data.profileValues("alice", "bob").value = UserProfile(SocialPlayer("bob", "Bob"), null,
            listOf(ProfileRecord("prairie-1-v2", 42_000), ProfileRecord("future-level", 50_000)))
        val vm = UserProfileViewModel(accounts, data, "bob").also { store.put("profile", it) }
        assertEquals("Bob", vm.state.value.player?.name)
        assertEquals(1, vm.state.value.records.size)
        accounts.value.value = PlayerAccount("charlie", "Charlie")
        assertNull(vm.state.value.player)
        assertTrue(vm.state.value.records.isEmpty())
        assertEquals("charlie", vm.state.value.viewerUid)
    }
    @Test fun aFailedProfileRefreshKeepsItsOfflineData() = runTest {
        val data = FakeFriendsRepository()
        data.profileValues("alice", "bob").value = UserProfile(SocialPlayer("bob", "Bob"), null, emptyList())
        data.onProfile = { _, _ -> throw FriendsException(FriendsFailure.NETWORK) }
        val vm = UserProfileViewModel(accounts(), data, "bob").also { store.put("profile", it) }
        assertEquals("Bob", vm.state.value.player?.name)
        assertNotNull(vm.state.value.error); assertTrue(vm.state.value.cached); assertFalse(vm.state.value.loading)
    }
}

internal class FakeFriendsRepository : FriendsRepository {
    val notices = MutableSharedFlow<IncomingFriendRequests>(extraBufferCapacity = 4)
    override val incomingRequests = notices.asSharedFlow()
    private val snapshots = mutableMapOf<String, MutableStateFlow<FriendsSnapshot>>()
    private val profiles = mutableMapOf<Pair<String, String>, MutableStateFlow<UserProfile?>>()
    private val channels = mutableMapOf<String, MutableSharedFlow<FriendsLiveEvent>>()
    val opened = mutableListOf<String>()
    val closed = mutableListOf<String>()
    val refreshes = mutableListOf<String>()
    val commands = mutableListOf<Pair<String, FriendCommand>>()
    var onRefresh: suspend (String) -> Int = { 0 }
    var onExecute: suspend (String, FriendCommand) -> Unit = { _, _ -> }
    var onProfile: suspend (String, String) -> Unit = { _, _ -> }
    fun values(uid: String) = snapshots.getOrPut(uid) { MutableStateFlow(FriendsSnapshot(uid)) }
    fun profileValues(viewer: String, target: String) = profiles.getOrPut(viewer to target) { MutableStateFlow(null) }
    fun events(uid: String) = channels.getOrPut(uid) { MutableSharedFlow(extraBufferCapacity = 4) }
    override fun observe(uid: String) = values(uid)
    override fun observeProfile(viewerUid: String, playerUid: String) = profileValues(viewerUid, playerUid)
    override suspend fun refresh(uid: String): Int {
        refreshes += uid
        val added = onRefresh(uid)
        currentCoroutineContext().ensureActive()
        if (added > 0) notices.emit(IncomingFriendRequests(uid, added))
        return added
    }
    override suspend fun execute(uid: String, command: FriendCommand) { commands += uid to command; onExecute(uid, command) }
    override suspend fun refreshProfile(viewerUid: String, playerUid: String) { onProfile(viewerUid, playerUid) }
    override fun liveEvents(uid: String): Flow<FriendsLiveEvent> = flow {
        opened += uid
        try { emitAll(events(uid)) } finally { closed += uid }
    }
}
