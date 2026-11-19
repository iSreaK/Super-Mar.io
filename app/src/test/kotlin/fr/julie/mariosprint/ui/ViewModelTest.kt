package fr.julie.mariosprint.ui

import androidx.lifecycle.ViewModelStore
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun <T : androidx.lifecycle.ViewModel> keep(vm: T): T = vm.also { store.put("vm", it) }

    @Test fun collectionSwitchesToTheNewAccountsOwnMissionsAndSkin() = runTest {
        val accounts = FakeAccounts().apply { value.value = PlayerAccount("alice", "Alice") }
        val progress = FakeProgress()
        progress.values["alice"] = MutableStateFlow(PlayerProgress(setOf(LevelId.MEADOW), CharacterSkin.TOAD))
        val vm = keep(CollectionViewModel(accounts, progress, FakeSync()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        assertEquals(CharacterSkin.TOAD, vm.state.value.equippedSkin)
        accounts.value.value = PlayerAccount("bob", "Bob")
        assertEquals(setOf(CharacterSkin.LUIGI), vm.state.value.unlockedSkins)
        assertEquals(CharacterSkin.LUIGI, vm.state.value.equippedSkin)
        accounts.value.value = null
        assertFalse(vm.state.value.signedIn)
        assertEquals(0, vm.state.value.missions.first().progress)
    }
    @Test fun guestCanEquipAnEarnedSkinWithoutSendingItToAnAccount() = runTest {
        val progress = FakeProgress()
        progress.values[null] = MutableStateFlow(PlayerProgress(setOf(LevelId.MEADOW)))
        val vm = keep(CollectionViewModel(FakeAccounts(), progress, FakeSync()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(CollectionAction.Equip(CharacterSkin.TOAD))
        assertEquals(CharacterSkin.TOAD, vm.state.value.equippedSkin)
        assertEquals(0, progress.syncs)
    }
    @Test fun collectionRejectsALockedSkinAndKeepsTheCurrentSelection() = runTest {
        val vm = keep(CollectionViewModel(FakeAccounts(), FakeProgress(), FakeSync()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(CollectionAction.Equip(CharacterSkin.PEACH))
        assertEquals(CharacterSkin.LUIGI, vm.state.value.equippedSkin)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }
    @Test fun synchronizationErrorIsNotReportedAsALockedSkin() = runTest {
        val accounts = FakeAccounts().apply { value.value = PlayerAccount("alice", "Alice") }
        val vm = keep(CollectionViewModel(accounts, FakeProgress(), FakeSync(IllegalArgumentException())))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(CollectionAction.Synchronize)
        assertEquals(UiText(fr.julie.mariosprint.R.string.progress_sync_pending), vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test fun selectedLevelUsesItsOwnStatsAndKeepsLegacyBestSeparate() = runTest {
        val records = FakeRecords()
        records.levelStats[LevelId.CANYON.value] = MutableStateFlow(PersonalStats(bestMs = 46000))
        records.levelStats[Level.LEGACY_ID] = MutableStateFlow(PersonalStats(bestMs = 29829))
        val vm = keep(HomeViewModel(records))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.selectLevel(LevelId.CANYON)
        assertEquals(LevelId.CANYON, vm.state.value.selectedLevel)
        assertEquals(46000L, vm.state.value.stats.bestMs); assertEquals(29829L, vm.state.value.legacyBestMs)
        vm.selectLevel(LevelId.NIGHT)
        assertNull(vm.state.value.stats.bestMs); assertEquals(29829L, vm.state.value.legacyBestMs)
    }
    @Test fun selectedGameRecordsAttemptsAndResultsForThatLevel() = runTest {
        val records = FakeRecords()
        val vm = keep(GameViewModel(records, FakeAccounts(), FakeSync(), LevelId.NIGHT))
        assertEquals(LevelId.NIGHT, vm.state.value.level); assertEquals(LevelId.NIGHT.value, records.lastLevel)
        vm.engine.player.x = Level.FLAG_X; vm.engine.advance(.02, GameInput().apply { right = true })
        vm.onAction(GameAction.Frame)
        assertEquals(LevelId.NIGHT.value, records.lastLevel)
    }
    @Test fun pauseInvalidatesRunAndRestartCreatesAFreshSession() = runTest {
        val records = FakeRecords(); val vm = keep(GameViewModel(records, FakeAccounts(), FakeSync()))
        assertEquals(1, records.attempts)
        vm.onAction(GameAction.Pause)
        assertEquals(Phase.PAUSED, vm.state.value.phase); assertFalse(vm.state.value.eligible)
        vm.onAction(GameAction.Resume)
        assertEquals(Phase.RUNNING, vm.state.value.phase)
        vm.onAction(GameAction.Restart)
        assertEquals(2, vm.state.value.sessionId); assertTrue(vm.state.value.eligible)
        assertEquals(Phase.READY, vm.state.value.phase); assertEquals(2, records.attempts)
    }
    @Test fun terminalFrameIsRecordedOnlyOnce() = runTest {
        val records = FakeRecords(); val vm = keep(GameViewModel(records, FakeAccounts(), FakeSync()))
        vm.engine.player.y = 1000f
        vm.engine.advance(.02, GameInput().apply { right = true })
        assertEquals(Phase.DEAD, vm.engine.phase)
        repeat(3) { vm.onAction(GameAction.Frame) }
        assertEquals(1, records.finishes)
    }
    @Test fun unconfiguredGoogleEmitsAnEventAndDoesNotStartAuthentication() = runTest {
        val accounts = FakeAccounts(configured = false)
        val vm = keep(AccountViewModel(accounts, FakeRecords(), FakeSync(), FakeProgress()))
        val events = mutableListOf<AccountEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(events) }
        vm.onAction(AccountAction.SignIn)
        assertTrue(events.single() is AccountEvent.ShowMessage); assertEquals(0, accounts.signIns)
    }
    @Test fun accountEventsAreNotReplayedToANewSubscriber() = runTest {
        val vm = keep(AccountViewModel(FakeAccounts(), FakeRecords(), FakeSync(), FakeProgress()))
        val first = mutableListOf<AccountEvent>()
        val job = backgroundScope.launch(dispatcher) { vm.events.toList(first) }
        vm.onAction(AccountAction.SignIn)
        assertEquals(AccountEvent.RequestGoogleSignIn, first.single())
        job.cancel()
        val second = mutableListOf<AccountEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(second) }
        assertTrue(second.isEmpty())
    }
    @Test fun accountSwitchImmediatelyRemovesPreviousRanking() = runTest {
        val accounts = FakeAccounts()
        accounts.value.value = PlayerAccount("alice", "Alice")
        val ranking = FakeRanking()
        ranking.rows["alice"] = MutableStateFlow(listOf(LeaderboardEntry("alice", "Alice", 42000, 1)))
        ranking.rows["bob"] = MutableStateFlow(listOf(LeaderboardEntry("bob", "Bob", 43000, 1)))
        val vm = keep(LeaderboardViewModel(accounts, ranking, FakeRecords()))
        assertEquals("alice", vm.state.value.entries.single().uid)
        accounts.value.value = PlayerAccount("bob", "Bob")
        assertEquals("bob", vm.state.value.entries.single().uid)
        accounts.value.value = null
        assertTrue(vm.state.value.entries.isEmpty())
    }
    @Test fun networkFailureKeepsTheCachedRankingAndOffersAnError() = runTest {
        val accounts = FakeAccounts(); accounts.value.value = PlayerAccount("alice", "Alice")
        val ranking = FakeRanking().apply { fail = true; rows["alice"] = MutableStateFlow(listOf(LeaderboardEntry("alice", "Alice", 42000, 1))) }
        val vm = keep(LeaderboardViewModel(accounts, ranking, FakeRecords()))
        assertNotNull(vm.state.value.error); assertEquals(1, vm.state.value.entries.size)
        assertFalse(vm.state.value.loading); assertTrue(vm.state.value.cached)
        ranking.fail = false; vm.onAction(LeaderboardAction.Refresh)
        assertNull(vm.state.value.error); assertFalse(vm.state.value.cached)
    }
    @Test fun guestRunCannotBeEnrolledBySigningInDuringTheRun() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts()
        val vm = keep(GameViewModel(records, accounts, FakeSync()))
        accounts.value.value = PlayerAccount("alice", "Alice")
        vm.engine.player.x = Level.FLAG_X
        vm.engine.advance(.02, GameInput().apply { right = true })
        vm.onAction(GameAction.Frame)
        assertEquals(Phase.FINISHED, vm.state.value.phase)
        assertEquals(1, records.finishes); assertNull(records.lastUid)
        assertEquals(RecordStatus.LOCAL, vm.state.value.recordStatus)
        assertFalse(records.awardedMissions)
    }
    @Test fun accountSwitchDuringARunCannotGiveTheTimeToAnotherPlayer() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts()
        accounts.value.value = PlayerAccount("alice", "Alice")
        val vm = keep(GameViewModel(records, accounts, FakeSync()))
        accounts.value.value = PlayerAccount("bob", "Bob")
        vm.engine.player.x = Level.FLAG_X
        vm.engine.advance(.02, GameInput().apply { right = true })
        vm.onAction(GameAction.Frame)
        assertEquals(1, records.finishes); assertNull(records.lastUid)
        assertFalse(records.awardedMissions)
    }
    @Test fun signOutRemainsBusyUntilTheGoogleSelectorIsCleared() = runTest {
        val accounts = FakeAccounts(); accounts.value.value = PlayerAccount("alice", "Alice")
        val vm = keep(AccountViewModel(accounts, FakeRecords(), FakeSync(), FakeProgress()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(AccountAction.SignOut)
        assertNull(accounts.account.value); assertTrue(vm.state.value.busy)
        assertTrue(vm.state.value.googleSessionClearPending)
        vm.onAction(AccountAction.SessionCleared)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.googleSessionClearPending)
    }
    @Test fun signOutCleanupSurvivesAnInactiveUiWithoutAnEventSubscriber() = runTest {
        val signedOut = CompletableDeferred<Unit>()
        val accounts = FakeAccounts().apply {
            value.value = PlayerAccount("alice", "Alice")
            onSignOut = { signedOut.await() }
        }
        val vm = keep(AccountViewModel(accounts, FakeRecords(), FakeSync(), FakeProgress()))
        val foreground = backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(AccountAction.SignOut)
        assertTrue(vm.state.value.busy)
        foreground.cancel()
        advanceTimeBy(5_001)
        runCurrent()

        signedOut.complete(Unit)
        runCurrent()
        assertNull(accounts.account.value)
        val resumed = backgroundScope.launch(dispatcher) { vm.state.collect() }
        assertNull(vm.state.value.player)
        assertTrue(vm.state.value.googleSessionClearPending)
        assertTrue(vm.state.value.busy)
        vm.onAction(AccountAction.SignOut)
        assertEquals(1, accounts.signOuts)

        vm.onAction(AccountAction.SessionCleared)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.googleSessionClearPending)
        resumed.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        assertFalse(vm.state.value.googleSessionClearPending)
        assertFalse(vm.state.value.busy)
    }
    @Test fun failedSignOutKeepsTheAccountAndAllowsAnotherAttempt() = runTest {
        val accounts = FakeAccounts().apply {
            value.value = PlayerAccount("alice", "Alice")
            onSignOut = { throw java.io.IOException() }
        }
        val vm = keep(AccountViewModel(accounts, FakeRecords(), FakeSync(), FakeProgress()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        val events = mutableListOf<AccountEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(events) }
        vm.onAction(AccountAction.SignOut)
        assertEquals("alice", vm.state.value.player?.uid)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.googleSessionClearPending)
        assertEquals(fr.julie.mariosprint.R.string.sign_out_failed, (events.single() as AccountEvent.ShowMessage).text.resource)

        accounts.onSignOut = {}
        vm.onAction(AccountAction.SignOut)
        assertEquals(2, accounts.signOuts)
        assertNull(vm.state.value.player)
        assertTrue(vm.state.value.googleSessionClearPending)
    }
    @Test fun canceledSignOutDoesNotLeaveTheAccountBusyOrRequestGoogleCleanup() = runTest {
        val accounts = FakeAccounts().apply {
            value.value = PlayerAccount("alice", "Alice")
            onSignOut = { throw CancellationException("Canceled sign-out") }
        }
        val vm = keep(AccountViewModel(accounts, FakeRecords(), FakeSync(), FakeProgress()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        val events = mutableListOf<AccountEvent>()
        backgroundScope.launch(dispatcher) { vm.events.toList(events) }
        vm.onAction(AccountAction.SignOut)
        assertEquals("alice", vm.state.value.player?.uid)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.googleSessionClearPending)
        assertTrue(events.isEmpty())
    }
    @Test fun anUnrelatedCleanupAcknowledgementCannotUnlockASignIn() = runTest {
        val vm = keep(AccountViewModel(FakeAccounts(), FakeRecords(), FakeSync(), FakeProgress()))
        backgroundScope.launch(dispatcher) { vm.state.collect() }
        vm.onAction(AccountAction.SignIn)
        assertTrue(vm.state.value.busy)
        vm.onAction(AccountAction.SessionCleared)
        assertTrue(vm.state.value.busy)
        assertFalse(vm.state.value.googleSessionClearPending)
    }

}

internal class FakeRecords : RecordsRepository {
    val stats = MutableStateFlow(PersonalStats())
    val levelStats = mutableMapOf<String, MutableStateFlow<PersonalStats>>()
    var lastLevel: String? = null
    val sound = MutableStateFlow(true)
    val pending = mutableMapOf<String, RunResult>()
    var attempts = 0
    var finishes = 0
    var lastUid: String? = null
    var awardedMissions = false
    override fun observeStats(levelId: String) = levelStats[levelId] ?: stats
    override fun observeSound() = sound
    override suspend fun toggleSound() { sound.value = !sound.value }
    override suspend fun startAttempt(levelId: String) { attempts++; lastLevel = levelId }
    override suspend fun finishAttempt(coins: Int, result: RunResult?, uid: String?, levelId: String, awardMissions: Boolean) {
        finishes++; lastUid = uid; lastLevel = levelId; awardedMissions = awardMissions
    }
    override suspend fun pending(uid: String) = pending[uid]
    override suspend fun acknowledge(uid: String, sent: RunResult) { if (pending[uid] == sent) pending.remove(uid) }
}
internal class FakeAccounts(override val configured: Boolean = true) : AccountRepository {
    val value = MutableStateFlow<PlayerAccount?>(null)
    override val account = value.asStateFlow()
    var signIns = 0
    var signOuts = 0
    var onSignOut: suspend () -> Unit = {}
    override suspend fun signIn(idToken: String, nonce: String) { signIns++ }
    override suspend fun signOut() { signOuts++; onSignOut(); value.value = null }
}
internal class FakeSync(private val failure: Exception? = null) : PlayerSyncRepository {
    override suspend fun synchronize(): Boolean { failure?.let { throw it }; return false }
}
internal class FakeProgress : ProgressRepository {
    val values = mutableMapOf<String?, MutableStateFlow<PlayerProgress>>()
    var syncs = 0
    override fun observe(uid: String?) = values.getOrPut(uid) { MutableStateFlow(PlayerProgress()) }
    override suspend fun equip(uid: String?, skin: CharacterSkin) {
        val value = observe(uid)
        require(skin in value.value.unlockedSkins)
        value.value = value.value.copy(equippedSkin = skin)
    }
    override suspend fun synchronize(account: PlayerAccount) { syncs++ }
}
internal class FakeRanking : RankingRepository {
    val rows = mutableMapOf<String, MutableStateFlow<List<LeaderboardEntry>>>()
    var fail = false
    var onSubmit: suspend (PlayerAccount, RunResult) -> Unit = { _, _ -> }
    override fun observeEntries(uid: String, levelId: String) = rows.getOrPut(uid) { MutableStateFlow(emptyList()) }
    override suspend fun refresh(uid: String, levelId: String) { if (fail) throw java.io.IOException() }
    override suspend fun submit(account: PlayerAccount, result: RunResult) { onSubmit(account, result) }
}
