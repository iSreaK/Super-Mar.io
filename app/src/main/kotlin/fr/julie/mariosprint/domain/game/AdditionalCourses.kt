package fr.julie.mariosprint.domain.game

internal data class CourseLayout(
    val tiles: Array<Tile>,
    val coins: MutableList<Coin>,
    val enemies: List<EnemySpawn>,
    val bonuses: List<Bonus>,
)

/** Plans fixes : pas de génération aléatoire des parcours classés. */
internal object AdditionalCourses {
    private data class Plan(
        val pits: List<IntRange>,
        val terraces: List<Int>,
        val blocks: List<Int>,
        val enemies: List<Int>,
        val coinLines: List<Int>,
        val bonuses: List<Int>,
    )

    fun create(id: LevelId): CourseLayout {
        val plan = when (id) {
            LevelId.MEADOW_TRAILS -> Plan(listOf(37..39, 78..80, 120..122, 163..165),
                listOf(24, 58, 101, 143, 191), listOf(12, 49, 91, 132, 180),
                listOf(31, 69, 111, 154, 202), listOf(7, 35, 76, 118, 161, 209), listOf(9, 68, 176))
            LevelId.MEADOW_SUMMITS -> Plan(listOf(31..34, 69..72, 111..114, 151..154, 191..194),
                listOf(17, 50, 91, 131, 171, 205), listOf(40, 80, 120, 160, 199),
                listOf(23, 43, 61, 83, 124, 143, 165, 183, 216),
                listOf(7, 29, 67, 109, 149, 189), listOf(10, 59, 179))
            LevelId.CANYON_OASIS -> Plan(listOf(53..54, 109..110, 169..170),
                listOf(29, 85, 143, 197), listOf(16, 70, 129, 182),
                listOf(43, 101, 158, 211), listOf(7, 51, 75, 107, 167, 211), listOf(10, 67, 185))
            LevelId.CANYON_RIDGES -> Plan(listOf(39..42, 80..83, 123..126, 164..167, 200..203),
                listOf(24, 63, 104, 145, 184), listOf(12, 52, 93, 134, 175, 210),
                listOf(31, 56, 97, 116, 138, 157, 179, 194, 216),
                listOf(7, 37, 78, 121, 162, 198), listOf(10, 71, 177))
            LevelId.NIGHT_LANTERNS -> Plan(listOf(46..47, 104..105, 164..165),
                listOf(23, 80, 139, 191), listOf(12, 62, 122, 178),
                listOf(36, 94, 154, 207), listOf(7, 44, 67, 102, 162, 211), listOf(10, 59, 181))
            LevelId.NIGHT_OBSERVATORY -> Plan(listOf(35..37, 76..78, 118..120, 160..162, 199..201),
                listOf(19, 58, 100, 141, 183), listOf(44, 87, 129, 172, 208),
                listOf(27, 49, 69, 92, 111, 134, 153, 176, 193, 216),
                listOf(7, 33, 74, 116, 158, 197), listOf(10, 66, 174))
            else -> error("Ce parcours possède déjà son plan historique")
        }
        val tiles = Array(Level.COLS * Level.ROWS) { Tile.AIR }
        fun put(column: Int, row: Int, tile: Tile) { tiles[row * Level.COLS + column] = tile }
        for (column in 0 until Level.COLS) {
            if (plan.pits.none { column in it }) for (row in 13..14) put(column, row, Tile.GROUND)
        }
        val terraceHeight = if (id.difficulty == Difficulty.EASY) 2 else 3
        plan.terraces.forEach { start ->
            for (offset in 0..4) for (height in 0 until minOf(offset + 1, 5 - offset, terraceHeight)) {
                put(start + offset, 12 - height, Tile.STONE)
            }
        }
        plan.blocks.forEachIndexed { index, start ->
            for (column in start..start + 3) put(column, 9, Tile.BRICK)
            put(start + 1, 9, if (index % 3 == 0) Tile.QUESTION_MUSHROOM else Tile.QUESTION_COIN)
        }
        val coins = mutableListOf<Coin>()
        plan.coinLines.forEachIndexed { index, start ->
            repeat(5) { coins += Coin((start + it) * Level.SIZE + 4, (if (index == 0) 11 else 8) * Level.SIZE + 3) }
        }
        return CourseLayout(tiles, coins,
            plan.enemies.map { EnemySpawn(it * Level.SIZE, 12 * Level.SIZE) },
            BonusType.entries.mapIndexed { index, type -> Bonus(type, plan.bonuses[index] * Level.SIZE, 12 * Level.SIZE) })
    }
}
