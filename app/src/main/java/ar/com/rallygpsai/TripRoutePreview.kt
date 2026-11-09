package ar.com.rallygpsai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.min

/** Lightweight local track thumbnail; no map network access and no fake streets. */
class TripRoutePreview(context: Context, private val track: List<GeoPoint>) : View(context) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (resources.displayMetrics.density * 160).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(18,27,42))
        if (track.size < 2) return
        val meanLat = track.map { it.latitude }.average()
        val cosLat = cos(Math.toRadians(meanLat)).coerceAtLeast(0.1)
        val points = track.map { Pair(it.longitude * cosLat, it.latitude) }
        val minX = points.minOf { it.first }; val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }; val maxY = points.maxOf { it.second }
        val pad = 22f
        val usableW = (width - pad * 2).coerceAtLeast(1f)
        val usableH = (height - pad * 2).coerceAtLeast(1f)
        val spanX = (maxX-minX).coerceAtLeast(0.00000001)
        val spanY = (maxY-minY).coerceAtLeast(0.00000001)
        val scale = min(usableW/spanX.toFloat(), usableH/spanY.toFloat())
        fun toX(x: Double) = (width/2f + ((x-(minX+maxX)/2)*scale).toFloat())
        fun toY(y: Double) = (height/2f - ((y-(minY+maxY)/2)*scale).toFloat())
        val path = Path()
        points.forEachIndexed { index, p ->
            if (index == 0) path.moveTo(toX(p.first),toY(p.second))
            else path.lineTo(toX(p.first),toY(p.second))
        }
        ink.style=Paint.Style.STROKE; ink.strokeCap=Paint.Cap.ROUND; ink.strokeJoin=Paint.Join.ROUND
        ink.color=Color.rgb(70,220,180); ink.strokeWidth=7f; canvas.drawPath(path,ink)
        ink.color=Color.rgb(137,104,245); ink.strokeWidth=3.5f; canvas.drawPath(path,ink)
        ink.style=Paint.Style.FILL
        val first=points.first();val last=points.last()
        ink.color=Color.rgb(57,211,154);canvas.drawCircle(toX(first.first),toY(first.second),8f,ink)
        ink.color=Color.rgb(255,119,90);canvas.drawCircle(toX(last.first),toY(last.second),8f,ink)
    }
}
