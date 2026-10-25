package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class DrivingCameraTest {
    private fun route(vararg coords: GeoPoint): RoutePlan = PaceNoteEngine.createPlan(coords.toList())

    @Test fun straightNorthKeepsNorthAhead() {
        val plan = route(GeoPoint(-43.250, -65.300), GeoPoint(-43.240, -65.300))
        val pose = DrivingCamera().pose(plan, 25.0, 15.0, true)
        assertTrue(pose.bearing < 5 || pose.bearing > 355)
        assertTrue(pose.focus.latitude > plan.points.first().latitude)
        assertEquals(60.0, pose.tilt, 0.01)
    }
    @Test fun straightEastPutsEastAtTop() {
        val plan = route(GeoPoint(-43.250, -65.300), GeoPoint(-43.250, -65.290))
        val pose = DrivingCamera().pose(plan, 20.0, 10.0, true)
        assertEquals(90.0, pose.bearing, 5.0)
    }
    @Test fun northUpTwoDMode() {
        val plan = route(GeoPoint(-43.250, -65.300), GeoPoint(-43.249, -65.299))
        val pose = DrivingCamera().pose(plan, 20.0, 10.0, false)
        assertEquals(0.0, pose.bearing, 0.01)
        assertEquals(0.0, pose.tilt, 0.01)
    }
    @Test fun shortestTurnAcrossNorth() {
        assertEquals(20.0, DrivingCamera.shortestDelta(350.0, 10.0), 0.01)
        assertEquals(-20.0, DrivingCamera.shortestDelta(10.0, 350.0), 0.01)
    }
    @Test fun interpolatesTheRoute() {
        val plan = route(GeoPoint(-43.25,-65.3), GeoPoint(-43.24,-65.3))
        val mid = DrivingCamera.pointAt(plan, plan.totalMetres/2)
        assertEquals(-43.245, mid.latitude, 0.0005)
    }
    @Test fun endClampsSafely() {
        val plan = route(GeoPoint(-43.25,-65.3), GeoPoint(-43.24,-65.3))
        val pose = DrivingCamera().pose(plan, plan.totalMetres + 500.0, 25.0, true)
        assertTrue(pose.focus.latitude <= -43.24 + 0.00001)
        assertTrue(pose.bearing.isFinite())
    }
}
