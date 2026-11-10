package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RouteProgressGeometryTest {
    private val points = listOf(GeoPoint(-43.0,-65.0),GeoPoint(-43.0,-64.99),GeoPoint(-43.01,-64.99))
    private val route = PaceNoteEngine.createPlan(points)

    @Test fun startAndEndIncludeCorrectCoordinates() {
        val first=RouteProgressGeometry.at(route,0.0)
        assertEquals(points.first(),first.travelled.first())
        assertEquals(points.last(),first.upcoming.last())
        val end=RouteProgressGeometry.at(route,route.totalMetres)
        assertEquals(points.last(),end.travelled.last())
    }
    @Test fun pathIsContinuousInMiddle() {
        val split=RouteProgressGeometry.at(route,route.totalMetres/2)
        assertEquals(split.travelled.last(),split.upcoming.first())
        assertEquals(points.first(),split.travelled.first())
        assertEquals(points.last(),split.upcoming.last())
    }
    @Test fun nonsenseDistanceDoesNotCorruptGeometry() {
        assertEquals(points.first(),RouteProgressGeometry.at(route,Double.NaN).upcoming.first())
    }
}
