package ar.com.rallygpsai

/** Display-only geometry. Never modify the actual routing or pacenotes for these strokes. */
data class RouteProgressShape(val travelled: List<GeoPoint>, val upcoming: List<GeoPoint>)

object RouteProgressGeometry {
    fun at(plan: RoutePlan, metres: Double): RouteProgressShape {
        val p = plan.points
        if(p.size < 2) return RouteProgressShape(emptyList(),p)
        val distance = if(metres.isFinite()) metres.coerceIn(0.0,plan.totalMetres) else 0.0
        val head = DrivingCamera.pointAt(plan,distance)
        val index = plan.metresAtPoint.indexOfLast { it <= distance }.coerceIn(0,p.lastIndex-1)
        val done = p.subList(0,index+1) + head
        val left = listOf(head) + p.subList(index+1,p.size)
        return RouteProgressShape(done,left)
    }
}
