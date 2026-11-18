package fr.julie.mariosprint.domain.game

import kotlin.math.floor

enum class Tile { AIR, GROUND, BRICK, QUESTION_COIN, QUESTION_MUSHROOM, USED, PIPE, PIPE_TOP, STONE }
data class Coin(var x: Float, var y: Float, var collected: Boolean = false)
enum class LevelTheme { MEADOW, CANYON, NIGHT }
enum class Difficulty { EASY, NORMAL, EXPERT }
enum class BonusType { STAR, SHIELD, MAGNET }
data class Bonus(val type: BonusType, val x: Float, val y: Float, var collected: Boolean = false)
enum class LevelId(val value: String, val version: Int, val theme: LevelTheme, val difficulty: Difficulty) {
    MEADOW("prairie-1-v2", 2, LevelTheme.MEADOW, Difficulty.EASY),
    CANYON("canyon-2-v1", 1, LevelTheme.CANYON, Difficulty.NORMAL),
    NIGHT("night-3-v1", 1, LevelTheme.NIGHT, Difficulty.EXPERT),
    MEADOW_TRAILS("prairie-trails-v1", 1, LevelTheme.MEADOW, Difficulty.NORMAL),
    MEADOW_SUMMITS("prairie-summits-v1", 1, LevelTheme.MEADOW, Difficulty.EXPERT),
    CANYON_OASIS("canyon-oasis-v1", 1, LevelTheme.CANYON, Difficulty.EASY),
    CANYON_RIDGES("canyon-ridges-v1", 1, LevelTheme.CANYON, Difficulty.EXPERT),
    NIGHT_LANTERNS("night-lanterns-v1", 1, LevelTheme.NIGHT, Difficulty.EASY),
    NIGHT_OBSERVATORY("night-observatory-v1", 1, LevelTheme.NIGHT, Difficulty.NORMAL);
    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: MEADOW
        val rankedIds = entries.map { it.value }
    }
}
data class EnemySpawn(val x: Float, val y: Float)

