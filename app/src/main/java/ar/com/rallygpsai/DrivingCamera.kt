package ar.com.rallygpsai

import kotlin.math.*

/**
 * Perspectiva de navegación orientada por la polilínea (no cámara del teléfono).
 * Es independiente de Android para verificar rumbo, interpolación y giros.
 */
data class DrivingCameraPose(val focus: GeoPoint, val bearing: Double, val zoom: Double, val tilt: Double)

class DrivingCamera {
    private var previousBearing: Double? = null

    fun reset() { previousBearing = null }

    fun pose(plan: RoutePlan, progressM: Double, speedMps: Double, perspective: Boolean): DrivingCameraPose {
        val at = progressM.coerceIn(0.0, plan.totalMetres)
        if (!perspective) return DrivingCameraPose(pointAt(plan, at), 0.0, 16.0, 0.0)
        // El rumbo se mide delante de la posición para que funcione también en DEMO
        // y a baja velocidad, cuando la brújula o el GPS no entregan bearing fiable.
        val p1 = pointAt(plan, (at - 5.0).coerceAtLeast(0.0))
        val p2 = pointAt(plan, (at + 19.0).coerceAtMost(plan.totalMetres))
        val rawBearing = if (distance(p1, p2) > 0.8) bearing(p1, p2) else previousBearing ?: 0.0
        val last = previousBearing
        val corrected = if (last == null) rawBearing else normalize(last + shortestDelta(last, rawBearing) * 0.42)
        previousBearing = corrected
        // Objetivo adelantado: el vehículo queda en la parte inferior del mapa.
        // Se ajusta por velocidad sin saltarse curvas demasiado próximas.
        val lookAheadM = (38.0 + speedMps.coerceIn(0.0, 35.0) * 1.5).coerceIn(38.0, 90.0)
        val focal = pointAt(plan, (at + lookAheadM).coerceAtMost(plan.totalMetres))
        val zoom = when {
            speedMps > 24 -> 16.4
            speedMps > 13 -> 16.9
            else -> 17.3
        }
        return DrivingCameraPose(focal, corrected, zoom, 60.0)
    }

    companion object {
        fun normalize(angle: Double): Double = ((angle % 360.0) + 360.0) % 360.0
        fun shortestDelta(from: Double, to: Double): Double = ((to - from + 540.0) % 360.0) - 180.0
        fun pointAt(plan: RoutePlan, m: Double): GeoPoint {
            val points = plan.points
            if (points.isEmpty()) return GeoPoint(0.0,0.0)
            if (points.size == 1) return points.first()
            val distance = m.coerceIn(0.0, plan.totalMetres)
            val chain = plan.metresAtPoint
            val match = chain.binarySearch(distance)
            if (match >= 0) return points[match]
            val right = (-match - 1).coerceIn(1, points.lastIndex)
            val left = right - 1
            val fraction = ((distance - chain[left]) / (chain[right] - chain[left]).coerceAtLeast(0.000001)).coerceIn(0.0, 1.0)
            return GeoPoint(points[left].latitude + (points[right].latitude - points[left].latitude) * fraction,
                points[left].longitude + (points[right].longitude - points[left].longitude) * fraction)
        }
        fun bearing(from: GeoPoint, to: GeoPoint): Double {
            val a = Math.toRadians(from.latitude)
            val b = Math.toRadians(to.latitude)
            val dLon = Math.toRadians(to.longitude - from.longitude)
            val east = sin(dLon) * cos(b)
            val north = cos(a) * sin(b) - sin(a) * cos(b) * cos(dLon)
            return normalize(Math.toDegrees(atan2(east, north)))
        }
        fun distance(a: GeoPoint, b: GeoPoint): Double {
            val lat = Math.toRadians(b.latitude-a.latitude)
            val lon = Math.toRadians(b.longitude-a.longitude)
            val c = sin(lat/2).pow(2) + cos(Math.toRadians(a.latitude))*cos(Math.toRadians(b.latitude))*sin(lon/2).pow(2)
            return 12742000.0 * asin(sqrt(c.coerceIn(0.0,1.0)))
        }
    }
}
