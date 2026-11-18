package fr.julie.mariosprint.data.remote

import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.PlayerAccount
import retrofit2.HttpException

internal class SupabaseRankingSource(private val api: SupabaseApi, private val tokens: AccessTokenProvider) {
    suspend fun load(levelId: String, uid: String): List<RankingRemote> {
        require(levelId in LevelId.rankedIds)
        return authorized(uid) { api.leaderboard(it, LevelRequest(levelId)) }.take(50).mapNotNull { row ->
            if (row.timeMs !in 10_000..Level.MAX_TIME_MS || row.uid.isBlank()) null
            else row.copy(displayName = row.displayName.trim().take(40), coins = row.coins.coerceIn(0, 100))
        }.sortedWith(compareBy<RankingRemote> { it.timeMs }.thenBy { it.uid })
    }
    suspend fun submit(account: PlayerAccount, result: RunResult) {
        require(result.eligible && result.timeMs in 10_000..Level.MAX_TIME_MS && result.coins in 0..100)
        require(result.levelId in LevelId.rankedIds)
        val level = LevelId.from(result.levelId)
        authorized(account.uid) { api.submit(it, RunRequest(level.value, level.version, result.timeMs, result.coins)) }
    }
    private suspend fun <T> authorized(uid: String, action: suspend (String) -> T): T {
        val token = tokens.tokenFor(uid)
        return try { action("Bearer $token") } catch (error: HttpException) {
            if (error.code() != 401) throw error
            action("Bearer " + tokens.tokenFor(uid, rejectedToken = token))
        }
    }
}
