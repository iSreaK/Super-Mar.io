package fr.julie.mariosprint.game.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import fr.julie.mariosprint.game.input.ControlLayout
import fr.julie.mariosprint.game.input.TouchControls
import kotlin.math.abs

/** Contrôles fixes dans la zone basse, dimensionnés pour les deux pouces. */
class ControlsRenderer(private val jumpLabel: String, private val runLabel: String,
    private val moveLabel: String, private val jumpHint: String) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Path()
    private val labelTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun draw(canvas: Canvas, layout: ControlLayout, touch: TouchControls) {
        val d = layout.density
        paint.color = 0xff111a2a.toInt()
        canvas.drawRect(0f, layout.panelTop, layout.width, layout.height, paint)
        paint.color = 0xff26364c.toInt()
        canvas.drawRect(18 * d, layout.panelTop, layout.width - 18 * d, layout.panelTop + d, paint)

        val x = layout.stickX
        val y = layout.centerY
        paint.color = 0xff0a1220.toInt(); canvas.drawCircle(x, y + 4 * d, layout.stickRadius, paint)
        paint.color = 0xff24334a.toInt(); canvas.drawCircle(x, y, layout.stickRadius, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d; paint.color = 0xff3c506c.toInt()
        canvas.drawCircle(x, y, layout.stickRadius - 2 * d, paint); paint.style = Paint.Style.FILL
        chevron(canvas, x - 37 * d, y, -1f, d)
        chevron(canvas, x + 37 * d, y, 1f, d)
        val knobX = x + touch.stickOffset * layout.stickTravel
        paint.color = 0xff0b1424.toInt(); canvas.drawCircle(knobX, y + 4 * d, 25 * d, paint)
        paint.color = if (abs(touch.stickOffset) > .7f) 0xffa1eac2.toInt() else 0xffe0e9f5.toInt()
        canvas.drawCircle(knobX, y, 25 * d, paint)
        paint.color = 0x70ffffff; canvas.drawCircle(knobX - 5 * d, y - 7 * d, 8 * d, paint)

        val jx = layout.jumpX
        paint.color = 0xff823a33.toInt(); canvas.drawCircle(jx, y + 5 * d, layout.jumpRadius, paint)
        paint.color = if (touch.jumpPressed) 0xffffa58c.toInt() else 0xffff705f.toInt()
        canvas.drawCircle(jx, y, layout.jumpRadius, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d; paint.color = 0x50ffffff
        canvas.drawCircle(jx, y, layout.jumpRadius - 4 * d, paint); paint.style = Paint.Style.FILL
        paint.color = Color.WHITE; paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3 * d; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        arrow.reset(); arrow.moveTo(jx - 9 * d, y - 1 * d); arrow.lineTo(jx, y - 10 * d)
        arrow.lineTo(jx + 9 * d, y - 1 * d); arrow.moveTo(jx, y - 10 * d); arrow.lineTo(jx, y + 10 * d)
        canvas.drawPath(arrow, paint); paint.style = Paint.Style.FILL
        text(canvas, jumpLabel, jx, y + 28 * d, 9 * d, Color.WHITE)
        text(canvas, if (abs(touch.stickOffset) > .7f) runLabel else moveLabel, x, y + 72 * d, 9 * d, 0xffa8bfd8.toInt())
        text(canvas, jumpHint, jx, y + 72 * d, 8 * d, 0xffa8bfd8.toInt())
    }

    private fun chevron(canvas: Canvas, x: Float, y: Float, direction: Float, density: Float) {
        paint.color = 0xff8ca7c7.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * density
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        arrow.reset(); arrow.moveTo(x - direction * 3 * density, y - 6 * density)
        arrow.lineTo(x + direction * 3 * density, y); arrow.lineTo(x - direction * 3 * density, y + 6 * density)
        canvas.drawPath(arrow, paint); paint.style = Paint.Style.FILL
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int) {
        paint.typeface = labelTypeface; paint.textAlign = Paint.Align.CENTER
        paint.textSize = size; paint.color = color; canvas.drawText(value, x, y, paint)
    }
}
