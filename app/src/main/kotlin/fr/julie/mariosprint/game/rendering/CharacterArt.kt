package fr.julie.mariosprint.game.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.julie.mariosprint.domain.model.CharacterSkin

/** Silhouettes distinctes, calculées au préchargement ; aucune allocation graphique par image. */
internal object CharacterArt {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun frames(skin: CharacterSkin): List<Bitmap> = List(4) { frame ->
        Bitmap.createBitmap(192, 240, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap).apply { scale(3f, 3f) }
            val step = when (frame) { 1 -> -4f; 2 -> 4f; else -> 0f }
            val lift = if (frame == 3) -6f else 0f
            when (skin) {
                CharacterSkin.TOAD -> toad(canvas, step, lift)
                CharacterSkin.YOSHI -> yoshi(canvas, step, lift)
                CharacterSkin.PEACH -> peach(canvas, step, lift)
                CharacterSkin.LUIGI -> error("Luigi est dessiné par GameArt")
            }
        }
    }

    private fun toad(canvas: Canvas, step: Float, lift: Float) {
        oval(canvas, 12f + step, 65f + lift, 32f + step, 76f + lift, 0xff86563b.toInt())
        oval(canvas, 34f - step, 65f + lift, 55f - step, 76f + lift, 0xff9b6541.toInt())
        round(canvas, 20f, 48f, 46f, 69f + lift, 7f, 0xfff7efdf.toInt())
        oval(canvas, 13f, 37f, 51f, 61f, 0xffffceaa.toInt())
        round(canvas, 14f, 36f, 25f, 58f, 4f, 0xff377bd3.toInt())
        round(canvas, 41f, 36f, 51f, 58f, 4f, 0xff377bd3.toInt())
        round(canvas, 17f, 37f, 21f, 55f, 1f, 0xffffd46a.toInt())
        round(canvas, 44f, 37f, 48f, 55f, 1f, 0xffffd46a.toInt())
        oval(canvas, 7f, 43f - step, 21f, 55f - step, 0xffffceaa.toInt())
        oval(canvas, 46f, 42f + step, 59f, 54f + step, 0xffffceaa.toInt())
        gradient(canvas, 19f, 18f, 48f, 43f, 12f, 0xffffdcba.toInt(), 0xffefb28e.toInt())
        oval(canvas, 30f, 26f, 33f, 35f, 0xff30374b.toInt())
        oval(canvas, 40f, 26f, 43f, 35f, 0xff30374b.toInt())
        oval(canvas, 34f, 36f, 39f, 39f, 0xffd47c73.toInt())
        gradient(canvas, 5f, 0f, 60f, 28f, 14f, Color.WHITE, 0xffe5e8f0.toInt())
        oval(canvas, 23f, 1f, 42f, 20f, 0xffec535c.toInt())
        oval(canvas, 6f, 10f, 18f, 25f, 0xffed5c61.toInt())
        oval(canvas, 48f, 9f, 59f, 24f, 0xffed5c61.toInt())
    }

    private fun yoshi(canvas: Canvas, step: Float, lift: Float) {
        path.reset(); path.moveTo(25f, 48f); path.lineTo(4f, 54f); path.lineTo(23f, 61f); path.close()
        paint.color = 0xff469f5b.toInt(); canvas.drawPath(path, paint)
        oval(canvas, 18f + step, 64f + lift, 36f + step, 77f + lift, 0xffed9950.toInt())
        oval(canvas, 35f - step, 64f + lift, 57f - step, 77f + lift, 0xffe7863e.toInt())
        gradient(canvas, 17f, 33f, 47f, 69f, 13f, 0xff8adb75.toInt(), 0xff379b58.toInt())
        oval(canvas, 29f, 42f, 49f, 65f, 0xfff5eedf.toInt())
        oval(canvas, 14f, 39f, 25f, 55f, 0xffe75c58.toInt())
        oval(canvas, 40f, 43f + step, 57f, 53f + step, 0xff5bbb6a.toInt())
        oval(canvas, 16f, 15f, 27f, 28f, 0xffe76559.toInt())
        oval(canvas, 14f, 27f, 24f, 39f, 0xffe76559.toInt())
        gradient(canvas, 21f, 6f, 48f, 38f, 12f, 0xff8fdf7a.toInt(), 0xff45a760.toInt())
        oval(canvas, 27f, 1f, 39f, 25f, Color.WHITE)
        oval(canvas, 38f, 2f, 50f, 26f, Color.WHITE)
        oval(canvas, 33f, 9f, 38f, 22f, 0xff26384a.toInt())
        oval(canvas, 44f, 10f, 49f, 23f, 0xff26384a.toInt())
        oval(canvas, 34f, 10f, 36f, 14f, Color.WHITE)
        gradient(canvas, 33f, 20f, 63f, 42f, 11f, 0xff8fdd77.toInt(), 0xff4eaf62.toInt())
        oval(canvas, 39f, 33f, 59f, 43f, 0xfff5efdc.toInt())
        oval(canvas, 51f, 24f, 54f, 27f, 0xff3c8f54.toInt())
        oval(canvas, 57f, 25f, 60f, 28f, 0xff3c8f54.toInt())
    }

    private fun peach(canvas: Canvas, step: Float, lift: Float) {
        gradient(canvas, 13f, 3f, 51f, 59f, 16f, 0xffffe58c.toInt(), 0xffeeb652.toInt())
        oval(canvas, 19f + step, 67f + lift, 34f + step, 76f + lift, 0xffc65b85.toInt())
        oval(canvas, 36f - step, 67f + lift, 51f - step, 76f + lift, 0xffc65b85.toInt())
        path.reset(); path.moveTo(25f, 35f); path.lineTo(42f, 35f)
        path.cubicTo(45f, 49f, 54f, 56f, 56f, 69f + lift)
        path.quadTo(33f, 79f + lift, 10f, 69f + lift); path.quadTo(17f, 49f, 25f, 35f); path.close()
        paint.shader = LinearGradient(15f, 34f, 52f, 75f, 0xffffa8cb.toInt(), 0xffdf6198.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint); paint.shader = null
        oval(canvas, 13f, 36f, 27f, 50f, 0xffffb4d1.toInt())
        oval(canvas, 41f, 36f, 54f, 50f, 0xffffb4d1.toInt())
        round(canvas, 9f, 45f - step, 22f, 59f - step, 5f, 0xfff7f4ee.toInt())
        round(canvas, 47f, 44f + step, 59f, 58f + step, 5f, 0xfff7f4ee.toInt())
        gradient(canvas, 21f, 9f, 49f, 37f, 12f, 0xffffdec3.toInt(), 0xffefb498.toInt())
        oval(canvas, 31f, 18f, 35f, 28f, 0xff448ab2.toInt())
        oval(canvas, 41f, 18f, 45f, 28f, 0xff448ab2.toInt())
        oval(canvas, 32f, 19f, 34f, 24f, Color.WHITE)
        oval(canvas, 36f, 30f, 41f, 33f, 0xffd8728d.toInt())
        oval(canvas, 18f, 8f, 39f, 18f, 0xffffdf7e.toInt())
        oval(canvas, 17f, 25f, 22f, 33f, 0xff68bed1.toInt())
        oval(canvas, 47f, 25f, 52f, 33f, 0xff68bed1.toInt())
        path.reset(); path.moveTo(24f, 12f); path.lineTo(22f, 0f); path.lineTo(29f, 5f)
        path.lineTo(34f, -1f); path.lineTo(39f, 5f); path.lineTo(46f, 0f); path.lineTo(44f, 12f); path.close()
        paint.color = 0xffffce64.toInt(); canvas.drawPath(path, paint)
        oval(canvas, 31f, 5f, 37f, 12f, 0xff62b9d2.toInt())
        oval(canvas, 30f, 38f, 39f, 47f, 0xff62b9d2.toInt())
    }

    private fun round(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
        paint.shader = null; paint.color = color
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
    }
    private fun oval(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        paint.shader = null; paint.color = color; canvas.drawOval(left, top, right, bottom, paint)
    }
    private fun gradient(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float, first: Int, last: Int) {
        paint.shader = LinearGradient(left, top, right, bottom, first, last, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint); paint.shader = null
    }
}
