package fr.julie.mariosprint.domain.repository

import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.model.LeaderboardEntry
import fr.julie.mariosprint.domain.model.PlayerAccount
import kotlinx.coroutines.flow.Flow

/** Refus définitif du serveur pour ce parcours : renvoyer le même temps ne peut pas réussir. */
class RunRejectedException(val levelId: String, cause: Throwable? = null) : Exception("Run rejected for $levelId", cause)

interface RankingRepository {
    fun observeEntries(uid: String, levelId: String = Level.ID): Flow<List<LeaderboardEntry>>
    suspend fun refresh(uid: String, levelId: String = Level.ID)
    suspend fun submit(account: PlayerAccount, result: RunResult)
}
