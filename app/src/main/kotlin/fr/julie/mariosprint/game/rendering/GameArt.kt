package fr.julie.mariosprint.game.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import fr.julie.mariosprint.domain.game.Tile
import fr.julie.mariosprint.domain.game.BonusType
import fr.julie.mariosprint.domain.game.LevelTheme
import fr.julie.mariosprint.domain.model.CharacterSkin
import kotlin.math.cos
import kotlin.math.sin

/** Assets vectoriels rendus une fois en bitmaps haute définition, partagés entre les vues. */
internal object GameArt {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    val tiles = Tile.entries.filter { it != Tile.AIR }.associateWith(::createTile)
    val soil = createSoil(false)
    val grass = createSoil(true)
    val characters = CharacterSkin.entries.associateWith { skin ->
        if (skin == CharacterSkin.LUIGI) List(4, ::createHero) else CharacterArt.frames(skin)
    }
    val heroFrames = characters.getValue(CharacterSkin.LUIGI)
    val enemy = createEnemy()
    val mushroom = createMushroom()
    val coin = createCoin()
    val pipes = mapOf(2 to createPipe(2), 3 to createPipe(3))
    val finishHouse = createFinishHouse()
    val bonuses = BonusType.entries.associateWith(::createBonus)
    val terrain = LevelTheme.entries.associateWith { theme ->
        if (theme == LevelTheme.MEADOW) grass to soil else createTerrain(theme, true) to createTerrain(theme, false)
    }

    private fun createTerrain(theme: LevelTheme, top: Boolean) = bitmap(16, 16) { canvas ->
        val night = theme == LevelTheme.NIGHT
        gradient(canvas, 0f, 0f, 16f, 16f, 0f, if (night) 0xff55658f.toInt() else 0xffd89c69.toInt(),
            if (night) 0xff303d68.toInt() else 0xff975a49.toInt())
        if (top) {
            round(canvas, 0f, 0f, 16f, 4f, 1f, if (night) 0xffa6c0ef.toInt() else 0xffffd391.toInt())
            stroke(canvas, 1f, 1f, 15f, 1f, .5f, 0xfff4ecdd.toInt())
        }
        oval(canvas, 3f, 9f, 6f, 11f, if (night) 0xff7f8eb7.toInt() else 0xffe9b88a.toInt())
        stroke(canvas, 11f, 6f, 14f, 7f, .8f, 0x50804646)
    }
    private fun createBonus(type: BonusType) = bitmap(20, 20) { canvas ->
        oval(canvas, 1f, 1f, 19f, 19f, 0x204d5779)
        when (type) {
            BonusType.STAR -> {
                path.reset()
                repeat(10) { i ->
                    val angle = -Math.PI / 2 + i * Math.PI / 5
                    val radius = if (i % 2 == 0) 9f else 4.3f
                    val x = 10 + cos(angle).toFloat() * radius; val y = 10 + sin(angle).toFloat() * radius
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); paint.color = 0xffffd76e.toInt(); canvas.drawPath(path, paint)
                oval(canvas, 7f, 8f, 8.5f, 12f, 0xff684e48.toInt()); oval(canvas, 11.5f, 8f, 13f, 12f, 0xff684e48.toInt())
            }
            BonusType.SHIELD -> {
                path.reset(); path.moveTo(10f, 1f); path.lineTo(18f, 5f); path.lineTo(17f, 12f)
                path.quadTo(15f, 17f, 10f, 19f); path.quadTo(5f, 17f, 3f, 12f)
                path.lineTo(2f, 5f); path.close(); paint.color = 0xffa4efda.toInt(); canvas.drawPath(path, paint)
                stroke(canvas, 10f, 5f, 10f, 14f, 2f, 0xff3b9c99.toInt())
            }
            BonusType.MAGNET -> {
                path.reset(); path.moveTo(4f, 3f); path.lineTo(4f, 11f)
                path.cubicTo(4f, 20f, 16f, 20f, 16f, 11f); path.lineTo(16f, 3f)
                paint.color = 0xffff879e.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f
                canvas.drawPath(path, paint); paint.style = Paint.Style.FILL
                round(canvas, 1.5f, 1f, 6.5f, 6f, 1f, 0xffe9f1ff.toInt())
                round(canvas, 13.5f, 1f, 18.5f, 6f, 1f, 0xffe9f1ff.toInt())
            }
        }
    }

