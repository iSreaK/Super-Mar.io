package fr.julie.mariosprint.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable internal data class FriendRelationRemote(
    val id: String, val uid: String, @SerialName("display_name") val displayName: String,
    @SerialName("skin_id") val skinId: String, val status: String,
)
@Serializable internal data class FriendsSnapshotRemote(
    @SerialName("viewer_uid") val viewerUid: String, val revision: Long,
    @SerialName("friend_code") val friendCode: String, val relations: List<FriendRelationRemote>,
)
@Serializable internal data class FriendActionRequest(
    @SerialName("p_action") val action: String,
    @SerialName("p_target_uid") val targetUid: String? = null,
    @SerialName("p_code") val code: String? = null,
    @SerialName("p_relation_id") val relationId: String? = null,
)
@Serializable internal data class UserProfileRequest(@SerialName("p_uid") val uid: String)
@Serializable internal data class ProfileRecordRemote(@SerialName("level_id") val levelId: String, @SerialName("time_ms") val timeMs: Long)
@Serializable internal data class UserProfileRemote(
    val uid: String, @SerialName("display_name") val displayName: String,
    @SerialName("skin_id") val skinId: String, @SerialName("friend_code") val friendCode: String? = null,
    val records: List<ProfileRecordRemote>,
)
@Serializable internal data class SocialErrorRemote(val message: String = "")
