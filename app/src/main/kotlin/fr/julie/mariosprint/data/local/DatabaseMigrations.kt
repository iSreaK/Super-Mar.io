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
}
