package fr.julie.mariosprint.ui.navigation

import kotlinx.serialization.Serializable

internal sealed interface Destination {
    @Serializable data object Home : Destination
    @Serializable data object Collection : Destination
    @Serializable data object Profile : Destination
    @Serializable data object Help : Destination
    @Serializable data class Game(val levelId: String) : Destination
    @Serializable data class Leaderboard(val levelId: String) : Destination
}
