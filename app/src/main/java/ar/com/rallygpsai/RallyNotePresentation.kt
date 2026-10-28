package ar.com.rallygpsai

import kotlin.math.roundToInt
import java.util.Locale

/** Presentation only. Pace-note geometry remains the source of truth.
 * Every badge is an estimate based on the mapped centreline, not road conditions.
 */
data class RallyNoteCardData(
    val headline: String,
    val distanceLabel: String,
    val distanceValue: String,
    val distanceUnit: String,
    val featuresLabel: String,
    val grade: Int,
    val side: PaceNote.Side,
    val chained: PaceNote?,
    val lowConfidence: Boolean,
)

object RallyDistanceFormatter {
    data class Value(val number: String, val unit: String, val label: String)
    fun format(metres: Double): Value {
        val distance = if (metres.isFinite()) metres.coerceAtLeast(0.0) else 0.0
        if (distance < 20.0) return Value("AHORA", "", "AHORA")
        if (distance >= 995.0) {
            val km = distance / 1000.0
            val value = String.format(Locale("es", "AR"), "%.1f", km)
            return Value(value, "km", "$value km")
        }
        val step = if (distance < 100) 5 else 10
        val metresRounded = ((distance / step).roundToInt() * step).coerceAtLeast(step)
        return Value(metresRounded.toString(), "m", "$metresRounded m")
    }
    fun remaining(distanceMetres: Double): String {
        val m = if (distanceMetres.isFinite()) distanceMetres.coerceAtLeast(0.0) else 0.0
        return if (m >= 1000) String.format(Locale("es", "AR"), "%.1f km", m / 1000.0)
            else "${m.roundToInt()} m"
    }
}

object RallyNotePresentation {
    fun render(note: PaceNote, following: PaceNote?, distanceM: Double): RallyNoteCardData {
        val distance = distanceM.coerceAtLeast(0.0)
        val distanceDisplay = RallyDistanceFormatter.format(distance)
        val details = mutableListOf<String>()
        if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) details += "HORQUILLA"
        if (note.endMetres - note.metresFromStart >= 95.0 ||
            (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) details += "LARGA"
        note.tightensTo?.let { if (it < note.grade) details += "CIERRA A $it" }
        note.opensTo?.let { if (it > note.grade) details += "ABRE A $it" }
        val chaining = following?.takeIf { it.metresFromStart - note.endMetres in 0.0..85.0 }
        return RallyNoteCardData(
            headline = if (note.side == PaceNote.Side.LEFT) "IZQUIERDA" else "DERECHA",
            distanceLabel = distanceDisplay.label,
            distanceValue = distanceDisplay.number,
            distanceUnit = distanceDisplay.unit,
            featuresLabel = details.joinToString("  ·  ").ifBlank { "NOTA ESTIMADA" },
            grade = note.grade.coerceIn(1, 6),
            side = note.side,
            chained = chaining,
            lowConfidence = note.confidence < 0.7,
        )
    }
}
