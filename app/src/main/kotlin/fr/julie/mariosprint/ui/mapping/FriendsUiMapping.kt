package fr.julie.mariosprint.ui.mapping

import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.ui.model.*
import kotlinx.coroutines.TimeoutCancellationException

internal fun Friendship.toUi() = FriendUi(id, player.uid, player.name, player.skin, status)
internal fun Throwable.toFriendsUiError() = UiText(when {
    this is TimeoutCancellationException -> R.string.friends_network_error
    this is FriendsException -> when (failure) {
        FriendsFailure.INVALID_CODE -> R.string.friends_invalid_code
        FriendsFailure.NOT_FOUND -> R.string.friends_player_not_found
        FriendsFailure.SELF -> R.string.friends_self_error
        FriendsFailure.FORBIDDEN -> R.string.friends_action_unavailable
        FriendsFailure.LIMIT -> R.string.friends_limit_error
        FriendsFailure.NETWORK -> R.string.friends_network_error
        FriendsFailure.LOCAL -> R.string.local_save_failed
        FriendsFailure.UNAVAILABLE -> R.string.friends_service_error
    }
    else -> R.string.friends_service_error
})
