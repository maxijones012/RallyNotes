package ar.com.rallygpsai

import kotlin.math.*

/** Distances available in prerecorded Spanish as well as Android TTS. */
object RallySpokenDistance {
    fun roundMetres(distance: Double): Int {
        if (!distance.isFinite() || distance <= 22.0) return 0
        if (distance > 400.0) return ((distance / 100.0).roundToInt() * 100).coerceIn(500, 1000)
        return ((distance / 10.0).roundToInt() * 10).coerceIn(20, 400)
    }
}

/** Projecta GPS a segmentos, no a vertices, para evitar saltos en trazados irregulares. */
data class RouteFix(val metres: Double, val errorM: Double, val segment: Int)
data class Guidance(val fix: RouteFix, val noteIndex: Int, val distanceToNoteM: Double,
                    val announcement: String?, val offRoute: Boolean, val arrived: Boolean)

class NavigationEngine(private val plan: RoutePlan, private val anticipation: () -> Int = { 1 }) {
    private var lastProgress = 0.0
    private var lastSegment = 0
    private val announced = mutableSetOf<Int>()
    private val earlyAnnounced = mutableSetOf<Int>()
    private var arrivalAnnounced = false
    private var lastFixAtNs = 0L

    fun reset() {
        lastProgress = 0.0; lastSegment = 0; announced.clear(); earlyAnnounced.clear(); arrivalAnnounced = false; lastFixAtNs = 0L
    }

    fun locate(location: GeoPoint): RouteFix {
        val coords = plan.points
        if (coords.size < 2) return RouteFix(0.0, Double.POSITIVE_INFINITY, 0)
        val r = 6371000.0
        val cosLat = cos(Math.toRadians(location.latitude)).coerceAtLeast(0.001)
        fun xy(p: GeoPoint) = Pair(Math.toRadians(p.longitude - location.longitude) * cosLat * r,
                                   Math.toRadians(p.latitude - location.latitude) * r)
        var bestDist = Double.POSITIVE_INFINITY
        var bestMetres = lastProgress
        var bestSegment = lastSegment
        val first = (lastSegment - 25).coerceAtLeast(0)
        // 600 m forward window: GPS can recover after tunnels, but cannot snap to an unrelated lap.
        val forwardEnd = (lastProgress + 650.0).coerceAtMost(plan.totalMetres)
        var last = plan.metresAtPoint.binarySearch(forwardEnd).let { if (it < 0) -it-1 else it }
        last = (last + 2).coerceIn((first+1).coerceAtMost(coords.lastIndex), coords.lastIndex)
        for (i in first until last) {
            val (ax,ay) = xy(coords[i]); val (bx,by) = xy(coords[i+1])
            val dx = bx - ax; val dy = by - ay
            val d2 = dx * dx + dy * dy
            val t = if (d2 < 0.0001) 0.0 else (-(ax*dx + ay*dy) / d2).coerceIn(0.0,1.0)
            val e = hypot(ax+t*dx,ay+t*dy)
            if (e < bestDist) {
                bestDist = e
                bestSegment = i
                bestMetres = plan.metresAtPoint[i] + t*(plan.metresAtPoint[i+1]-plan.metresAtPoint[i])
            }
        }
        return RouteFix(bestMetres, bestDist, bestSegment)
    }

    /** Accepted fixes never rewind the route. Unreliable positions don't change the cursor. */
    fun update(location: GeoPoint, speedMs: Float, gpsAccuracyM: Float = 5f): Guidance {
        val fix = locate(location)
        val offRoute = fix.errorM > max(55.0, min(90.0, gpsAccuracyM*2.0)) || gpsAccuracyM > 65f
        if (!offRoute) {
            val now = System.nanoTime()
            val elapsed = if (lastFixAtNs == 0L) 5.0 else
                ((now - lastFixAtNs) / 1e9).coerceIn(0.1, 40.0)
            val maxAdvance = max(70.0, speedMs.coerceAtLeast(0f).toDouble() * elapsed * 2.5 + 45.0)
            val moved = fix.metres - lastProgress
            if (moved >= -20.0 && moved <= maxAdvance) {
                lastProgress = max(lastProgress, fix.metres)
                lastSegment = fix.segment
                lastFixAtNs = now
            }
        }
        val index = plan.notes.indexOfFirst { it.metresFromStart + 12.0 >= lastProgress }
        val distance = if (index < 0) Double.POSITIVE_INFINITY else
            (plan.notes[index].metresFromStart - lastProgress).coerceAtLeast(0.0)
        val arrived = !offRoute && plan.totalMetres - lastProgress <= 20.0
        var speech: String? = null
        if (arrived && !arrivalAnnounced) {
            arrivalAnnounced = true; speech = "Llegaste al final del recorrido"
        } else if (!offRoute && index >= 0) {
            val note = plan.notes[index]
            val following = plan.notes.getOrNull(index + 1)
            val nearThreshold = PaceNotes.warningDistance(speedMs, anticipation())
            // Two call points: distance preview (~800 m), then the actual nearby bend.
            // A note that begins inside the near zone gets just one announcement.
            val near = distance <= nearThreshold
            val early = !near && distance <= 800.0
            val shouldSpeak = when {
                near -> announced.add(index)
                early -> earlyAnnounced.add(index)
                else -> false
            }
            if (shouldSpeak) {
                val rounded = RallySpokenDistance.roundMetres(distance)
                val prefix = if (rounded == 0) "Ahora, " else "En $rounded metros, "
                speech = prefix + PaceNotes.phrase(note, following)
                // Linked turns are delivered in the same call, so suppress an immediate echo.
                if (following != null &&
                    following.metresFromStart - note.endMetres in 0.0..85.0) {
                    if (near) announced.add(index+1) else earlyAnnounced.add(index+1)
                }
            }
        }

        return Guidance(fix.copy(metres=lastProgress), index, distance, speech, offRoute, arrived)
    }
}
