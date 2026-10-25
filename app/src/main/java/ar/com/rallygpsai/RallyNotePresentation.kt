package ar.com.rallygpsai

import kotlin.math.roundToInt

/** Presentation only. Pace-note geometry remains the source of truth.
 * Every badge is an estimate based on the mapped centreline, not road conditions.
 */
data class RallyNoteCardData(
    val headline: String,
    val distanceLabel: String,
    val featuresLabel: String,
    val grade: Int,
    val side: PaceNote.Side,
    val chained: PaceNote?,
    val lowConfidence: Boolean,
)

object RallyNotePresentation {
    fun render(note: PaceNote, following: PaceNote?, distanceM: Double): RallyNoteCardData {
        val distance = distanceM.coerceAtLeast(0.0)
        val distanceText = if (distance < 20.0) "AHORA" else "EN ${((distance / 10.0).roundToInt() * 10)} M"
        val details = mutableListOf<String>()
        if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) details += "HORQUILLA"
        if (note.endMetres - note.metresFromStart >= 95.0 ||
            (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) details += "LARGA"
        note.tightensTo?.let { if (it < note.grade) details += "CIERRA A $it" }
        note.opensTo?.let { if (it > note.grade) details += "ABRE A $it" }
        val chaining = following?.takeIf { it.metresFromStart - note.endMetres in 0.0..85.0 }
        return RallyNoteCardData(
            headline = if (note.side == PaceNote.Side.LEFT) "IZQUIERDA" else "DERECHA",
            distanceLabel = distanceText,
            featuresLabel = details.joinToString("  ·  ").ifBlank { "MANTENÉ EL TRAZADO" },
            grade = note.grade.coerceIn(1, 6),
            side = note.side,
            chained = chaining,
            lowConfidence = note.confidence < 0.7,
        )
    }
}
