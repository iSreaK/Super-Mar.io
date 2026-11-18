package fr.julie.mariosprint.ui.model

import fr.julie.mariosprint.domain.game.Phase
import fr.julie.mariosprint.domain.game.LevelId

data class PlayerUi(val uid: String, val name: String)
data class StatsUi(val bestMs: Long? = null, val attempts: Int = 0, val finishes: Int = 0, val coins: Int = 0)
data class RankingEntryUi(val uid: String, val name: String, val timeMs: Long)
data class ProfileState(val player: PlayerUi? = null, val sound: Boolean = true,
    val configured: Boolean = false, val busy: Boolean = false, val syncing: Boolean = false)
data class HomeState(val selectedLevel: LevelId = LevelId.MEADOW, val legacyBestMs: Long? = null, val stats: StatsUi = StatsUi(), val profile: ProfileState = ProfileState(), val error: UiText? = null)
data class LeaderboardState(val level: LevelId = LevelId.MEADOW, val entries: List<RankingEntryUi> = emptyList(), val loading: Boolean = false,
    val error: UiText? = null, val cached: Boolean = true, val stats: StatsUi = StatsUi(), val profile: ProfileState = ProfileState())
enum class RecordStatus { NONE, PRACTICE, LOCAL, SYNCING, SYNCED, PENDING, SAVE_FAILED }
data class GameState(val level: LevelId = LevelId.MEADOW, val starSeconds: Int = 0, val magnetSeconds: Int = 0,
    val shield: Boolean = false, val personalBest: Boolean = false, val phase: Phase = Phase.READY, val timeMs: Long = 0, val coins: Int = 0,
    val progress: Float = 0f, val eligible: Boolean = true, val sessionId: Int = 0, val loading: Boolean = true,
    val stats: StatsUi = StatsUi(), val recordStatus: RecordStatus = RecordStatus.NONE, val profile: ProfileState = ProfileState())
