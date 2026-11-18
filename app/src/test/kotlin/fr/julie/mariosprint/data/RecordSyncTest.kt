package fr.julie.mariosprint.data

import fr.julie.mariosprint.data.repository.DefaultPlayerSyncRepository
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.model.PlayerAccount
import fr.julie.mariosprint.domain.repository.RunRejectedException
import fr.julie.mariosprint.ui.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class RecordSyncTest {
    @Test fun noAccountDoesNotReadAnotherPlayersProgress() = runTest {
        val progress = FakeProgress()
        assertFalse(DefaultPlayerSyncRepository(FakeRecords(), FakeAccounts(), FakeRanking(), progress).synchronize())
        assertEquals(0, progress.syncs)
    }
    @Test fun progressIsRestoredEvenWithoutPendingRecords() = runTest {
        val accounts = FakeAccounts().apply { value.value = PlayerAccount("alice", "Alice") }
        val progress = FakeProgress()
        assertFalse(DefaultPlayerSyncRepository(FakeRecords(), accounts, FakeRanking(), progress).synchronize())
        assertEquals(1, progress.syncs)
    }
    @Test fun switchingAccountsDuringSubmissionDoesNotFetchThePreviousPlayersSkins() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts(); val ranking = FakeRanking(); val progress = FakeProgress()
        accounts.value.value = PlayerAccount("alice", "Alice")
        records.pending["alice"] = RunResult(42000, 1, true)
        ranking.onSubmit = { _, _ -> accounts.value.value = PlayerAccount("bob", "Bob") }
        assertTrue(DefaultPlayerSyncRepository(records, accounts, ranking, progress).synchronize())
        assertEquals(0, progress.syncs)
    }
    @Test fun fasterRunArrivingDuringSubmissionIsAlsoSent() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts(); val ranking = FakeRanking()
        accounts.value.value = PlayerAccount("alice", "Alice")
        records.pending["alice"] = RunResult(42000, 1, true)
        val sent = mutableListOf<Long>()
        ranking.onSubmit = { _, result ->
            sent.add(result.timeMs)
            if (sent.size == 1) records.pending["alice"] = RunResult(40000, 2, true)
        }
        assertTrue(DefaultPlayerSyncRepository(records, accounts, ranking, FakeProgress()).synchronize())
        assertEquals(listOf(42000L, 40000L), sent); assertTrue(records.pending.isEmpty())
    }
    @Test fun failedSubmissionRetainsPendingRecordWithoutRetryLoop() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts(); val ranking = FakeRanking()
        accounts.value.value = PlayerAccount("alice", "Alice")
        records.pending["alice"] = RunResult(42000, 1, true)
        var calls = 0
        ranking.onSubmit = { _, _ -> calls++; throw java.io.IOException() }
        try { DefaultPlayerSyncRepository(records, accounts, ranking, FakeProgress()).synchronize(); fail("Expected failure") }
        catch (_: java.io.IOException) { }
        assertEquals(1, calls); assertNotNull(records.pending["alice"])
    }
    @Test fun runRefusedByTheServerDoesNotBlockOtherLevelsOrMissions() = runTest {
        val records = FakeRecords(); val accounts = FakeAccounts(); val ranking = FakeRanking(); val progress = FakeProgress()
        accounts.value.value = PlayerAccount("alice", "Alice")
        val refused = RunResult(42000, 1, true, LevelId.CANYON.value)
        val accepted = RunResult(43000, 2, true, LevelId.NIGHT.value)
        records.queued += "alice" to refused; records.queued += "alice" to accepted
        val attempts = mutableListOf<String>()
        ranking.onSubmit = { _, result ->
            attempts += result.levelId
            if (result.levelId == refused.levelId) throw RunRejectedException(result.levelId)
        }
        try { DefaultPlayerSyncRepository(records, accounts, ranking, progress).synchronize(); fail("Expected rejection") }
        catch (error: RunRejectedException) { assertEquals(refused.levelId, error.levelId) }
        // Un seul essai par parcours ; le temps refusé est conservé, le suivant est envoyé, les missions sont lues.
        assertEquals(listOf(refused.levelId, accepted.levelId), attempts)
        assertEquals(listOf("alice" to refused), records.queued)
        assertEquals(1, progress.syncs)
    }
}
