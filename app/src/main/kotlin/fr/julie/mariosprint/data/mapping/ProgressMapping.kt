package fr.julie.mariosprint.data.mapping

import fr.julie.mariosprint.data.local.model.AppearanceEntity
import fr.julie.mariosprint.data.local.model.CompletedLevelEntity
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.PlayerProgress

internal fun progressOwner(uid: String?): String = uid?.let { "account:$it" } ?: "guest"

internal fun List<CompletedLevelEntity>.toProgress(appearance: AppearanceEntity?): PlayerProgress {
    val completed = mapNotNull { row -> LevelId.entries.firstOrNull { it.value == row.levelId } }.toSet()
    val progress = PlayerProgress(completedLevels = completed)
    val skin = appearance?.skinId?.let(CharacterSkin::find)?.takeIf { it in progress.unlockedSkins } ?: CharacterSkin.LUIGI
    return progress.copy(equippedSkin = skin, pendingSelection = appearance?.pending == true)
}

internal fun AppearanceEntity.acceptsRemoteSelection(snapshot: AppearanceEntity, serverSkin: CharacterSkin): Boolean =
    revision == snapshot.revision && skinId == snapshot.skinId && (!snapshot.pending || serverSkin.value == snapshot.skinId)
