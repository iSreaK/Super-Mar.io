package fr.julie.mariosprint.data.local.dao

import androidx.room.*
import fr.julie.mariosprint.data.local.model.*
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FriendsDao {
    @Transaction @Query("SELECT * FROM friends_cache WHERE viewerUid = :uid")
    fun observe(uid: String): Flow<FriendsCacheWithRelations?>
    @Query("SELECT * FROM friends_cache WHERE viewerUid = :uid")
    suspend fun metadata(uid: String): FriendsCacheEntity?
    @Query("SELECT relationId FROM friend_relations_cache WHERE viewerUid = :uid AND status = 'incoming'")
    suspend fun incomingIds(uid: String): List<String>
    @Upsert suspend fun saveMetadata(value: FriendsCacheEntity)
    @Query("DELETE FROM friend_relations_cache WHERE viewerUid = :uid") suspend fun clearRelations(uid: String)
    @Insert suspend fun saveRelations(values: List<FriendRelationEntity>)
    @Query("""SELECT p.*, r.levelId, r.timeMs FROM social_profiles_cache p LEFT JOIN social_profile_records r
        ON r.viewerUid = p.viewerUid AND r.profileUid = p.profileUid
        WHERE p.viewerUid = :viewerUid AND p.profileUid = :playerUid ORDER BY r.levelId""")
    fun observeProfile(viewerUid: String, playerUid: String): Flow<List<SocialProfileRow>>
    @Upsert suspend fun saveProfile(value: SocialProfileEntity)
    @Query("DELETE FROM social_profile_records WHERE viewerUid = :viewerUid AND profileUid = :playerUid")
    suspend fun clearProfileRecords(viewerUid: String, playerUid: String)
    @Insert suspend fun saveProfileRecords(values: List<SocialProfileRecordEntity>)
    @Query("""DELETE FROM social_profiles_cache WHERE viewerUid = :uid AND profileUid NOT IN
        (SELECT profileUid FROM social_profiles_cache WHERE viewerUid = :uid ORDER BY refreshedAtMs DESC, profileUid LIMIT 50)""")
    suspend fun trimProfiles(uid: String)
}
