package fr.julie.mariosprint.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import fr.julie.mariosprint.data.local.dao.RecordsDao
import fr.julie.mariosprint.data.local.dao.ProgressDao
import fr.julie.mariosprint.data.local.dao.FriendsDao
import fr.julie.mariosprint.data.local.model.*

@Database(entities = [StatsEntity::class, PendingRecordEntity::class, SettingsEntity::class, RankingEntity::class,
    CompletedLevelEntity::class, AppearanceEntity::class, FriendsCacheEntity::class, FriendRelationEntity::class,
    SocialProfileEntity::class, SocialProfileRecordEntity::class], version = 3, exportSchema = true)
internal abstract class SprintDatabase : RoomDatabase() {
    abstract fun recordsDao(): RecordsDao
    abstract fun progressDao(): ProgressDao
    abstract fun friendsDao(): FriendsDao
}
