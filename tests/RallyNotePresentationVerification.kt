import ar.com.rallygpsai.*

fun main() {
    var assertions = 0
    fun verify(value: Boolean, name: String) { check(value) { name }; assertions++ }
    for (grade in 1..6) for (side in PaceNote.Side.values()) for (distance in listOf(-5.0, 0.0, 12.0, 29.0, 49.0, 95.0, 210.0)) {
        val note = PaceNote(100.0,side,grade,80,60.0,160.0,confidence=0.6)
        val next = PaceNote(199.0, PaceNote.Side.LEFT,3,70,60.0,250.0)
        val result = RallyNotePresentation.render(note,next,distance)
        verify(result.headline == if(side == PaceNote.Side.LEFT) "IZQUIERDA" else "DERECHA", "direction")
        verify(result.grade == grade, "grade")
        verify(result.distanceLabel.isNotBlank(), "distance")
        verify(result.chained == next, "linked")
        verify(result.lowConfidence, "confidence")
    }
    verify(RallyNotePresentation.render(PaceNote(100.0,PaceNote.Side.RIGHT,5,90,120.0,180.0,tightensTo=3),null,50.0).featuresLabel.contains("CIERRA A 3"),"tightens")
    verify(RallyNotePresentation.render(PaceNote(100.0,PaceNote.Side.RIGHT,5,90,120.0,180.0,opensTo=6),null,50.0).featuresLabel.contains("ABRE A 6"),"opens")
    verify(RallyNotePresentation.render(PaceNote(100.0,PaceNote.Side.RIGHT,5,90,120.0,180.0),PaceNote(400.0,PaceNote.Side.LEFT,4,100,80.0),50.0).chained==null,"not linked")
    println("PACENOTE UI: $assertions verificaciones OK")
}
