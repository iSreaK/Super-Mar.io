package fr.julie.mariosprint

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import fr.julie.mariosprint.data.local.*
import fr.julie.mariosprint.data.local.model.RankingEntity
import fr.julie.mariosprint.data.repository.RoomRecordsRepository
import fr.julie.mariosprint.data.mapping.progressOwner
import fr.julie.mariosprint.domain.game.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class RoomRecordsTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val context = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("room-test-$name", mode)
    }
    private lateinit var database: SprintDatabase
    private lateinit var local: RoomRecordsRepository
    @Before fun prepare() {
        base.deleteSharedPreferences("room-test-prairie-v1")
        database = Room.inMemoryDatabaseBuilder(base, SprintDatabase::class.java).build()
        local = RoomRecordsRepository(database, LegacyRecordsMigration(context, database))
    }
    @After fun cleanup() { database.close(); base.deleteSharedPreferences("room-test-prairie-v1") }
    @Test fun missionsAreAtomicWithValidFinishesAndIsolatedByOwner() = runBlocking {
        local.finishAttempt(2, RunResult(42000, 2, true), "alice")
        local.finishAttempt(3, RunResult(43000, 3, true, LevelId.CANYON.value), null)
        local.finishAttempt(2, RunResult(44000, 2, false, LevelId.NIGHT.value), "alice")
        local.finishAttempt(1, RunResult(45000, 1, true, LevelId.NIGHT.value), "bob", awardMissions = false)
        assertEquals(listOf(Level.ID), database.progressDao().completed(progressOwner("alice")).map { it.levelId })
        assertEquals(listOf(LevelId.CANYON.value), database.progressDao().completed(progressOwner(null)).map { it.levelId })
        assertTrue(database.progressDao().completed(progressOwner("bob")).isEmpty())
    }
    @Test fun legacyImportPreservesAllDataAndRunsOnlyOnce() = runBlocking {
        context.getSharedPreferences("prairie-v1", 0).edit().putLong("bestMs", 29829).putInt("attempts", 7)
            .putInt("finishes", 1).putInt("coins", 38).putBoolean("sound", false)
            .putLong("pending:alice:time", 42000).putInt("pending:alice:coins", 12)
            .putLong("pending:bob:time", 43000).putInt("pending:bob:coins", 9).commit()
        val stats = local.observeStats(Level.LEGACY_ID).first()
        assertEquals(29829L, stats.bestMs); assertEquals(7, stats.attempts)
        assertEquals(1, stats.finishes); assertEquals(38, stats.coins)
        assertFalse(local.observeSound().first())
        assertNull(local.pending("alice")); assertNull(local.pending("bob"))
        assertEquals(42000L, database.recordsDao().pending(Level.LEGACY_ID, "alice")?.timeMs)
        assertEquals(43000L, database.recordsDao().pending(Level.LEGACY_ID, "bob")?.timeMs)
        assertNull(local.observeStats().first().bestMs)
        local.startAttempt(Level.LEGACY_ID)
        val recreated = RoomRecordsRepository(database, LegacyRecordsMigration(context, database))
        assertEquals(8, recreated.observeStats(Level.LEGACY_ID).first().attempts)
        assertEquals(7, context.getSharedPreferences("prairie-v1", 0).getInt("attempts", 0))
    }
    @Test fun levelsHaveSeparateStatsAndAcknowledgements() = runBlocking {
        val meadow = RunResult(42000, 2, true)
        val night = RunResult(46000, 4, true, LevelId.NIGHT.value)
        local.startAttempt(); local.startAttempt(LevelId.NIGHT.value)
        local.finishAttempt(2, meadow, "alice"); local.finishAttempt(4, night, "alice")
        assertEquals(42000L, local.observeStats().first().bestMs)
        assertEquals(46000L, local.observeStats(LevelId.NIGHT.value).first().bestMs)
        local.acknowledge("alice", meadow)
        assertEquals(night, local.pending("alice"))
        local.acknowledge("alice", night); assertNull(local.pending("alice"))
    }
    @Test fun pendingRecordsAreSeparatedByAccount() = runBlocking {
        val alice = RunResult(42000, 12, true); val bob = RunResult(43000, 10, true)
        local.finishAttempt(12, alice, "alice"); local.finishAttempt(10, bob, "bob")
        local.acknowledge("alice", alice)
        assertNull(local.pending("alice")); assertEquals(bob, local.pending("bob"))
    }
    @Test fun slowerPendingRunCannotReplaceAFasterRun() = runBlocking {
        local.finishAttempt(1, RunResult(42000, 1, true), "alice")
        local.finishAttempt(2, RunResult(45000, 2, true), "alice")
        assertEquals(42000L, local.pending("alice")?.timeMs)
    }
    @Test fun acknowledgementCannotDeleteANewerFasterRun() = runBlocking {
        val old = RunResult(42000, 1, true); val newer = RunResult(40000, 2, true)
        local.finishAttempt(1, old, "alice"); local.finishAttempt(2, newer, "alice"); local.acknowledge("alice", old)
        assertEquals(newer, local.pending("alice"))
    }
    @Test fun practiceAndGuestRunsAreNotQueued() = runBlocking {
        local.startAttempt(); local.finishAttempt(12, RunResult(42000, 12, true), null)
        local.startAttempt(); local.finishAttempt(10, RunResult(20000, 10, false), "alice")
        val stats = local.observeStats().first()
        assertEquals(42000L, stats.bestMs); assertEquals(2, stats.attempts)
        assertEquals(2, stats.finishes); assertEquals(22, stats.coins); assertNull(local.pending("alice"))
    }
    @Test fun concurrentAttemptsAreNotLostAndSoundPersists() = runBlocking {
        coroutineScope { repeat(12) { launch(Dispatchers.IO) { local.startAttempt() } } }
        assertEquals(12, local.observeStats().first().attempts)
        local.toggleSound()
        assertFalse(RoomRecordsRepository(database, LegacyRecordsMigration(context, database)).observeSound().first())
    }
    @Test fun rankingCacheIsBoundedSortedAndSeparatedByAccount() = runBlocking {
        database.recordsDao().insertRanking((1..60).map { RankingEntity(Level.ID, "alice", "$it", "Player $it", (100000 - it).toLong(), 0) } +
            RankingEntity(Level.ID, "bob", "private", "Bob", 20000, 0))
        val alice = database.recordsDao().observeRanking(Level.ID, "alice").first()
        assertEquals(50, alice.size); assertEquals("60", alice.first().uid)
        assertEquals("private", database.recordsDao().observeRanking(Level.ID, "bob").first().single().uid)
        database.recordsDao().clearRanking(Level.ID, "alice")
        assertTrue(database.recordsDao().observeRanking(Level.ID, "alice").first().isEmpty())
        assertEquals(1, database.recordsDao().observeRanking(Level.ID, "bob").first().size)
    }
}