    private fun bitmap(width: Int, height: Int, scale: Int = 8, draw: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply { scale(scale.toFloat(), scale.toFloat()); draw(this) }
        return bitmap
    }

    private fun createHero(frame: Int) = bitmap(64, 80, 3) { canvas ->
        val leftFoot = if (frame == 1) -4f else if (frame == 2) 4f else 0f
        val rightFoot = -leftFoot
        val lift = if (frame == 3) -7f else 0f
        // Bottes, jambes et silhouette : trois poses de course et une pose aérienne.
        round(canvas, 19f + leftFoot, 51f + lift, 29f + leftFoot, 70f + lift, 4f, 0xff2053a4.toInt())
        round(canvas, 34f + rightFoot, 51f + lift, 44f + rightFoot, 70f + lift, 4f, 0xff2c69c2.toInt())
        gradient(canvas, 13f + leftFoot, 66f + lift, 31f + leftFoot, 76f + lift, 5f, 0xff9c643a.toInt(), 0xff533b33.toInt())
        gradient(canvas, 32f + rightFoot, 66f + lift, 51f + rightFoot, 76f + lift, 5f, 0xffaa7041.toInt(), 0xff593d30.toInt())
        round(canvas, 16f + leftFoot, 67f + lift, 25f + leftFoot, 69f + lift, 1f, 0x70ffd49c)
        round(canvas, 36f + rightFoot, 67f + lift, 46f + rightFoot, 69f + lift, 1f, 0x70ffd49c)
        // Bras et gants blancs contrastés pour rester lisibles sur petit écran.
        gradient(canvas, 11f, 36f, 25f, 56f, 6f, 0xff69cf76.toInt(), 0xff218b54.toInt())
        gradient(canvas, 40f, 35f, 52f, 53f, 6f, 0xff80da78.toInt(), 0xff239352.toInt())
        oval(canvas, 7f, 48f - leftFoot, 24f, 62f - leftFoot, 0xffe2e7f2.toInt())
        oval(canvas, 9f, 48f - leftFoot, 23f, 58f - leftFoot, Color.WHITE)
        oval(canvas, 43f, 46f + leftFoot, 58f, 60f + leftFoot, 0xffe2e7f2.toInt())
        oval(canvas, 44f, 46f + leftFoot, 57f, 56f + leftFoot, Color.WHITE)
        gradient(canvas, 21f, 33f, 46f, 61f, 9f, 0xff66c968.toInt(), 0xff289551.toInt())
        round(canvas, 22f, 43f, 46f, 64f, 6f, 0xff3379d4.toInt())
        round(canvas, 23f, 35f, 28f, 51f, 2f, 0xff3e88e4.toInt())
        round(canvas, 39f, 35f, 44f, 51f, 2f, 0xff3e88e4.toInt())
        oval(canvas, 25f, 45f, 29f, 49f, 0xffffd46a.toInt())
        oval(canvas, 38f, 45f, 42f, 49f, 0xffffd46a.toInt())
        // Cheveux, visage, yeux, nez et moustache.
        oval(canvas, 15f, 10f, 47f, 38f, 0xff603d32.toInt())
        gradient(canvas, 21f, 10f, 51f, 38f, 13f, 0xffffd6ac.toInt(), 0xfff0a17e.toInt())
        oval(canvas, 16f, 22f, 27f, 33f, 0xffefaa88.toInt())
        oval(canvas, 18f, 23f, 24f, 29f, 0xffffc6a0.toInt())
        oval(canvas, 36f, 13f, 45f, 27f, Color.WHITE)
        oval(canvas, 45f, 14f, 52f, 27f, Color.WHITE)
        oval(canvas, 40f, 16f, 45f, 26f, 0xff3d9fc8.toInt())
        oval(canvas, 48f, 17f, 52f, 26f, 0xff3d9fc8.toInt())
        oval(canvas, 42f, 18f, 45f, 25f, 0xff263346.toInt())
        oval(canvas, 49f, 19f, 52f, 25f, 0xff263346.toInt())
        oval(canvas, 41f, 17f, 43f, 20f, Color.WHITE)
        round(canvas, 35f, 12f, 45f, 15f, 2f, 0xff624034.toInt())
        round(canvas, 46f, 13f, 52f, 16f, 2f, 0xff624034.toInt())
        oval(canvas, 32f, 27f, 55f, 35f, 0xff59372e.toInt())
        oval(canvas, 36f, 29f, 45f, 37f, 0xff59372e.toInt())
        oval(canvas, 43f, 29f, 51f, 36f, 0xff59372e.toInt())
        oval(canvas, 45f, 23f, 59f, 32f, 0xffefaa87.toInt())
        oval(canvas, 45f, 22f, 58f, 29f, 0xffffd2aa.toInt())
        // Luigi : casquette verte et badge L.
        path.reset(); path.moveTo(17f, 16f); path.cubicTo(13f, -1f, 39f, -4f, 48f, 6f)
        path.quadTo(52f, 10f, 51f, 17f); path.close()
        paint.shader = LinearGradient(20f, 0f, 44f, 20f, 0xff80db78.toInt(), 0xff238b51.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint); paint.shader = null
        oval(canvas, 34f, 12f, 61f, 19f, 0xff1e7849.toInt())
        oval(canvas, 34f, 11f, 61f, 16f, 0xff5dc865.toInt())
        oval(canvas, 26f, 2f, 39f, 14f, 0xfffff4df.toInt())
        paint.color = 0xff258c50.toInt(); paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD); paint.textSize = 9f
        canvas.drawText("L", 32.5f, 11.5f, paint)
    }

    private fun createEnemy() = bitmap(18, 18) { canvas ->
        oval(canvas, 0f, 12f, 9f, 17.5f, 0xff4d3935.toInt())
        oval(canvas, 10f, 12f, 18f, 17.5f, 0xff4d3935.toInt())
        gradient(canvas, 4f, 9f, 14f, 16f, 4f, 0xffffd7ac.toInt(), 0xffd39269.toInt())
        path.reset(); path.moveTo(1f, 11f); path.cubicTo(-1f, 6f, 4f, 0f, 9f, .5f)
        path.cubicTo(14f, 0f, 19f, 6f, 17f, 11f); path.quadTo(9f, 14f, 1f, 11f); path.close()
        paint.shader = LinearGradient(3f, 2f, 14f, 14f, 0xffbf8251.toInt(), 0xff80503b.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint); paint.shader = null
        oval(canvas, 4f, 5f, 8f, 11f, Color.WHITE); oval(canvas, 10f, 5f, 14f, 11f, Color.WHITE)
        oval(canvas, 6f, 6f, 8f, 10f, 0xff2c3444.toInt()); oval(canvas, 10f, 6f, 12f, 10f, 0xff2c3444.toInt())
        stroke(canvas, 3.8f, 4.8f, 8.3f, 6.2f, 1.2f, 0xff50372e.toInt())
        stroke(canvas, 9.7f, 6.2f, 14.2f, 4.8f, 1.2f, 0xff50372e.toInt())
    }

    private fun createMushroom() = bitmap(18, 18) { canvas ->
        gradient(canvas, 5f, 8f, 13f, 17f, 3f, 0xfffff1d1.toInt(), 0xffe2b98a.toInt())
        path.reset(); path.moveTo(1f, 11f); path.cubicTo(-1f, -3f, 19f, -3f, 17f, 11f)
        path.quadTo(9f, 14f, 1f, 11f); path.close()
        paint.shader = LinearGradient(5f, 0f, 12f, 13f, 0xffff7464.toInt(), 0xffda384b.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint); paint.shader = null
        oval(canvas, 6f, 1.5f, 12f, 7.5f, 0xfffff6e7.toInt())
        oval(canvas, 1f, 7f, 5f, 11f, 0xfffff6e7.toInt()); oval(canvas, 13f, 7f, 17f, 11f, 0xfffff6e7.toInt())
        round(canvas, 7f, 12.5f, 8f, 15.5f, .5f, 0xff313447.toInt())
        round(canvas, 10f, 12.5f, 11f, 15.5f, .5f, 0xff313447.toInt())
    }

    private fun createCoin() = bitmap(12, 16) { canvas ->
        oval(canvas, 1f, 1f, 11f, 15f, 0xffc9892e.toInt())
        oval(canvas, .5f, .3f, 10f, 14f, 0xffffd85f.toInt())
        oval(canvas, 2f, 1.8f, 8.5f, 12.5f, 0xffffbb44.toInt())
        round(canvas, 3.7f, 3f, 5.8f, 10.5f, 1f, 0xffffef99.toInt())
        oval(canvas, 2f, 2f, 4f, 4f, Color.WHITE)
    }

    private fun createSoil(withGrass: Boolean) = bitmap(16, 16) { canvas ->
        gradient(canvas, 0f, 0f, 16f, 16f, 0f, 0xffbd8256.toInt(), 0xff976348.toInt())
        oval(canvas, 2f, 8f, 6f, 11f, 0xffd79c68.toInt())
        oval(canvas, 10f, 5f, 14f, 8f, 0xffd29a67.toInt())
        oval(canvas, 8f, 12f, 11f, 14f, 0xff7e5746.toInt())
        if (withGrass) {
            round(canvas, -.3f, 0f, 16.3f, 4f, 1f, 0xff56ba68.toInt())
            round(canvas, 2f, 1f, 6f, 6f, 2f, 0xff56ba68.toInt())
            round(canvas, 11f, 1f, 14f, 5f, 1.5f, 0xff56ba68.toInt())
            round(canvas, -.2f, 0f, 16.2f, 2f, .8f, 0xffa3e66b.toInt())
            stroke(canvas, 4f, .8f, 8f, .8f, .5f, 0xffd8f49b.toInt())
        }
    }

    private fun createTile(tile: Tile) = bitmap(16, 16) { canvas ->
        when (tile) {
            Tile.QUESTION_COIN, Tile.QUESTION_MUSHROOM, Tile.USED -> {
                val used = tile == Tile.USED
                round(canvas, .5f, 1f, 16f, 16f, 2.5f, 0xff906441.toInt())
                gradient(canvas, 0f, 0f, 15f, 14.5f, 2f,
                    if (used) 0xffbb9270.toInt() else 0xffffe383.toInt(),
                    if (used) 0xff967358.toInt() else 0xfff8b542.toInt())
                round(canvas, 1.5f, 1.2f, 13.5f, 2.2f, .5f, 0x90fff9cb.toInt())
                if (!used) {
                    paint.color = 0xff9b612f.toInt(); paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    paint.textSize = 12f; paint.textAlign = Paint.Align.CENTER; canvas.drawText("?", 7.7f, 11.8f, paint)
                }
                oval(canvas, 2f, 11f, 3f, 12f, 0xffa77743.toInt()); oval(canvas, 12f, 11f, 13f, 12f, 0xffa77743.toInt())
            }
            Tile.BRICK -> {
                gradient(canvas, 0f, 0f, 16f, 16f, 1.3f, 0xffe5a071.toInt(), 0xffb66e55.toInt())
                stroke(canvas, 0f, 8f, 16f, 8f, 1f, 0xff955d4d.toInt())
                stroke(canvas, 8f, 0f, 8f, 8f, .8f, 0xff955d4d.toInt())
                stroke(canvas, 4f, 8f, 4f, 16f, .8f, 0xff955d4d.toInt())
                stroke(canvas, 12f, 8f, 12f, 16f, .8f, 0xff955d4d.toInt())
                stroke(canvas, 1f, 1f, 14f, 1f, .6f, 0xffffc89d.toInt())
            }
            Tile.STONE -> {
                round(canvas, 0f, 0f, 16f, 16f, 2f, 0xff73958e.toInt())
                gradient(canvas, .5f, .5f, 15f, 14f, 2f, 0xffd2dec2.toInt(), 0xff9eb7a4.toInt())
                round(canvas, 2f, 2f, 13.5f, 3.5f, .6f, 0xffedf0d7.toInt())
                oval(canvas, 10f, 9f, 13f, 11f, 0xff8da99b.toInt())
            }
            else -> Unit // Sol et tuyaux sont assemblés séparément pour éviter les raccords visibles.
        }
    }

    private fun createPipe(height: Int) = bitmap(32, height * 16) { canvas ->
        val bottom = height * 16f
        paint.shader = LinearGradient(3f, 0f, 29f, 0f,
            intArrayOf(0xff328b76.toInt(), 0xff91df87.toInt(), 0xff42b889.toInt(), 0xff29876f.toInt()),
            floatArrayOf(0f, .22f, .55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(3f, 5f, 29f, bottom, 1.5f, 1.5f, paint); paint.shader = null
        round(canvas, 6f, 9f, 8f, bottom - 2, 1f, 0x80d6ffc1.toInt())
        gradient(canvas, 0f, 2f, 32f, 10f, 2f, 0xffa0ec98.toInt(), 0xff2e9b7b.toInt())
        oval(canvas, 0f, 0f, 32f, 7f, 0xffa4eda5.toInt())
        oval(canvas, 3f, 1.5f, 29f, 5.5f, 0xff297e6c.toInt())
        oval(canvas, 5f, 2f, 27f, 4.5f, 0xff1c655d.toInt())
        stroke(canvas, 4f, 7.5f, 28f, 7.5f, .5f, 0x90d1f9b6.toInt())
    }

    private fun createFinishHouse() = bitmap(70, 72, 4) { canvas ->
        gradient(canvas, 7f, 25f, 63f, 71f, 6f, 0xffffebc7.toInt(), 0xffe6c29b.toInt())
        path.reset(); path.moveTo(0f, 30f); path.quadTo(2f, 0f, 35f, 0f)
        path.quadTo(68f, 0f, 70f, 30f); path.quadTo(35f, 39f, 0f, 30f); path.close()
        paint.shader = LinearGradient(15f, 0f, 40f, 35f, 0xffff8d72.toInt(), 0xffdf4d58.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint); paint.shader = null
        oval(canvas, 24f, 4f, 44f, 22f, 0xffffe4c2.toInt())
        oval(canvas, 6f, 18f, 17f, 28f, 0xffffe4c2.toInt()); oval(canvas, 54f, 15f, 66f, 26f, 0xffffe4c2.toInt())
        gradient(canvas, 27f, 43f, 44f, 71f, 8f, 0xff548e9d.toInt(), 0xff3d697e.toInt())
        oval(canvas, 39f, 57f, 41f, 59f, 0xffffd87b.toInt())
        round(canvas, 12f, 43f, 22f, 56f, 3f, 0xff75b9c5.toInt())
        round(canvas, 49f, 43f, 59f, 56f, 3f, 0xff75b9c5.toInt())
        stroke(canvas, 17f, 43f, 17f, 56f, 1f, 0xfff6e4c2.toInt())
        stroke(canvas, 54f, 43f, 54f, 56f, 1f, 0xfff6e4c2.toInt())
    }

    private fun round(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = color
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
    }
    private fun oval(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = color
        canvas.drawOval(left, top, right, bottom, paint)
    }
    private fun gradient(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float, topColor: Int, bottomColor: Int) {
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        paint.shader = LinearGradient(left, top, right, bottom, topColor, bottomColor, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint); paint.shader = null
    }
    private fun stroke(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        paint.shader = null; paint.color = color; paint.strokeWidth = width; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
}
