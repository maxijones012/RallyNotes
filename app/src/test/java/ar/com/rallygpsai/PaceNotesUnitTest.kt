package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class PaceNotesUnitTest {
    private val origin = GeoPoint(-43.25, -65.31)

    /** Closed-loop test geometry only. Never shipped in the Android app. */
    private fun fixtureRoute(): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var x = 0.0
        var y = 0.0
        var heading = -Math.PI / 2.0
        for (i in 0..180) {
            val curve = when (i) {
                in 25..44 -> Math.PI / 2.0 / 20.0
                in 66..84 -> -Math.PI * 0.7 / 19.0
                in 110..121 -> Math.PI / 2.0 / 12.0
                in 143..166 -> -Math.PI / 2.0 / 24.0
                else -> 0.0
            }
            heading += curve
            x += kotlin.math.cos(heading) * 9.0
            y += kotlin.math.sin(heading) * 9.0
            points += GeoPoint(origin.latitude + y / 111120.0,
                origin.longitude + x / (111120.0 * kotlin.math.cos(Math.toRadians(origin.latitude))))
        }
        return points
    }


    @Test fun straightHasNoFalsePacenotes() {
        val plan = PaceNoteEngine.createPlan((0..100).map { GeoPoint(-43.25, -65.31 + it*0.00002) })
        assertTrue(plan.notes.isEmpty())
    }

    @Test fun syntheticContainsNotesBothWaysAndConsistentGrades() {
        val plan = PaceNoteEngine.createPlan(fixtureRoute())
        assertTrue(plan.notes.size >= 3)
        assertTrue(plan.notes.any { it.side == PaceNote.Side.LEFT })
        assertTrue(plan.notes.any { it.side == PaceNote.Side.RIGHT })
        assertTrue(plan.notes.all { it.grade in 1..6 })
        assertTrue(plan.notes.zipWithNext().all { (a,b) -> a.metresFromStart < b.metresFromStart })
    }

    @Test fun navigatorDoesNotAnnounceOffRoute() {
        val navigator = NavigationEngine(PaceNoteEngine.createPlan(fixtureRoute()))
        val guidance = navigator.update(GeoPoint(origin.latitude + 0.05, origin.longitude), 15f, 8f)
        assertTrue(guidance.offRoute)
        assertNull(guidance.announcement)
    }

    @Test fun validRouteProducesAtMostTwoStagedAnnouncementsPerNote() {
        val plan = PaceNoteEngine.createPlan(fixtureRoute())
        val navigator = NavigationEngine(plan)
        val announcements = plan.points.mapNotNull { navigator.update(it, 15f).announcement }
        assertTrue(announcements.isNotEmpty())
        assertTrue(announcements.size <= plan.notes.size * 2 + 1)
    }

    @Test fun speedControlsAnticipation() {
        assertTrue(PaceNotes.warningDistance(27f, 2) > PaceNotes.warningDistance(27f, 0))
        assertEquals("#F0475A", PaceNotes.color(1))
    }
}
