package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.model.LeaderboardEntry
import fr.julie.mariosprint.domain.model.PlayerAccount
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    fun observeEntries(uid: String, levelId: String = Level.ID): Flow<List<LeaderboardEntry>>
    suspend fun refresh(uid: String, levelId: String = Level.ID)
    suspend fun submit(account: PlayerAccount, result: RunResult)
}
