package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyNotePresentationTest {
    private fun note(side: PaceNote.Side = PaceNote.Side.RIGHT, grade: Int = 4, start: Double = 100.0,
                     end: Double = 160.0) = PaceNote(start, side, grade, 85, 95.0, end, confidence = 0.75)

    @Test fun leftAndRightHaveCorrectTextAndGrade() {
        val left = RallyNotePresentation.render(note(PaceNote.Side.LEFT, 2), null, 112.0)
        val right = RallyNotePresentation.render(note(PaceNote.Side.RIGHT, 5), null, 112.0)
        assertEquals("IZQUIERDA", left.headline)
        assertEquals(2, left.grade)
        assertEquals("DERECHA", right.headline)
        assertEquals(5, right.grade)
    }

    @Test fun distanceIsRoundedAndDoesNotGoNegative() {
        assertEquals("120 m", RallyNotePresentation.render(note(), null, 117.0).distanceLabel)
        val ahead = RallyNotePresentation.render(note(), null, 1_880.0)
        assertEquals("1,9", ahead.distanceValue)
        assertEquals("km", ahead.distanceUnit)
        val close = RallyNotePresentation.render(note(), null, 82.0)
        assertEquals("80", close.distanceValue)
        assertEquals("m", close.distanceUnit)
        assertEquals("AHORA", RallyNotePresentation.render(note(), null, -12.0).distanceLabel)
        assertEquals("AHORA", RallyNotePresentation.render(note(), null, 18.0).distanceLabel)
    }

    @Test fun linkedTurnOnlyAppearsWhenActuallyLinked() {
        val first = note(end = 150.0)
        val linked = note(PaceNote.Side.LEFT, 3, start = 210.0)
        val far = note(PaceNote.Side.LEFT, 3, start = 400.0)
        assertEquals(linked, RallyNotePresentation.render(first, linked, 60.0).chained)
        assertNull(RallyNotePresentation.render(first, far, 60.0).chained)
    }

    @Test fun tighteningAndLongAppearWithoutInventingWarnings() {
        val first = note(grade = 5, end = 240.0).copy(tightensTo=3)
        val ui = RallyNotePresentation.render(first, null, 50.0)
        assertTrue(ui.featuresLabel.contains("LARGA"))
        assertTrue(ui.featuresLabel.contains("CIERRA A 3"))
        assertFalse(ui.featuresLabel.contains("SALTO"))
    }

    @Test fun lowConfidenceIsMarked() {
        val ui = RallyNotePresentation.render(note().copy(confidence = 0.5), null, 30.0)
        assertTrue(ui.lowConfidence)
    }
}
