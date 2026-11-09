package ar.com.rallygpsai

import kotlin.math.*

/** Trip statistics from real, accuracy-filtered GPS fixes only, never DEMO. */
data class TripRecord(
    val id: String,
    val startedAtWallMs: Long,
    val durationMs: Long,
    val distanceM: Double,
    val averageKmh: Double,
    val maximumKmh: Double,
    val route: List<GeoPoint>,
    val actualTrack: List<GeoPoint>
)

class RealTripRecorder(
    private val wallStartMs: Long,
    private val clockStartMs: Long,
    private val plannedRoute: List<GeoPoint>
) {
    private var last: GeoPoint? = null
    private var lastMs = 0L
    private var distanceM = 0.0
    private var maximumKmh = 0.0
    private val sampledTrack = mutableListOf<GeoPoint>()

    fun accept(point: GeoPoint, gpsSpeedMps: Float, accuracyM: Float, clockMs: Long) {
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0 ||
            !point.latitude.isFinite() || !point.longitude.isFinite() ||
            !accuracyM.isFinite() || accuracyM > 25f || accuracyM < 0f ||
            !gpsSpeedMps.isFinite() || gpsSpeedMps < 0f || gpsSpeedMps > 70f ||
            clockMs < clockStartMs) return
        // Do not use DEMO velocity, noisy location fixes, or impossible position jumps.
        val current = last
        val dt = clockMs - lastMs
        if (current != null && dt in 200L..20_000L) {
            val distance = distanceBetween(current, point)
            val limit = max(25.0, gpsSpeedMps * (dt / 1000.0) * 2.6 + 15.0)
            if (distance > limit) {
                // Treat implausible fix as an outlier, retaining the last good position.
                return
            }
            if (distance >= 4.0 && (gpsSpeedMps >= 1.0f || distance >= 12.0)) {
                distanceM += distance
            }
        }
        // An outage does not add the skipped displacement to statistics.
        last = point
        lastMs = clockMs
        maximumKmh = max(maximumKmh, gpsSpeedMps * 3.6)
        if (sampledTrack.size < 5000 && (sampledTrack.isEmpty() ||
            distanceBetween(sampledTrack.last(), point) >= 5.0)) sampledTrack.add(point)
    }

    fun finish(clockMs: Long): TripRecord? {
        val duration = (clockMs - clockStartMs).coerceAtLeast(0L)
        if (duration < 15_000L || distanceM < 30.0 || sampledTrack.size < 2) return null
        val average = (distanceM / (duration / 1000.0)) * 3.6
        return TripRecord("trip_" + wallStartMs, wallStartMs, duration,
            distanceM, average.coerceIn(0.0, 250.0), maximumKmh,
            plannedRoute.toList(), sampledTrack.toList())
    }

    companion object {
        fun distanceBetween(a: GeoPoint, b: GeoPoint): Double {
            val rad = Math.PI / 180.0
            val dLat = (b.latitude-a.latitude)*rad
            val dLon = (b.longitude-a.longitude)*rad
            val p = sin(dLat/2).pow(2) + cos(a.latitude*rad) *
                cos(b.latitude*rad) * sin(dLon/2).pow(2)
            return 12742000.0 * asin(sqrt(p.coerceIn(0.0, 1.0)))
        }
    }
}
