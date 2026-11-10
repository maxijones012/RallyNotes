package ar.com.rallygpsai

/** Conservative GPS acceptance. Never invent movement when precision is unknown. */
object GpsFixPolicy {
    fun usable(latitude: Double, longitude: Double, accuracyM: Float?, ageMs: Long): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            accuracyM != null && accuracyM.isFinite() &&
            accuracyM in 0f..55f && ageMs in -2_000L..15_000L

    fun safeSpeed(speedMps: Float?, hasSpeed: Boolean): Float =
        if (hasSpeed && speedMps != null && speedMps.isFinite() &&
            speedMps >= 0f && speedMps <= 70f) speedMps else 0f
}
