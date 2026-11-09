package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RollingNotesTest {
    private val points = listOf(GeoPoint(-43.0, -65.0), GeoPoint(-43.0, -64.99))
    private fun n(m: Double, side: PaceNote.Side) =
        PaceNote(m, side, 4, 60, 80.0, m + 30)
    private val notes = listOf(n(100.0, PaceNote.Side.RIGHT),
        n(180.0, PaceNote.Side.LEFT), n(400.0, PaceNote.Side.RIGHT))
    private val route = RoutePlan(points, listOf(0.0, 900.0), notes)

    @Test fun queueInitiallyShowsCurrentNextAndFollowing() {
        val w = RollingNotes.at(route, 0, 0.0)
        assertEquals(100.0, w.metresToCurrent, 0.01)
        assertEquals(180.0, w.metresToNext, 0.01)
        assertEquals(notes[0], w.current)
        assertEquals(notes[1], w.next)
        assertEquals(notes[2], w.afterNext)
        assertTrue(w.linked)
    }

    @Test fun crossingFirstBendAdvancesTheQueue() {
        val w = RollingNotes.at(route, 1, 115.0)
        assertEquals(notes[1], w.current)
        assertEquals(notes[2], w.next)
        assertNull(w.afterNext)
        assertFalse(w.linked)
        assertEquals(65.0, w.metresToCurrent, 0.01)
    }

    @Test fun finalBendAndEndHaveNoPhantomNotes() {
        assertNull(RollingNotes.at(route, 2, 410.0).next)
        assertNull(RollingNotes.at(route, 3, 900.0).current)
    }
}
