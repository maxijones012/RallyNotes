package ar.com.rallygpsai

import kotlin.math.*

/**
 * Motor geométrico independiente de Android y de servicios de terceros.
 * NO determina velocidades seguras, peligros del terreno ni condiciones reales.
 */
data class GeoPoint(val latitude: Double, val longitude: Double)

data class PaceNote(
    val metresFromStart: Double,
    val side: Side,
    val grade: Int,
    val cumulativeTurnDegrees: Int,
    val approximateRadiusM: Double,
    val endMetres: Double = metresFromStart,
    val tightensTo: Int? = null,
    val opensTo: Int? = null,
    val confidence: Double = 0.65,
) {
    enum class Side { LEFT, RIGHT }
    val title: String get() = "${if (side == Side.LEFT) "Izquierda" else "Derecha"} $grade"
}

data class RoutePlan(
    val points: List<GeoPoint>,
    val metresAtPoint: List<Double>,
    val notes: List<PaceNote>,
) {
    val totalMetres: Double get() = metresAtPoint.lastOrNull() ?: 0.0
}

object PaceNoteEngine {
    private const val EARTH = 6371000.0
    private const val SAMPLE_M = 5.0
    private const val WINDOW = 3

    fun createPlan(route: List<GeoPoint>): RoutePlan {
        require(route.size >= 2) { "Se requieren al menos dos puntos para una ruta" }
        require(route.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 })
        val refLat = Math.toRadians(route.first().latitude)
        val refLon = Math.toRadians(route.first().longitude)
        val cosLat = cos(refLat).coerceAtLeast(0.01)
        fun project(p: GeoPoint) = XY(
            (Math.toRadians(p.longitude) - refLon) * EARTH * cosLat,
            (Math.toRadians(p.latitude) - refLat) * EARTH,
        )
        fun unproject(p: XY) = GeoPoint(
            Math.toDegrees(p.y / EARTH + refLat),
            Math.toDegrees(p.x / (EARTH * cosLat) + refLon),
        )
        // Uniform arc-length resampling is required for GPX tracks of any point density.
        // Interpolating ONLY long segments misses every tight curve in a dense GPX file.
        val raw = route.map(::project).fold(mutableListOf<XY>()) { acc, p ->
            if (acc.isEmpty() || acc.last().distance(p) >= 0.25) acc.add(p)
            acc
        }
        if (raw.size < 2) return RoutePlan(listOf(route.first(), route.last()), listOf(0.0, 0.0), emptyList())
        val rawDistances = DoubleArray(raw.size)
        for (j in 1 until raw.size) rawDistances[j] = rawDistances[j-1] + raw[j-1].distance(raw[j])
        val total = rawDistances.last()
        val sampled = mutableListOf<XY>()
        var segment = 1
        var at = 0.0
        while (at < total) {
            while (segment < raw.lastIndex && rawDistances[segment] < at) segment++
            val den = (rawDistances[segment] - rawDistances[segment-1]).coerceAtLeast(0.00001)
            val t = ((at-rawDistances[segment-1]) / den).coerceIn(0.0,1.0)
            val a = raw[segment-1]; val b = raw[segment]
            sampled.add(XY(a.x+(b.x-a.x)*t, a.y+(b.y-a.y)*t))
            at += SAMPLE_M
        }
        sampled.add(raw.last())
        val smooth = sampled.mapIndexed { i, p ->
            if (i == 0 || i == sampled.lastIndex) p
            else XY(
                (sampled[i - 1].x + 2.0 * p.x + sampled[i + 1].x) / 4.0,
                (sampled[i - 1].y + 2.0 * p.y + sampled[i + 1].y) / 4.0,
            )
        }
        val chain = MutableList(smooth.size) { 0.0 }
        for (i in 1 until smooth.size) chain[i] = chain[i - 1] + smooth[i - 1].distance(smooth[i])
        val curvature = DoubleArray(smooth.size)
        val turnSign = IntArray(smooth.size)
        for (i in WINDOW until smooth.size - WINDOW) {
            val a = smooth[i - WINDOW]
            val b = smooth[i]
            val c = smooth[i + WINDOW]
            val h1 = atan2(b.y - a.y, b.x - a.x)
            val h2 = atan2(c.y - b.y, c.x - b.x)
            val delta = wrap(h2 - h1)
            // Cambio de rumbo en ~60 m. El radio es únicamente una aproximación geométrica.
            if (abs(delta) < Math.toRadians(5.0)) continue
            curvature[i] = abs(delta)
            turnSign[i] = if (delta > 0.0) 1 else -1
        }
        fun grade(radius: Double) = when {
            radius < 22.0 -> 1
            radius < 40.0 -> 2
            radius < 65.0 -> 3
            radius < 100.0 -> 4
            radius < 165.0 -> 5
            else -> 6
        }
        fun radiusAt(k: Int): Double {
            val left = (k - WINDOW).coerceAtLeast(0)
            val right = (k + WINDOW).coerceAtMost(chain.lastIndex)
            return ((chain[right] - chain[left]) / (2.0*curvature[k].coerceAtLeast(0.01))).coerceAtLeast(1.0)
        }
        val detected = mutableListOf<PaceNote>()
        var i = WINDOW
        while (i < smooth.size - WINDOW) {
            val sign = turnSign[i]
            if (sign == 0) { i++; continue }
            val start = i
            var peak = i
            while (i < smooth.size - WINDOW && turnSign[i] == sign) {
                if (curvature[i] > curvature[peak]) peak = i
                i++
            }
            val end = i - 1
            // Ángulo neto de la curva: evita anunciar pequeñas ondulaciones del dibujo.
            val before = smooth[(start - WINDOW).coerceAtLeast(0)]
            val first = smooth[start]
            val last = smooth[end]
            val after = smooth[(end + WINDOW).coerceAtMost(smooth.lastIndex)]
            val turn = abs(Math.toDegrees(wrap(
                atan2(after.y - last.y, after.x - last.x) -
                atan2(first.y - before.y, first.x - before.x)
            )))
            if (turn < 18.0) continue
            val arc = chain[(peak + WINDOW).coerceAtMost(chain.lastIndex)] -
                chain[(peak - WINDOW).coerceAtLeast(0)]
            val radius = (arc / (2.0 * curvature[peak])).coerceAtLeast(1.0)
            val mainGrade = grade(radius)
            // Only call a changing radius when the curve contains enough samples to support it.
            val endMetres = chain[(end + WINDOW).coerceAtMost(chain.lastIndex)]
            val mid = (start + end) / 2
            val early = (start + mid) / 2
            val late = (mid + end) / 2
            val earlyGrade = grade(radiusAt(early))
            val lateGrade = grade(radiusAt(late))
            val tightensTo = if (end-start >= 10 && lateGrade + 1 < earlyGrade) lateGrade else null
            val opensTo = if (end-start >= 10 && earlyGrade + 1 < lateGrade) lateGrade else null
            val quality = if (end-start >= 5 && turn >= 30.0) 0.8 else 0.55
            detected.add(PaceNote(chain[start], if (sign > 0) PaceNote.Side.LEFT else PaceNote.Side.RIGHT,
                mainGrade, turn.roundToInt(), radius, endMetres, tightensTo, opensTo, quality))
        }
        // Un mismo arco no debe generar múltiples notas repetidas.
        val notes = mutableListOf<PaceNote>()
        for (n in detected) {
            val prev = notes.lastOrNull()
            if (prev != null && n.metresFromStart - prev.metresFromStart < 45.0 && prev.side == n.side) {
                if (n.grade < prev.grade) notes[notes.lastIndex] = prev.copy(grade = n.grade,
                    cumulativeTurnDegrees = max(prev.cumulativeTurnDegrees, n.cumulativeTurnDegrees),
                    approximateRadiusM = min(prev.approximateRadiusM, n.approximateRadiusM),
                    endMetres = max(prev.endMetres, n.endMetres))
            } else notes.add(n)
        }
        return RoutePlan(smooth.map(::unproject), chain, notes)
    }

    /** Posición longitudinal aproximada. Requiere filtro adicional para rutas en bucle. */
    fun nearestProgress(plan: RoutePlan, point: GeoPoint, lowerIndex: Int = 0): Pair<Int, Double> {
        if (plan.points.isEmpty()) return 0 to 0.0
        val refLat = Math.toRadians(point.latitude)
        val cosLat = cos(refLat)
        val start = lowerIndex.coerceIn(0, plan.points.lastIndex)
        var best = start
        var bestDistance2 = Double.POSITIVE_INFINITY
        // Ventana permite recuperar GPS tras una pérdida breve, sin saltar a otra vuelta de un circuito.
        val first = (start - 15).coerceAtLeast(0)
        val last = (start + 350).coerceAtMost(plan.points.lastIndex)
        for (i in first..last) {
            val p = plan.points[i]
            val dx = Math.toRadians(p.longitude - point.longitude) * cosLat * EARTH
            val dy = Math.toRadians(p.latitude - point.latitude) * EARTH
            val dist2 = dx * dx + dy * dy
            if (dist2 < bestDistance2) { bestDistance2 = dist2; best = i }
        }
        return best to sqrt(bestDistance2)
    }

    private data class XY(val x: Double, val y: Double) {
        fun distance(other: XY) = hypot(other.x - x, other.y - y)
    }
    private fun wrap(angle: Double): Double {
        var a = angle
        while (a > Math.PI) a -= 2 * Math.PI
        while (a < -Math.PI) a += 2 * Math.PI
        return a
    }
}
