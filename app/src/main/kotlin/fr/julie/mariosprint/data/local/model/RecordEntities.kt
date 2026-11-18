package fr.julie.mariosprint.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_stats")
internal data class StatsEntity(@PrimaryKey val levelId: String, val bestMs: Long? = null,
    val attempts: Int = 0, val finishes: Int = 0, val coins: Int = 0)

@Entity(tableName = "pending_records", primaryKeys = ["levelId", "uid"])
internal data class PendingRecordEntity(val levelId: String, val uid: String, val timeMs: Long, val coins: Int)

@Entity(tableName = "settings")
internal data class SettingsEntity(@PrimaryKey val id: Int = 1, val sound: Boolean = true)

@Entity(tableName = "leaderboard_cache", primaryKeys = ["levelId", "viewerUid", "uid"])
internal data class RankingEntity(val levelId: String, val viewerUid: String, val uid: String,
    val name: String, val timeMs: Long, val coins: Int)
