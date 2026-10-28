package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class PaceNotesUnitTest {
    private val origin = GeoPoint(-43.25, -65.31)

    @Test fun straightHasNoFalsePacenotes() {
        val plan = PaceNoteEngine.createPlan((0..100).map { GeoPoint(-43.25, -65.31 + it*0.00002) })
        assertTrue(plan.notes.isEmpty())
    }

    @Test fun syntheticContainsNotesBothWaysAndConsistentGrades() {
        val plan = PaceNoteEngine.createPlan(PaceNotes.synthetic(origin))
        assertTrue(plan.notes.size >= 3)
        assertTrue(plan.notes.any { it.side == PaceNote.Side.LEFT })
        assertTrue(plan.notes.any { it.side == PaceNote.Side.RIGHT })
        assertTrue(plan.notes.all { it.grade in 1..6 })
        assertTrue(plan.notes.zipWithNext().all { (a,b) -> a.metresFromStart < b.metresFromStart })
    }

    @Test fun navigatorDoesNotAnnounceOffRoute() {
        val navigator = NavigationEngine(PaceNoteEngine.createPlan(PaceNotes.synthetic(origin)))
        val guidance = navigator.update(GeoPoint(origin.latitude + 0.05, origin.longitude), 15f, 8f)
        assertTrue(guidance.offRoute)
        assertNull(guidance.announcement)
    }

    @Test fun validRouteProducesAtMostTwoStagedAnnouncementsPerNote() {
        val plan = PaceNoteEngine.createPlan(PaceNotes.synthetic(origin))
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
