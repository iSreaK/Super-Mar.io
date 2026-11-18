package fr.julie.mariosprint.domain.model

import fr.julie.mariosprint.domain.game.Difficulty
import fr.julie.mariosprint.domain.game.LevelId

enum class CharacterSkin(val value: String) {
    LUIGI("luigi"), TOAD("toad"), YOSHI("yoshi"), PEACH("peach");

    companion object {
        fun find(value: String): CharacterSkin? = entries.firstOrNull { it.value == value }
    }
}

enum class MissionId(val target: Int, val reward: CharacterSkin) {
    FIRST_FINISH(1, CharacterSkin.TOAD),
    THREE_WORLDS(3, CharacterSkin.YOSHI),
    EXPERT_FINISHES(3, CharacterSkin.PEACH);

    fun progress(completedLevels: Set<LevelId>): Int = when (this) {
        FIRST_FINISH -> completedLevels.size.coerceAtMost(target)
        THREE_WORLDS -> completedLevels.map { it.theme }.distinct().size
        EXPERT_FINISHES -> completedLevels.count { it.difficulty == Difficulty.EXPERT }.coerceAtMost(target)
    }
}

data class PlayerProgress(
    val completedLevels: Set<LevelId> = emptySet(),
    val equippedSkin: CharacterSkin = CharacterSkin.LUIGI,
    val pendingSelection: Boolean = false,
) {
    val unlockedSkins: Set<CharacterSkin> get() = buildSet {
        add(CharacterSkin.LUIGI)
        MissionId.entries.filter { it.progress(completedLevels) >= it.target }.forEach { add(it.reward) }
    }
}
