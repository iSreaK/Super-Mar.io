package fr.julie.mariosprint.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import fr.julie.mariosprint.data.local.model.AppearanceEntity
import fr.julie.mariosprint.data.local.model.CompletedLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ProgressDao {
    @Query("SELECT * FROM completed_levels WHERE owner = :owner ORDER BY levelId")
    fun observeCompleted(owner: String): Flow<List<CompletedLevelEntity>>

    @Query("SELECT * FROM completed_levels WHERE owner = :owner ORDER BY levelId")
    suspend fun completed(owner: String): List<CompletedLevelEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompleted(values: List<CompletedLevelEntity>)

    @Query("SELECT * FROM player_appearance WHERE owner = :owner")
    fun observeAppearance(owner: String): Flow<AppearanceEntity?>

    @Query("SELECT * FROM player_appearance WHERE owner = :owner")
    suspend fun appearance(owner: String): AppearanceEntity?

    @Upsert
    suspend fun saveAppearance(value: AppearanceEntity)
}
