package fr.julie.mariosprint.data.remote

import fr.julie.mariosprint.domain.model.*
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

internal class SupabaseFriendsSource(private val api: SupabaseApi, private val tokens: AccessTokenProvider) {
    suspend fun snapshot(uid: String) = request { tokens.authorized(uid) { api.friends(it) } }
    suspend fun profile(viewerUid: String, playerUid: String) = request {
        tokens.authorized(viewerUid) { api.socialProfile(it, UserProfileRequest(playerUid)) }
    }
    suspend fun execute(uid: String, command: FriendCommand): FriendsSnapshotRemote {
        val body = when (command) {
            is FriendCommand.SendToPlayer -> {
                if (command.uid == uid) throw FriendsException(FriendsFailure.SELF)
                FriendActionRequest("send", targetUid = command.uid)
            }
            is FriendCommand.SendByCode -> FriendActionRequest("send", code = FriendCode.normalize(command.code)
                ?: throw FriendsException(FriendsFailure.INVALID_CODE))
            is FriendCommand.Accept -> FriendActionRequest("accept", relationId = command.relationId)
            is FriendCommand.Decline -> FriendActionRequest("decline", relationId = command.relationId)
            is FriendCommand.Cancel -> FriendActionRequest("cancel", relationId = command.relationId)
            is FriendCommand.Remove -> FriendActionRequest("remove", relationId = command.relationId)
        }
        return request { tokens.authorized(uid) { api.friendAction(it, body) } }
    }
    private suspend fun <T> request(block: suspend () -> T): T = try { block() }
    catch (error: CancellationException) { throw error }
    catch (error: HttpException) {
        val message = try { error.response()?.errorBody()?.string()?.take(4096)?.let {
            SupabaseClient.json.decodeFromString<SocialErrorRemote>(it).message
        } } catch (_: Exception) { null }
        throw FriendsException(when (message) {
            "friend_invalid_code" -> FriendsFailure.INVALID_CODE
            "friend_self" -> FriendsFailure.SELF
            "friend_not_found" -> FriendsFailure.NOT_FOUND
            "friend_limit", "friend_rate_limit" -> FriendsFailure.LIMIT
            "friend_forbidden" -> FriendsFailure.FORBIDDEN
            else -> FriendsFailure.UNAVAILABLE
        })
    } catch (_: IOException) { throw FriendsException(FriendsFailure.NETWORK) }
}
