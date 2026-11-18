package fr.julie.mariosprint.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.julie.mariosprint.data.local.dao.RecordsDao
import fr.julie.mariosprint.data.local.dao.ProgressDao
import fr.julie.mariosprint.data.local.model.*

@Database(entities = [StatsEntity::class, PendingRecordEntity::class, SettingsEntity::class, RankingEntity::class,
    CompletedLevelEntity::class, AppearanceEntity::class], version = 2, exportSchema = true)
internal abstract class SprintDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao
    abstract fun progressDao(): ProgressDao
}
