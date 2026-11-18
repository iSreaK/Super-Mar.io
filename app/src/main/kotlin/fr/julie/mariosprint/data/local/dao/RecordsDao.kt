package fr.julie.mariosprint.data.local.dao

import androidx.room.*
import fr.julie.mariosprint.data.local.model.*
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RecordsDao {
    @Query("SELECT * FROM personal_stats WHERE levelId = :levelId")
    fun observeStats(levelId: String): Flow<StatsEntity?>
    @Query("SELECT * FROM personal_stats WHERE levelId = :levelId")
    suspend fun stats(levelId: String): StatsEntity?
    @Upsert suspend fun saveStats(value: StatsEntity)
    @Query("SELECT * FROM settings WHERE id = 1") fun observeSettings(): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id = 1") suspend fun settings(): SettingsEntity?
    @Upsert suspend fun saveSettings(value: SettingsEntity)
    @Query("SELECT * FROM pending_records WHERE levelId = :levelId AND uid = :uid")
    suspend fun pending(levelId: String, uid: String): PendingRecordEntity?
    @Query("SELECT * FROM pending_records WHERE uid = :uid AND levelId IN (:levelIds) ORDER BY levelId LIMIT 1")
    suspend fun nextPending(uid: String, levelIds: List<String>): PendingRecordEntity?
    @Upsert suspend fun savePending(value: PendingRecordEntity)
    @Query("DELETE FROM pending_records WHERE levelId = :levelId AND uid = :uid AND timeMs = :timeMs")
    suspend fun acknowledge(levelId: String, uid: String, timeMs: Long)
    @Query("SELECT * FROM leaderboard_cache WHERE levelId = :levelId AND viewerUid = :viewerUid ORDER BY timeMs, uid LIMIT 50")
    fun observeRanking(levelId: String, viewerUid: String): Flow<List<RankingEntity>>
    @Query("DELETE FROM leaderboard_cache WHERE levelId = :levelId AND viewerUid = :viewerUid")
    suspend fun clearRanking(levelId: String, viewerUid: String)
    @Insert suspend fun insertRanking(entries: List<RankingEntity>)
}
