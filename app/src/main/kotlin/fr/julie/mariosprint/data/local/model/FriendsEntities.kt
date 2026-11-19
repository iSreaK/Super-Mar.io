package fr.julie.mariosprint.data.local.model

import androidx.room.*

@Entity(tableName = "friends_cache")
internal data class FriendsCacheEntity(@PrimaryKey val viewerUid: String, val friendCode: String,
    val revision: Long, val refreshedAtMs: Long)

@Entity(tableName = "friend_relations_cache", primaryKeys = ["viewerUid", "peerUid"],
    foreignKeys = [ForeignKey(entity = FriendsCacheEntity::class, parentColumns = ["viewerUid"],
        childColumns = ["viewerUid"], onDelete = ForeignKey.CASCADE)])
internal data class FriendRelationEntity(val viewerUid: String, val peerUid: String, val relationId: String,
    val displayName: String, val skinId: String, val status: String)

internal data class FriendsCacheWithRelations(@Embedded val cache: FriendsCacheEntity,
    @Relation(parentColumn = "viewerUid", entityColumn = "viewerUid") val relations: List<FriendRelationEntity>)

@Entity(tableName = "social_profiles_cache", primaryKeys = ["viewerUid", "profileUid"])
internal data class SocialProfileEntity(val viewerUid: String, val profileUid: String, val displayName: String,
    val skinId: String, val friendCode: String?, val refreshedAtMs: Long)

@Entity(tableName = "social_profile_records", primaryKeys = ["viewerUid", "profileUid", "levelId"],
    foreignKeys = [ForeignKey(entity = SocialProfileEntity::class, parentColumns = ["viewerUid", "profileUid"],
        childColumns = ["viewerUid", "profileUid"], onDelete = ForeignKey.CASCADE)])
internal data class SocialProfileRecordEntity(val viewerUid: String, val profileUid: String, val levelId: String, val timeMs: Long)

internal data class SocialProfileRow(@Embedded val profile: SocialProfileEntity, val levelId: String?, val timeMs: Long?)
