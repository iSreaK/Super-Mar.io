package fr.julie.mariosprint.ui.model

import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.FriendCommand
import fr.julie.mariosprint.domain.model.FriendshipStatus

data class FriendUi(val id: String, val uid: String, val name: String, val skin: CharacterSkin, val status: FriendshipStatus)
data class FriendsState(
    val viewerUid: String? = null, val friendCode: String = "", val relations: List<FriendUi> = emptyList(),
    val refreshing: Boolean = false, val busy: Boolean = false, val error: UiText? = null,
    val realtimeConnected: Boolean = false, val cached: Boolean = true,
) {
    val incoming: List<FriendUi> get() = relations.filter { it.status == FriendshipStatus.INCOMING }
    val outgoing: List<FriendUi> get() = relations.filter { it.status == FriendshipStatus.OUTGOING }
    val friends: List<FriendUi> get() = relations.filter { it.status == FriendshipStatus.FRIEND }
}
data class PublicPlayerUi(val uid: String, val name: String, val skin: CharacterSkin)
data class ProfileRecordUi(val level: LevelId, val timeMs: Long)
data class UserProfileState(
    val viewerUid: String? = null, val playerUid: String = "", val player: PublicPlayerUi? = null,
    val friendCode: String? = null, val records: List<ProfileRecordUi> = emptyList(), val relation: FriendUi? = null,
    val loading: Boolean = false, val busy: Boolean = false, val error: UiText? = null, val cached: Boolean = true,
)
sealed interface FriendsAction {
    data object Back : FriendsAction
    data object Refresh : FriendsAction
    data object SignIn : FriendsAction
    data object CopyCode : FriendsAction
    data class OpenProfile(val uid: String) : FriendsAction
    data class Execute(val command: FriendCommand) : FriendsAction
    data class VisibilityChanged(val visible: Boolean) : FriendsAction
}
sealed interface UserProfileAction {
    data object Back : UserProfileAction
    data object Refresh : UserProfileAction
    data object SignIn : UserProfileAction
    data class Execute(val command: FriendCommand) : UserProfileAction
}
sealed interface FriendsEvent {
    data class IncomingRequests(val uid: String, val count: Int) : FriendsEvent
}
