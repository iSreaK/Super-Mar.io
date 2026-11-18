package fr.julie.mariosprint.data.repository

import androidx.room.withTransaction
import fr.julie.mariosprint.data.local.SprintDatabase
import fr.julie.mariosprint.data.mapping.*
import fr.julie.mariosprint.data.remote.SupabaseRankingSource
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

internal class CachedRankingRepository(private val database: SprintDatabase, private val remote: SupabaseRankingSource,
    private val accounts: AccountRepository) : RankingRepository {
    override fun observeEntries(uid: String, levelId: String) = database.recordsDao().observeRanking(levelId, uid)
        .map { rows -> rows.map { it.toDomain() } }
    override suspend fun refresh(uid: String, levelId: String) = withTimeout(12_000) {
        check(accounts.account.value?.uid == uid)
        val downloaded = remote.load(levelId, uid)
        check(accounts.account.value?.uid == uid)
        database.withTransaction {
            database.recordsDao().clearRanking(levelId, uid)
            database.recordsDao().insertRanking(downloaded.map {
                LeaderboardEntry(it.uid, it.displayName, it.timeMs, it.coins).toEntity(levelId, uid)
            })
        }
    }
    override suspend fun submit(account: PlayerAccount, result: RunResult) = withTimeout(12_000) {
        check(accounts.account.value?.uid == account.uid)
        remote.submit(account, result)
    }
}
