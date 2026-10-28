package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos

class LongDistanceGuidanceTest {
    private fun straightPlan(): RoutePlan {
        val lat = -43.0
        val metresPerLon = 111_120.0 * cos(Math.toRadians(lat))
        val points = (0..50).map { GeoPoint(lat, -65.0 + (it * 40.0) / metresPerLon) }
        val metres = (0..50).map { it * 40.0 }
        val note = PaceNote(920.0, PaceNote.Side.RIGHT, 3, 82, 95.0, 980.0)
        return RoutePlan(points, metres, listOf(note))
    }

    @Test fun earlyWarningAndCloserWarningAreSpokenOnceEach() {
        val plan = straightPlan()
        val nav = NavigationEngine(plan) { 1 }
        var early = 0
        var close = 0
        for (i in 0..22) {
            val g = nav.update(plan.points[i], 15f, 5f)
            assertFalse("GPS on traced road", g.offRoute)
            if (g.announcement != null) {
                if (g.distanceToNoteM > 200) {
                    early++
                    assertTrue(g.announcement.contains("800 metros") || g.announcement.contains("700 metros"))
                } else {
                    close++
                    assertTrue(g.announcement.contains("derecha 3"))
                }
            }
        }
        assertEquals("one early announcement",1,early)
        assertEquals("one near bend announcement",1,close)
    }

    @Test fun spokenDistanceRoundingAvoidsIncorrectFourHundredMetreCap() {
        assertEquals(800,RallySpokenDistance.roundMetres(780.0))
        assertEquals(700,RallySpokenDistance.roundMetres(680.0))
        assertEquals(1000,RallySpokenDistance.roundMetres(995.0))
        assertEquals(200,RallySpokenDistance.roundMetres(195.0))
        assertEquals(0,RallySpokenDistance.roundMetres(18.0))
    }
}
