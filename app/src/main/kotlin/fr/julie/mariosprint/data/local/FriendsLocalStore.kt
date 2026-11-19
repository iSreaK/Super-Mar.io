package fr.julie.mariosprint.data.local

import androidx.room.withTransaction
import fr.julie.mariosprint.data.local.model.*
import kotlinx.coroutines.flow.Flow

internal interface FriendsLocalStore {
    fun observe(uid: String): Flow<FriendsCacheWithRelations?>
    fun observeProfile(viewerUid: String, playerUid: String): Flow<List<SocialProfileRow>>
    suspend fun replace(cache: FriendsCacheEntity, relations: List<FriendRelationEntity>): Int
    suspend fun saveProfile(profile: SocialProfileEntity, records: List<SocialProfileRecordEntity>)
}

internal class RoomFriendsLocalStore(private val database: SprintDatabase) : FriendsLocalStore {
    private val dao = database.friendsDao()
    override fun observe(uid: String) = dao.observe(uid)
    override fun observeProfile(viewerUid: String, playerUid: String) = dao.observeProfile(viewerUid, playerUid)
    override suspend fun replace(cache: FriendsCacheEntity, relations: List<FriendRelationEntity>): Int = database.withTransaction {
        require(relations.all { it.viewerUid == cache.viewerUid })
        val previous = dao.metadata(cache.viewerUid)
        if (previous != null && previous.revision > cache.revision) return@withTransaction 0
        val known = dao.incomingIds(cache.viewerUid).toSet()
        dao.saveMetadata(cache)
        dao.clearRelations(cache.viewerUid)
        dao.saveRelations(relations)
        relations.count { it.status == "incoming" && it.relationId !in known }
    }
    override suspend fun saveProfile(profile: SocialProfileEntity, records: List<SocialProfileRecordEntity>) = database.withTransaction {
        require(records.all { it.viewerUid == profile.viewerUid && it.profileUid == profile.profileUid })
        dao.saveProfile(profile)
        dao.clearProfileRecords(profile.viewerUid, profile.profileUid)
        dao.saveProfileRecords(records)
        dao.trimProfiles(profile.viewerUid)
    }
}
