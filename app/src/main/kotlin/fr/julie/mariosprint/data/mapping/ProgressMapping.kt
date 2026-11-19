package fr.julie.mariosprint.data.mapping

import fr.julie.mariosprint.data.local.model.AppearanceEntity
import fr.julie.mariosprint.data.local.model.CompletedLevelEntity
import fr.julie.mariosprint.data.remote.ProgressRemote
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.PlayerProgress

/** Borne de la réponse distante : bien au-dessus des neuf parcours, sans accepter une liste démesurée. */
private const val MAX_REMOTE_COMPLETED_LEVELS = 50

internal fun progressOwner(uid: String?): String = uid?.let { "account:$it" } ?: "guest"

/**
 * Règle commune au cache Room et à la réponse Supabase : un parcours ou un personnage inconnu de cette
 * version est ignoré. Il ne débloque rien, et il n'empêche pas de lire le reste de la progression.
 */
private fun progressOf(levelIds: List<String>, skinId: String?): PlayerProgress {
    val completed = levelIds.mapNotNull { value -> LevelId.entries.firstOrNull { it.value == value } }.toSet()
    val progress = PlayerProgress(completedLevels = completed)
    val skin = skinId?.let(CharacterSkin::find)?.takeIf { it in progress.unlockedSkins } ?: CharacterSkin.LUIGI
    return progress.copy(equippedSkin = skin)
}

internal fun List<CompletedLevelEntity>.toProgress(appearance: AppearanceEntity?): PlayerProgress =
    progressOf(map { it.levelId }, appearance?.skinId).copy(pendingSelection = appearance?.pending == true)

internal fun ProgressRemote.toProgress(): PlayerProgress {
    require(completedLevels.size <= MAX_REMOTE_COMPLETED_LEVELS)
    return progressOf(completedLevels, equippedSkin)
}

internal fun AppearanceEntity.acceptsRemoteSelection(snapshot: AppearanceEntity, serverSkin: CharacterSkin): Boolean =
    revision == snapshot.revision && skinId == snapshot.skinId && (!snapshot.pending || serverSkin.value == snapshot.skinId)
