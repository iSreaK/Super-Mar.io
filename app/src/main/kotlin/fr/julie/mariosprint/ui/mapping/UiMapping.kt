package fr.julie.mariosprint.ui.mapping

import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.ui.model.*

internal fun PlayerAccount.toUi() = PlayerUi(uid, name)
internal fun PersonalStats.toUi() = StatsUi(bestMs, attempts, finishes, coins)
internal fun LeaderboardEntry.toUi() = RankingEntryUi(uid, name, timeMs)