/** Coordonnées indépendantes de l'écran : 1 case = 16 unités. */
class Level private constructor(
    val id: LevelId,
    val tiles: Array<Tile>,
    val coins: MutableList<Coin>,
    val enemies: List<EnemySpawn>,
    val bonuses: List<Bonus>,
) {
    fun tile(col: Int, row: Int): Tile = when {
        row < 0 || row >= ROWS -> Tile.AIR
        col < 0 || col >= COLS -> Tile.STONE
        else -> tiles[row * COLS + col]
    }
    fun setTile(col: Int, row: Int, tile: Tile) { tiles[row * COLS + col] = tile }
    fun solidAt(x: Float, y: Float) = tile(floor(x / SIZE).toInt(), floor(y / SIZE).toInt()) != Tile.AIR

    companion object {
        const val LEGACY_ID = "prairie-1-v1"
        const val ID = "prairie-1-v2"
        const val VERSION = 2
        const val SIZE = 16f
        const val COLS = 232
        const val ROWS = 15
        const val HEIGHT = 240f
        const val FLAG_X = 220 * SIZE
        const val START_X = 3 * SIZE
        const val START_Y = 12 * SIZE
        const val MAX_TIME_MS = 600_000L

        private fun bonuses(star: Int, shield: Int, magnet: Int) = listOf(
            Bonus(BonusType.STAR, star * SIZE, 12 * SIZE),
            Bonus(BonusType.SHIELD, shield * SIZE, 12 * SIZE),
            Bonus(BonusType.MAGNET, magnet * SIZE, 12 * SIZE),
        )
        fun create(id: LevelId): Level = when (id) {
            LevelId.MEADOW -> prairie()
            LevelId.CANYON, LevelId.NIGHT -> adventure(id)
            else -> AdditionalCourses.create(id).let { Level(id, it.tiles, it.coins, it.enemies, it.bonuses) }
        }

        /** Deux tracés distincts : petites terrasses du canyon et plateformes nocturnes. */
        private fun adventure(id: LevelId): Level {
            val tiles = Array(COLS * ROWS) { Tile.AIR }
            fun put(x: Int, y: Int, tile: Tile) { tiles[y * COLS + x] = tile }
            val canyon = id == LevelId.CANYON
            val pits = if (canyon) listOf(33..35, 70..73, 111..114, 152..154, 180..183)
                else listOf(28..31, 63..66, 99..102, 136..139, 174..177, 199..201)
            for (x in 0 until COLS) if (pits.none { x in it }) for (y in 13..14) put(x, y, Tile.GROUND)
            val terraces = if (canyon) listOf(19, 47, 88, 128, 164, 191) else listOf(43, 80, 117, 154, 185)
            for (start in terraces) {
                // Au plus trois cases de haut ; une marche de chaque côté.
                for (i in 0..4) for (h in 0 until minOf(i + 1, 5 - i, 3)) put(start + i, 12 - h, Tile.STONE)
                for (x in start + 1..start + 4) put(x, 6, Tile.BRICK)
                put(start + 2, 6, Tile.QUESTION_COIN)
            }
            for (x in if (canyon) listOf(58, 141) else listOf(16, 109, 165)) {
                for (c in x..x + 3) put(c, 9, Tile.BRICK)
                put(x + 1, 9, Tile.QUESTION_MUSHROOM)
            }
            val coins = mutableListOf<Coin>()
            for (start in if (canyon) listOf(8, 31, 56, 68, 109, 150, 178, 204) else listOf(7, 26, 61, 97, 134, 172, 197, 210)) {
                repeat(5) { coins += Coin((start + it) * SIZE + 4, (if (start in 7..10 || start > 202) 11 else 8) * SIZE + 3) }
            }
            val enemies = (if (canyon) listOf(40, 62, 82, 120, 146, 172, 206)
                else listOf(22, 53, 74, 93, 126, 147, 165, 189, 209)).map { EnemySpawn(it * SIZE, 12 * SIZE) }
            return Level(id, tiles, coins, enemies, if (canyon) bonuses(10, 66, 145) else bonuses(12, 56, 129))
        }

        /** Niveau original : apprentissage, fossés, chemins hauts, puis escalier final. */
        fun prairie(): Level {
            val tiles = Array(COLS * ROWS) { Tile.AIR }
            fun put(x: Int, y: Int, t: Tile) { tiles[y * COLS + x] = t }
            val pits = listOf(38..40, 76..79, 119..122, 159..162)
            for (x in 0 until COLS) if (pits.none { x in it }) {
                put(x, 13, Tile.GROUND); put(x, 14, Tile.GROUND)
            }
            fun blocks(start: Int, end: Int, row: Int) {
                for (x in start..end) put(x, row, Tile.BRICK)
            }
            fun pipe(x: Int, height: Int) {
                for (y in (13 - height)..12) for (c in x..(x + 1)) {
                    put(c, y, if (y == 13 - height) Tile.PIPE_TOP else Tile.PIPE)
                }
            }
            blocks(16, 20, 9)
            put(17, 9, Tile.QUESTION_COIN); put(19, 9, Tile.QUESTION_MUSHROOM)
            pipe(28, 2)
            blocks(45, 49, 9); put(47, 9, Tile.QUESTION_COIN)
            pipe(57, 3); pipe(66, 2)
            blocks(85, 90, 9); put(87, 9, Tile.QUESTION_COIN)
            blocks(94, 99, 6)
            blocks(106, 110, 9); put(108, 9, Tile.QUESTION_MUSHROOM)
            pipe(132, 2)
            blocks(140, 145, 9); put(143, 9, Tile.QUESTION_COIN)
            blocks(168, 174, 9)
            for (i in 0..3) for (h in 0..i) put(185 + i, 12 - h, Tile.STONE)
            for (i in 0..3) for (h in 0..(3 - i)) put(192 + i, 12 - h, Tile.STONE)
            for (i in 0..5) for (h in 0..i) put(208 + i, 12 - h, Tile.STONE)
            val coins = mutableListOf<Coin>()
            fun line(start: Int, count: Int, row: Int) {
                repeat(count) { coins += Coin((start + it) * SIZE + 4, row * SIZE + 3) }
            }
            line(8, 5, 11); line(16, 5, 7); line(37, 5, 9)
            line(46, 4, 7); line(74, 6, 8); line(85, 6, 7)
            line(94, 6, 4); line(118, 6, 8); line(140, 6, 7)
            line(158, 6, 8); line(168, 7, 7); line(198, 5, 11)
            return Level(LevelId.MEADOW, tiles, coins, listOf(23, 48, 63, 89, 103, 137, 151, 177, 200)
                .map { EnemySpawn(it * SIZE, 12 * SIZE) }, bonuses(12, 53, 148))
        }
    }
}
