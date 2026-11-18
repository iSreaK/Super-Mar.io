package fr.julie.mariosprint.data.repository

import androidx.room.withTransaction
import fr.julie.mariosprint.data.local.LegacyRecordsMigration
import fr.julie.mariosprint.data.local.SprintDatabase
import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.data.mapping.toDomain
import fr.julie.mariosprint.data.mapping.progressOwner
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.model.PersonalStats
import fr.julie.mariosprint.domain.repository.RecordsRepository
import kotlinx.coroutines.flow.*

internal class RoomRecordsRepository(private val database: SprintDatabase,
    private val migration: LegacyRecordsMigration) : RecordsRepository {
    private val dao = database.recordsDao()
    override fun observeStats(levelId: String): Flow<PersonalStats> = flow {
        migration.initialize()
        emitAll(dao.observeStats(levelId).map { it?.toDomain() ?: PersonalStats() })
    }
    override fun observeSound(): Flow<Boolean> = flow {
        migration.initialize()
        emitAll(dao.observeSettings().map { it?.sound ?: true })
    }
    override suspend fun toggleSound() {
        migration.initialize()
        database.withTransaction { val old = dao.settings() ?: SettingsEntity(); dao.saveSettings(old.copy(sound = !old.sound)) }
    }
    override suspend fun startAttempt(levelId: String) {
        migration.initialize()
        database.withTransaction { val old = dao.stats(levelId) ?: StatsEntity(levelId); dao.saveStats(old.copy(attempts = old.attempts + 1)) }
    }
    override suspend fun finishAttempt(coins: Int, result: RunResult?, uid: String?, levelId: String, awardMissions: Boolean) {
        require(result == null || result.levelId == levelId)
        require(coins >= 0)
        migration.initialize()
        database.withTransaction {
            val old = dao.stats(levelId) ?: StatsEntity(levelId)
            val best = if (result?.eligible == true) minOf(old.bestMs ?: Long.MAX_VALUE, result.timeMs) else old.bestMs
            dao.saveStats(old.copy(coins = old.coins + coins, finishes = old.finishes + if (result != null) 1 else 0, bestMs = best))
            if (result?.eligible == true && uid != null) {
                val previous = dao.pending(levelId, uid)
                if (previous == null || result.timeMs < previous.timeMs) dao.savePending(PendingRecordEntity(levelId, uid, result.timeMs, result.coins))
            }
            if (awardMissions && result?.eligible == true && result.levelId in LevelId.rankedIds && result.timeMs in 10_000..Level.MAX_TIME_MS) {
                database.progressDao().insertCompleted(listOf(CompletedLevelEntity(progressOwner(uid), result.levelId)))
            }
        }
    }
    override suspend fun pending(uid: String, excludedLevelIds: Set<String>): RunResult? {
        migration.initialize()
        return dao.nextPending(uid, LevelId.rankedIds - excludedLevelIds)?.toDomain()
    }
    override suspend fun acknowledge(uid: String, sent: RunResult) { migration.initialize(); dao.acknowledge(sent.levelId, uid, sent.timeMs) }
}
