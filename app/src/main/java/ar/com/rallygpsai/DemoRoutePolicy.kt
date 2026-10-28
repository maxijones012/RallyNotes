package ar.com.rallygpsai

/** Demo runs on an existing route only. Never create synthetic roads on the real map. */
object DemoRoutePolicy {
    fun canStart(plan: RoutePlan?): Boolean {
        if (plan == null || plan.points.size < 2) return false
        if (plan.metresAtPoint.size != plan.points.size) return false
        if (plan.totalMetres < 25.0 || !plan.totalMetres.isFinite()) return false
        if (plan.points.any { !it.latitude.isFinite() || !it.longitude.isFinite() ||
            it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) return false
        return true
    }
}
