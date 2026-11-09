package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RoadRouteCheckerTest {
    private val a=GeoPoint(-43.25,-65.31)
    private val b=GeoPoint(-43.25,-65.305)
    private val c=GeoPoint(-43.248,-65.305)
    private val route=listOf(a,b,c)
    @Test fun plausibleOsrmRoutePasses() {
        val m=RoadRouteChecker.inspect(listOf(a,c),route,listOf(a,c),610.0)
        assertFalse(m.rejected)
        assertFalse(m.reviewRequired)
        assertTrue(m.totalMetres>500)
    }
    @Test fun inaccurateDestinationRequiresReview() {
        val offset=GeoPoint(-43.249, -65.305)
        val m=RoadRouteChecker.inspect(listOf(a,offset),route,listOf(a,c),610.0)
        assertFalse(m.rejected)
        assertTrue(m.reviewRequired)
        assertTrue(m.maximumSnapMetres>120.0)
    }
    @Test fun farSnapAndImpossibleRoadAreRejected() {
        val distant=GeoPoint(-43.22,-65.305)
        assertTrue(RoadRouteChecker.inspect(listOf(a,distant),route,listOf(a,c),610.0).rejected)
        assertTrue(RoadRouteChecker.inspect(listOf(a,c),listOf(a),listOf(a,c),610.0).rejected)
        assertTrue(RoadRouteChecker.inspect(listOf(a,c),route,listOf(a,c),Double.NaN).rejected)
    }
    @Test fun invalidCoordinatesAndDiscontinuitiesFailClosed() {
        val invalid=GeoPoint(Double.NaN,-65.31)
        assertTrue(RoadRouteChecker.inspect(listOf(a,c),listOf(a,invalid,c),listOf(a,c),500.0).rejected)
        val remote=GeoPoint(-43.25,-64.8)
        assertTrue(RoadRouteChecker.inspect(listOf(a,remote),listOf(a,remote),listOf(a,remote),40000.0).rejected)
    }
}
