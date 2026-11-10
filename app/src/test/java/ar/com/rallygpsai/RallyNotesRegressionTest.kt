package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyNotesRegressionTest {
    @Test fun directionAndGradeRemainIntact() {
        for (grade in 1..6) {
            val n=PaceNote(250.0,PaceNote.Side.RIGHT,grade,50,60.0,300.0)
            val rendered=RallyNotePresentation.render(n,null,200.0)
            assertEquals("DERECHA",rendered.headline)
            assertEquals(grade,rendered.grade)
            assertEquals(PaceNote.Side.RIGHT,rendered.side)
            assertEquals("200",rendered.distanceValue)
        }
        val left=PaceNote(250.0,PaceNote.Side.LEFT,5,60,85.0,340.0)
        assertEquals("IZQUIERDA",RallyNotePresentation.render(left,null,80.0).headline)
    }
    @Test fun chainedNotesDoNotHideEstimatedStatus() {
        val first=PaceNote(150.0,PaceNote.Side.LEFT,3,75,60.0,175.0)
        val second=PaceNote(210.0,PaceNote.Side.RIGHT,4,60,90.0,270.0)
        val rendering=RallyNotePresentation.render(first,second,120.0)
        assertEquals(second,rendering.chained)
        assertTrue(rendering.lowConfidence)
    }
    @Test fun longCurveAndTightensModifiersStillWork() {
        val long=PaceNote(100.0,PaceNote.Side.LEFT,4,100,80.0,220.0,
            tightensTo=2,confidence=0.8,manuallyLong=true)
        val rendered=RallyNotePresentation.render(long,null,50.0)
        assertTrue(rendered.featuresLabel.contains("LARGA"))
        assertTrue(rendered.featuresLabel.contains("CIERRA A 2"))
    }
}
