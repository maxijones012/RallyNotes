package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class EditablePacenotesTest {
    private val notes = listOf(
        PaceNote(100.0,PaceNote.Side.RIGHT,3,70,50.0,145.0),
        PaceNote(240.0,PaceNote.Side.LEFT,4,75,60.0,280.0),
        PaceNote(410.0,PaceNote.Side.RIGHT,5,50,110.0,450.0)
    )
    private val base = RoutePlan(
        listOf(GeoPoint(-43.0,-65.0), GeoPoint(-43.0,-64.994)),
        listOf(0.0, 550.0), notes
    )
    private fun edit(index: Int=1, metre: Double=248.0, grade: Int=2) =
        NoteCorrection(index, notes[index].metresFromStart, metre,
            PaceNote.Side.RIGHT, grade, 1, null, true)

    @Test fun validEditUpdatesDirectionGradeAndPositionButNeverRoadGeometry() {
        val originalRoute = base.points
        val changed = EditablePacenotes.apply(base,listOf(edit()))
        assertSame(originalRoute, changed.points)
        assertEquals(248.0,changed.notes[1].metresFromStart,0.01)
        assertEquals(288.0,changed.notes[1].endMetres,0.01)
        assertEquals(2,changed.notes[1].grade)
        assertEquals(PaceNote.Side.RIGHT,changed.notes[1].side)
        assertTrue(changed.notes[1].manuallyLong == true)
        assertEquals(1.0,changed.notes[1].confidence,0.001)
        assertEquals(240.0,base.notes[1].metresFromStart,0.01)
    }

    @Test fun staleAndUnsafeEditsAreDiscarded() {
        assertNotNull(EditablePacenotes.validate(base, edit(metre=500.0)))
        assertNotNull(EditablePacenotes.validate(base, edit(metre=99.0)))
        assertNotNull(EditablePacenotes.validate(base, edit(grade=7)))
        val invalid = edit().copy(originalMetres=120.0)
        assertEquals(base.notes,EditablePacenotes.apply(base,listOf(invalid)).notes)
    }

    @Test fun routeFingerprintChangesWithGeometryAndIsRepeatable() {
        val key=EditablePacenotes.fingerprint(base.points)
        assertEquals(key,EditablePacenotes.fingerprint(base.points.toList()))
        val different=base.points.toMutableList().apply { set(1, GeoPoint(-43.1,-64.994)) }
        assertNotEquals(key,EditablePacenotes.fingerprint(different))
        assertEquals(64,key.length)
    }

    @Test fun correctedLongModifierAppearsInBothVisualAndSpokenNotes() {
        val changed = EditablePacenotes.apply(base,listOf(edit()))
        assertTrue(PaceNotes.phrase(changed.notes[1]).contains("larga"))
        assertTrue(RallyNotePresentation.render(changed.notes[1],null,100.0).featuresLabel.contains("LARGA"))
    }
}
