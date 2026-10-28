package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class DemoRoutePolicyTest {
    @Test fun noRouteMeansNoSimulation() {
        assertFalse(DemoRoutePolicy.canStart(null))
    }

    @Test fun invalidOrDegenerateRoutesAreRejected() {
        assertFalse(DemoRoutePolicy.canStart(RoutePlan(
            listOf(GeoPoint(-43.0,-65.0), GeoPoint(-43.0,-65.0)), listOf(0.0,0.0), emptyList())))
        assertFalse(DemoRoutePolicy.canStart(RoutePlan(
            listOf(GeoPoint(-43.0,-65.0), GeoPoint(-43.0,-64.999)), listOf(0.0), emptyList())))
        assertFalse(DemoRoutePolicy.canStart(RoutePlan(
            listOf(GeoPoint(Double.NaN,-65.0), GeoPoint(-43.0,-64.999)), listOf(0.0,80.0), emptyList())))
    }

    @Test fun selectedGpxOrCalculatedGeometryCanBeSimulated() {
        val points = listOf(GeoPoint(-43.250,-65.310),GeoPoint(-43.250,-65.309))
        val plan = PaceNoteEngine.createPlan(points)
        assertTrue(DemoRoutePolicy.canStart(plan))
        assertTrue(plan.totalMetres > 25.0)
    }
}
