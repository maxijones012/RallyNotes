package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RealTripRecorderTest {
    private val start = GeoPoint(-43.2500, -65.3100)
    private val moved = GeoPoint(-43.2500, -65.3085)
    @Test fun realGpsProducesDistanceAverageAndMax() {
        val recorder = RealTripRecorder(1_000_000L, 0L, listOf(start,moved))
        recorder.accept(start, 12f, 5f, 0)
        recorder.accept(GeoPoint(-43.2500,-65.3095), 14f, 6f, 5000)
        recorder.accept(moved, 15f, 6f, 10000)
        val trip = recorder.finish(20000)
        assertNotNull(trip)
        assertTrue(trip!!.distanceM > 80)
        assertEquals(54.0,trip.maximumKmh,0.01)
        assertTrue(trip.averageKmh in 12.0..70.0)
        assertEquals(2,trip.route.size)
    }

    @Test fun badGpsAndShortDriveCannotCreateStats() {
        val recorder = RealTripRecorder(2_000_000L,0L,listOf(start,moved))
        recorder.accept(start, 10f, 60f, 0)
        recorder.accept(moved, 200f, 4f, 3000)
        assertNull(recorder.finish(5000))
    }

    @Test fun grossGpsJumpIsRejected() {
        val recorder = RealTripRecorder(1_000L,0L,listOf(start,moved))
        recorder.accept(start, 10f, 5f, 0)
        recorder.accept(GeoPoint(-43.26,-65.27), 10f, 5f, 1000)
        recorder.accept(moved, 1f, 5f, 2000)
        assertNull(recorder.finish(20000))
    }
}
