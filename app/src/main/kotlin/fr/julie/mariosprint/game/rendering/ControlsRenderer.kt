package fr.julie.mariosprint.game.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import fr.julie.mariosprint.game.input.ControlLayout
import fr.julie.mariosprint.game.input.TouchControls
import kotlin.math.abs

/** Même géométrie tactile ; rendu plat et libellés centrés avec les métriques de police. */
class ControlsRenderer(private val jumpLabel: String, private val runLabel: String,
    private val moveLabel: String, private val jumpHint: String) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Path()
    private val fontMetrics = Paint.FontMetrics()
    private val labelTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun draw(canvas: Canvas, layout: ControlLayout, touch: TouchControls) {
        val d = layout.density
        paint.style = Paint.Style.FILL
        paint.color = BACKGROUND
        canvas.drawRect(0f, layout.panelTop, layout.width, layout.height, paint)
        paint.color = OUTLINE
        canvas.drawRect(20 * d, layout.panelTop, layout.width - 20 * d, layout.panelTop + d, paint)

        val x = layout.stickX
        val y = layout.centerY
        val running = abs(touch.stickOffset) > .7f
        paint.color = SURFACE
        canvas.drawCircle(x, y, layout.stickRadius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * d
        paint.color = if (running) MINT else OUTLINE
        canvas.drawCircle(x, y, layout.stickRadius - d, paint)
        paint.style = Paint.Style.FILL
        chevron(canvas, x - 37 * d, y, -1f, d)
        chevron(canvas, x + 37 * d, y, 1f, d)
        val knobX = x + touch.stickOffset * layout.stickTravel
        paint.color = if (running) MINT else TEXT
        canvas.drawCircle(knobX, y, 24 * d, paint)

        val jumpX = layout.jumpX
        paint.color = if (touch.jumpPressed) PRESSED_CORAL else CORAL
        canvas.drawCircle(jumpX, y, layout.jumpRadius, paint)
        paint.color = BACKGROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * d
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        arrow.reset()
        arrow.moveTo(jumpX - 8 * d, y - 8 * d)
        arrow.lineTo(jumpX, y - 16 * d)
        arrow.lineTo(jumpX + 8 * d, y - 8 * d)
        arrow.moveTo(jumpX, y - 16 * d)
        arrow.lineTo(jumpX, y + 2 * d)
        canvas.drawPath(arrow, paint)
        paint.style = Paint.Style.FILL
        centeredText(canvas, jumpLabel, jumpX, y + 19 * d, 13 * d, BACKGROUND)
        centeredText(canvas, if (running) runLabel else moveLabel, x, y + 72 * d, 12 * d, MUTED)
        centeredText(canvas, jumpHint, jumpX, y + 72 * d, 11 * d, MUTED)
    }

    private fun chevron(canvas: Canvas, x: Float, y: Float, direction: Float, density: Float) {
        paint.color = MUTED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2 * density
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        arrow.reset()
        arrow.moveTo(x - direction * 3 * density, y - 6 * density)
        arrow.lineTo(x + direction * 3 * density, y)
        arrow.lineTo(x - direction * 3 * density, y + 6 * density)
        canvas.drawPath(arrow, paint)
        paint.style = Paint.Style.FILL
    }

    private fun centeredText(canvas: Canvas, value: String, x: Float, centerY: Float, size: Float, color: Int) {
        paint.typeface = labelTypeface
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        paint.color = color
        paint.getFontMetrics(fontMetrics)
        canvas.drawText(value, x, centerY - (fontMetrics.ascent + fontMetrics.descent) / 2, paint)
    }

    private companion object {
        const val BACKGROUND = 0xff101928.toInt()
        const val SURFACE = 0xff1a293d.toInt()
        const val OUTLINE = 0xff354a63.toInt()
        const val CORAL = 0xffff7966.toInt()
        const val PRESSED_CORAL = 0xffffa08f.toInt()
        const val MINT = 0xfface7c5.toInt()
        const val TEXT = 0xfff7f1e7.toInt()
        const val MUTED = 0xffabc0d5.toInt()
    }
}
