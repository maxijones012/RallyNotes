package ar.com.rallygpsai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/** Hand-drawn high contrast direction + grade: stable across Android fonts. */
class RallyNoteTile(context: Context) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundRect = RectF()
    var grade: Int = 4
        private set
    var side: PaceNote.Side = PaceNote.Side.RIGHT
        private set

    fun show(grade: Int, side: PaceNote.Side) {
        this.grade = grade.coerceIn(1, 6)
        this.side = side
        contentDescription = "${if (side == PaceNote.Side.LEFT) "Izquierda" else "Derecha"} ${this.grade}"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        val tone = Color.parseColor(PaceNotes.color(grade))
        val foreground = Color.parseColor("#101522")
        backgroundRect.set(0f, 0f, w, h)
        ink.style = Paint.Style.FILL
        ink.color = tone
        canvas.drawRoundRect(backgroundRect, s * 0.12f, s * 0.12f, ink)

        // Pacenote pictogram: curved route, arrow toward left/right at its exit.
        val mirror = if (side == PaceNote.Side.LEFT) -1f else 1f
        val stemX = w * 0.37f
        val yBottom = h * 0.78f
        val yTurn = h * 0.30f
        val tipX = stemX + mirror * w * 0.16f
        val arc = Path().apply {
            moveTo(stemX, yBottom)
            lineTo(stemX, h * 0.48f)
            cubicTo(stemX, yTurn, stemX + mirror * w * 0.07f, yTurn, tipX, yTurn)
        }
        ink.style = Paint.Style.STROKE
        ink.strokeCap = Paint.Cap.ROUND
        ink.strokeJoin = Paint.Join.ROUND
        ink.strokeWidth = s * 0.080f
        ink.color = foreground
        canvas.drawPath(arc, ink)
        val head = Path().apply {
            moveTo(tipX - mirror * s * 0.085f, yTurn - s * 0.075f)
            lineTo(tipX, yTurn)
            lineTo(tipX - mirror * s * 0.085f, yTurn + s * 0.075f)
        }
        canvas.drawPath(head, ink)
        ink.style = Paint.Style.FILL
        ink.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        ink.textSize = s * 0.49f
        ink.textAlign = Paint.Align.CENTER
        val font = ink.fontMetrics
        val baseline = h * 0.68f - (font.ascent + font.descent) / 2f
        canvas.drawText(grade.toString(), w * 0.75f, baseline, ink)
    }
}
