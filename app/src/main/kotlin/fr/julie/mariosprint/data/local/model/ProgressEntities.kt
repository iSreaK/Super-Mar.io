package fr.julie.mariosprint.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_levels", primaryKeys = ["owner", "levelId"])
internal data class CompletedLevelEntity(val owner: String, val levelId: String)

@Entity(tableName = "player_appearance")
internal data class AppearanceEntity(
    @PrimaryKey val owner: String,
    val skinId: String = "luigi",
    val revision: Long = 0,
    val pending: Boolean = false,
)
