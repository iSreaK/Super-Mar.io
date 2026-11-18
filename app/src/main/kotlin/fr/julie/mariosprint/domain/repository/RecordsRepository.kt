package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.model.PersonalStats
import kotlinx.coroutines.flow.Flow

interface RecordsRepository {
    fun observeStats(levelId: String = Level.ID): Flow<PersonalStats>
    fun observeSound(): Flow<Boolean>
    suspend fun toggleSound()
    suspend fun startAttempt(levelId: String = Level.ID)
    suspend fun finishAttempt(coins: Int, result: RunResult?, uid: String?, levelId: String = result?.levelId ?: Level.ID,
        awardMissions: Boolean = true)
    suspend fun pending(uid: String): RunResult?
    suspend fun acknowledge(uid: String, sent: RunResult)
}
