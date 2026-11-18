package fr.julie.mariosprint.domain.model

enum class FriendshipStatus { INCOMING, OUTGOING, FRIEND }

data class SocialPlayer(val uid: String, val name: String, val skin: CharacterSkin = CharacterSkin.LUIGI)
data class Friendship(val id: String, val player: SocialPlayer, val status: FriendshipStatus)
data class FriendsSnapshot(
    val viewerUid: String,
    val friendCode: String = "",
    val relations: List<Friendship> = emptyList(),
    val refreshedAtMs: Long = 0,
)
data class ProfileRecord(val levelId: String, val timeMs: Long)
data class UserProfile(val player: SocialPlayer, val friendCode: String?, val records: List<ProfileRecord>)

sealed interface FriendCommand {
    data class SendToPlayer(val uid: String) : FriendCommand
    data class SendByCode(val code: String) : FriendCommand
    data class Accept(val relationId: String) : FriendCommand
    data class Decline(val relationId: String) : FriendCommand
    data class Cancel(val relationId: String) : FriendCommand
    data class Remove(val relationId: String) : FriendCommand
}

enum class FriendsLiveEvent { CONNECTED, CHANGED, DISCONNECTED }
data class IncomingFriendRequests(val viewerUid: String, val count: Int)
enum class FriendsFailure { INVALID_CODE, NOT_FOUND, SELF, FORBIDDEN, LIMIT, NETWORK, UNAVAILABLE, LOCAL }
class FriendsException(val failure: FriendsFailure) : Exception(failure.name)

object FriendCode {
    fun normalize(value: String): String? = value.filterNot { it.isWhitespace() || it == '-' }
        .uppercase().takeIf { it.matches(Regex("[A-F0-9]{12}")) }
    fun format(value: String): String = normalize(value)?.chunked(4)?.joinToString("-") ?: value
}
