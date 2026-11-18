package fr.julie.mariosprint.game.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import fr.julie.mariosprint.domain.game.GameEngine
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.game.Phase
import fr.julie.mariosprint.domain.game.Tile
import fr.julie.mariosprint.domain.game.LevelTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Rendu 2D illustré : assets HD, dégradés, profondeur et caméra adaptés au portrait. */
class GameSceneRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val destination = RectF()
    private val path = Path()
    private var skyHeight = 0f
    private var sky: Shader? = null
    private var theme = LevelTheme.MEADOW

    fun draw(canvas: Canvas, width: Float, height: Float, game: GameEngine, preview: Boolean = false) {
        if (width <= 0 || height <= 0) return
        if (height != skyHeight || theme != game.levelId.theme) {
            theme = game.levelId.theme
            skyHeight = height
            sky = LinearGradient(0f, 0f, 0f, height, when (theme) {
                    LevelTheme.MEADOW -> intArrayOf(0xff68bde5.toInt(), 0xffbce8ef.toInt(), 0xffe5f2d6.toInt())
                    LevelTheme.CANYON -> intArrayOf(0xffe89f83.toInt(), 0xffffd7a1.toInt(), 0xffffe6bd.toInt())
                    LevelTheme.NIGHT -> intArrayOf(0xff172142.toInt(), 0xff3e4a7c.toInt(), 0xff8c8ca4.toInt())
                },
                floatArrayOf(0f, .65f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.save(); canvas.clipRect(0f, 0f, width, height)
        paint.shader = sky; canvas.drawRect(0f, 0f, width, height, paint); paint.shader = null
        val scale = if (preview) height / Level.HEIGHT else min(height / Level.HEIGHT, width / 224f)
        val visibleWidth = width / scale
        val verticalOffset = height / scale - Level.HEIGHT
        val cameraX = if (preview) 108f else (game.player.x - visibleWidth * .32f)
            .coerceIn(0f, (Level.COLS * Level.SIZE - visibleWidth).coerceAtLeast(0f))
        canvas.scale(scale, scale); canvas.translate(0f, verticalOffset)
        drawScenery(canvas, cameraX, visibleWidth, verticalOffset, game.elapsedSeconds.toFloat())
        canvas.translate(-cameraX, 0f)
        val firstColumn = (floor(cameraX / 16).toInt() - 1).coerceAtLeast(0)
        val lastColumn = ceil((cameraX + visibleWidth) / 16).toInt().coerceAtMost(Level.COLS - 1)
        for (row in 0 until Level.ROWS) for (column in firstColumn..lastColumn) {
            val tile = game.level.tile(column, row)
            when (tile) {
                Tile.AIR, Tile.PIPE -> Unit
                Tile.GROUND -> drawBitmap(canvas, if (row == 13) GameArt.terrain.getValue(theme).first else GameArt.terrain.getValue(theme).second, column * 16f, row * 16f, 16f, 16f)
                Tile.PIPE_TOP -> if (game.level.tile(column - 1, row) != Tile.PIPE_TOP) {
                    var pipeHeight = 1
                    while (game.level.tile(column, row + pipeHeight) == Tile.PIPE) pipeHeight++
                    drawBitmap(canvas, GameArt.pipes.getValue(pipeHeight), column * 16f, row * 16f, 32f, pipeHeight * 16f)
                }
                else -> drawBitmap(canvas, GameArt.tiles.getValue(tile), column * 16f, row * 16f, 16f, 16f)
            }
        }
        for ((index, coin) in game.level.coins.withIndex()) if (!coin.collected && coin.x > cameraX - 16 && coin.x < cameraX + visibleWidth) {
            val phase = game.elapsedSeconds.toFloat() * 4 + index * .5f
            val coinWidth = if (preview) 9f else 5f + abs(cos(phase)) * 4
            drawBitmap(canvas, GameArt.coin, coin.x + (9 - coinWidth) / 2, coin.y - 2 + sin(phase) * .8f, coinWidth, 13f)
        }
        for (bonus in game.level.bonuses) if (!bonus.collected && bonus.x > cameraX - 24 && bonus.x < cameraX + visibleWidth) {
            val bob = sin(game.elapsedSeconds.toFloat() * 3f + bonus.x) * 1.5f
            paint.color = 0x30fff4ba; canvas.drawCircle(bonus.x + 7, bonus.y + 7 + bob, 13f, paint)
            drawBitmap(canvas, GameArt.bonuses.getValue(bonus.type), bonus.x - 3, bonus.y - 3 + bob, 20f, 20f)
        }
        drawFinish(canvas, cameraX, visibleWidth)
        for (enemy in game.enemies) if ((enemy.alive || enemy.squashed > 0) && enemy.x > cameraX - 20 && enemy.x < cameraX + visibleWidth) {
            val squashHeight = if (enemy.alive) 18f else 5f
            shadow(canvas, enemy.x + 7, enemy.y + 14, 7f)
            drawBitmap(canvas, GameArt.enemy, enemy.x - 2, enemy.y + 14 - squashHeight, 18f, squashHeight)
        }
        for (mushroom in game.mushrooms) if (mushroom.alive) {
            drawBitmap(canvas, GameArt.mushroom, mushroom.x - 2, mushroom.y - 4, 18f, 18f)
        }
        val player = game.player
        if (player.shield || player.starSeconds > 0) {
            paint.color = if (player.shield) 0x604de8d4 else 0x80ffe6a1.toInt()
            canvas.drawCircle(player.x + 6, player.y + player.height / 2, player.height * .85f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = .8f
            paint.color = if (player.shield) 0xffb3fff0.toInt() else 0xffffe7a1.toInt()
            canvas.drawCircle(player.x + 6, player.y + player.height / 2, player.height * .85f, paint)
            paint.style = Paint.Style.FILL
        }
        if (player.magnetSeconds > 0) drawBitmap(canvas, GameArt.bonuses.getValue(fr.julie.mariosprint.domain.game.BonusType.MAGNET), player.x, player.y - 19, 12f, 12f)
        if (player.invincible <= 0 || (game.elapsedSeconds * 16).toInt() % 2 == 0) {
            val frame = when {
                !player.grounded && game.phase != Phase.READY -> 3
                abs(player.vx) < 10 -> 0
                else -> 1 + (game.elapsedSeconds * 12).toInt() % 2
            }
            if (player.grounded || game.phase == Phase.READY) shadow(canvas, player.x + 6, player.y + player.height, 8f)
            canvas.save()
            if (player.facing < 0) { canvas.translate(player.x * 2 + player.width, 0f); canvas.scale(-1f, 1f) }
            drawBitmap(canvas, GameArt.heroFrames[frame], player.x - 3, player.y - 6, 18f, player.height + 6f)
            canvas.restore()
        }
        paint.color = 0xffffefaa.toInt()
        for (spark in game.sparks) canvas.drawCircle(spark.x, spark.y, 1.5f, paint)
        canvas.restore()
    }

    private fun drawScenery(canvas: Canvas, cameraX: Float, visibleWidth: Float, extraSky: Float, time: Float) {
        // Collines à plusieurs vitesses pour une profondeur douce, sans textures téléchargées.
        for (layer in 0..2) {
            val spacing = 155f + layer * 40
            val speed = .12f + layer * .12f
            val offset = cameraX * speed % spacing
            paint.color = when (theme) {
                LevelTheme.MEADOW -> when (layer) { 0 -> 0xffb0dcd1.toInt(); 1 -> 0xff8dcab5.toInt(); else -> 0xff68b998.toInt() }
                LevelTheme.CANYON -> when (layer) { 0 -> 0xffe6b096.toInt(); 1 -> 0xffce8b78.toInt(); else -> 0xffb27366.toInt() }
                LevelTheme.NIGHT -> when (layer) { 0 -> 0xff606a94.toInt(); 1 -> 0xff4c577f.toInt(); else -> 0xff39496b.toInt() }
            }
            for (index in -1..ceil(visibleWidth / spacing).toInt() + 1) {
                val left = index * spacing - offset
                val peak = 106f + layer * 26 + (index and 1) * 15
                path.reset(); path.moveTo(left - 55, 208f)
                if (theme == LevelTheme.CANYON) {
                    path.lineTo(left + 12, peak); path.lineTo(left + 72, peak); path.lineTo(left + 105, 208f)
                } else {
                    path.cubicTo(left - 10, 208f, left + 1, peak, left + 55, peak)
                    path.cubicTo(left + 108, peak, left + 119, 208f, left + 185, 208f)
                }
                path.close(); canvas.drawPath(path, paint)
            }
        }
        if (theme == LevelTheme.NIGHT) {
            paint.color = 0xffffeac1.toInt(); canvas.drawCircle(visibleWidth - 36, 29f - extraSky * .4f, 13f, paint)
            repeat(30) { i ->
                paint.color = if (i % 3 == 0) 0xffffeac1.toInt() else 0x90e4ecff.toInt()
                canvas.drawCircle(((i * 47f + 17 - cameraX * .025f) % (visibleWidth + 16) + visibleWidth + 16) % (visibleWidth + 16),
                    9f - extraSky * .3f + (i * 29 % 85), if (i % 3 == 0) 1f else .55f, paint)
            }
        }
        if (theme != LevelTheme.NIGHT) for (index in -1..ceil(visibleWidth / 160).toInt() + 1) {
            val x = index * 160f - (cameraX * .1f % 160) + 18
            val y = 31f - extraSky * .45f + (index and 1) * 21
            paint.color = 0xb8ffffff.toInt()
            canvas.drawOval(x, y + 8, x + 61, y + 25, paint)
            canvas.drawOval(x + 10, y - 1, x + 37, y + 24, paint)
            canvas.drawOval(x + 29, y + 3, x + 51, y + 23, paint)
        }
        val firstPlant = floor(cameraX / 88).toInt()
        for (index in firstPlant..firstPlant + ceil(visibleWidth / 88).toInt() + 1) {
            val x = index * 88f - cameraX + 34
            paint.color = when (theme) { LevelTheme.MEADOW -> 0xff4c9d79.toInt(); LevelTheme.CANYON -> 0xff9c8665.toInt(); LevelTheme.NIGHT -> 0xff697aa0.toInt() }
            canvas.drawOval(x - 15, 197f, x + 8, 214f, paint)
            canvas.drawOval(x - 3, 190f, x + 18, 213f, paint)
            paint.color = if (index % 2 == 0) 0xffffe58f.toInt() else 0xffffb9a4.toInt()
            val sway = sin(time * 2 + index).toFloat()
            canvas.drawCircle(x + 4 + sway, 192f, 2.3f, paint)
            paint.color = 0xfffdf7d5.toInt(); canvas.drawCircle(x + 4 + sway, 192f, 1f, paint)
        }
    }

    private fun drawFinish(canvas: Canvas, cameraX: Float, visibleWidth: Float) {
        val x = Level.FLAG_X
        if (x < cameraX - 120 || x > cameraX + visibleWidth + 20) return
        paint.color = 0xff437b81.toInt()
        canvas.drawRoundRect(x + 3, 53f, x + 5, 208f, 1f, 1f, paint)
        paint.color = 0xffffdf82.toInt(); canvas.drawCircle(x + 4, 52f, 3f, paint)
        path.reset(); path.moveTo(x + 5, 58f); path.quadTo(x + 18, 51f, x + 33, 59f)
        path.lineTo(x + 30, 78f); path.quadTo(x + 17, 71f, x + 5, 78f); path.close()
        paint.color = 0xffef6b64.toInt(); canvas.drawPath(path, paint)
        paint.color = Color.WHITE; canvas.drawCircle(x + 18, 65f, 4f, paint)
        drawBitmap(canvas, GameArt.finishHouse, x + 55, 136f, 70f, 72f)
    }
    private fun shadow(canvas: Canvas, center: Float, bottom: Float, radius: Float) {
        paint.color = 0x3045654d; canvas.drawOval(center - radius, bottom - 2, center + radius, bottom + 1, paint)
    }
    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, width: Float, height: Float) {
        paint.color = Color.WHITE; destination.set(x, y, x + width, y + height)
        canvas.drawBitmap(bitmap, null, destination, paint)
    }
}
