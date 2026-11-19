package fr.julie.mariosprint.ui.model

sealed interface AccountAction {
    data object SignIn : AccountAction
    class TokenReceived(val token: String, val nonce: String) : AccountAction
    data class SignInFailed(val reason: SignInFailure) : AccountAction
    data object SignOut : AccountAction
    data object SessionCleared : AccountAction
    data object ToggleSound : AccountAction
    data object Synchronize : AccountAction
}
enum class SignInFailure { CANCELED, NO_ACCOUNT, UNAVAILABLE }
sealed interface HomeAction {
    data class SelectLevel(val level: fr.julie.mariosprint.domain.game.LevelId) : HomeAction { }
    data object Play : HomeAction
    data object Leaderboard : HomeAction
    data object Collection : HomeAction
    data object Profile : HomeAction
    data object Help : HomeAction
    data object Friends : HomeAction
    data class Account(val action: AccountAction) : HomeAction
}
sealed interface LeaderboardAction {
    data object Back : LeaderboardAction
    data object Refresh : LeaderboardAction
    data class OpenProfile(val uid: String) : LeaderboardAction
    data class AddFriend(val uid: String) : LeaderboardAction
    data class Account(val action: AccountAction) : LeaderboardAction
}
sealed interface GameAction {
    data object Frame : GameAction
    data object Pause : GameAction
    data object Resume : GameAction
    data object Restart : GameAction
    data object Quit : GameAction
    data object Leaderboard : GameAction
    data object Synchronize : GameAction
}
sealed interface AccountEvent {
    data object RequestGoogleSignIn : AccountEvent
    data class ShowMessage(val text: UiText) : AccountEvent
}

sealed interface CollectionAction {
    data object Back : CollectionAction
    data object Synchronize : CollectionAction
    data class Equip(val skin: fr.julie.mariosprint.domain.model.CharacterSkin) : CollectionAction
}

sealed interface ProfileAction {
    data object Back : ProfileAction
    data object Friends : ProfileAction
    data object PublicProfile : ProfileAction
    data object About : ProfileAction
    data class Account(val action: AccountAction) : ProfileAction
}

sealed interface HelpAction {
    data object Back : HelpAction
}
