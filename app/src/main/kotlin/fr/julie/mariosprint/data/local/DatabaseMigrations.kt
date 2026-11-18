package fr.julie.mariosprint.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseMigrations {
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `completed_levels` (`owner` TEXT NOT NULL, `levelId` TEXT NOT NULL, PRIMARY KEY(`owner`, `levelId`))""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `player_appearance` (`owner` TEXT NOT NULL, `skinId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `pending` INTEGER NOT NULL, PRIMARY KEY(`owner`))""")
        }
    }
    val FROM_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `friends_cache` (`viewerUid` TEXT NOT NULL, `friendCode` TEXT NOT NULL, `revision` INTEGER NOT NULL, `refreshedAtMs` INTEGER NOT NULL, PRIMARY KEY(`viewerUid`))""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `friend_relations_cache` (`viewerUid` TEXT NOT NULL, `peerUid` TEXT NOT NULL, `relationId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `skinId` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`viewerUid`, `peerUid`), FOREIGN KEY(`viewerUid`) REFERENCES `friends_cache`(`viewerUid`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `social_profiles_cache` (`viewerUid` TEXT NOT NULL, `profileUid` TEXT NOT NULL, `displayName` TEXT NOT NULL, `skinId` TEXT NOT NULL, `friendCode` TEXT, `refreshedAtMs` INTEGER NOT NULL, PRIMARY KEY(`viewerUid`, `profileUid`))""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `social_profile_records` (`viewerUid` TEXT NOT NULL, `profileUid` TEXT NOT NULL, `levelId` TEXT NOT NULL, `timeMs` INTEGER NOT NULL, PRIMARY KEY(`viewerUid`, `profileUid`, `levelId`), FOREIGN KEY(`viewerUid`, `profileUid`) REFERENCES `social_profiles_cache`(`viewerUid`, `profileUid`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        }
    }
}
