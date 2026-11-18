package fr.julie.mariosprint.data.mapping

import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.model.*

internal fun StatsEntity.toDomain() = PersonalStats(bestMs, attempts, finishes, coins)
internal fun PendingRecordEntity.toDomain() = RunResult(timeMs, coins, true, levelId)
internal fun RankingEntity.toDomain() = LeaderboardEntry(uid, name, timeMs, coins)
internal fun LeaderboardEntry.toEntity(levelId: String, viewerUid: String) =
    RankingEntity(levelId, viewerUid, uid, name, timeMs, coins)
