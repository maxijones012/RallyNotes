package ar.com.rallygpsai

import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.roundToLong

/** A user's deliberate correction; does not modify road geometry. */
data class NoteCorrection(
    val baseIndex: Int,
    val originalMetres: Double,
    val correctedMetres: Double,
    val side: PaceNote.Side,
    val grade: Int,
    val tightensTo: Int?,
    val opensTo: Int?,
    val longCurve: Boolean?
)

object EditablePacenotes {
    /** Same ordered route sampled to centimetre-independent 1m precision = same local edits. */
    fun fingerprint(points: List<GeoPoint>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buf = ByteBuffer.allocate(16)
        for (point in points) {
            buf.clear()
            buf.putLong((point.latitude * 100_000.0).roundToLong())
            buf.putLong((point.longitude * 100_000.0).roundToLong())
            digest.update(buf.array())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun validate(base: RoutePlan, correction: NoteCorrection): String? {
        val n = base.notes.getOrNull(correction.baseIndex) ?: return "La nota ya no existe"
        if (!correction.originalMetres.isFinite() ||
            abs(n.metresFromStart - correction.originalMetres) > 10.0)
            return "La corrección pertenece a otra ruta"
        if (correction.grade !in 1..6) return "Elegí una dificultad de 1 a 6"
        if (!correction.correctedMetres.isFinite() ||
            correction.correctedMetres < 0.0 || correction.correctedMetres > base.totalMetres)
            return "Posición fuera del recorrido"
        if (abs(correction.correctedMetres - n.metresFromStart) > 80.0)
            return "La nota solo se puede mover hasta 80 metros"
        if (correction.tightensTo != null && correction.tightensTo !in 1 until correction.grade)
            return "CIERRA A debe ser menor que la curva"
        if (correction.opensTo != null && correction.opensTo !in (correction.grade + 1)..6)
            return "ABRE A debe ser mayor que la curva"
        if (correction.tightensTo != null && correction.opensTo != null)
            return "Elegí CIERRA o ABRE, no ambos"
        val prev = base.notes.getOrNull(correction.baseIndex - 1)
        val after = base.notes.getOrNull(correction.baseIndex + 1)
        if (prev != null && correction.correctedMetres <= prev.metresFromStart + 5.0)
            return "La nota se superpone con la anterior"
        if (after != null && correction.correctedMetres >= after.metresFromStart - 5.0)
            return "La nota se superpone con la siguiente"
        return null
    }

    /** Invalid/stale edits never affect a regenerated route. */
    fun apply(base: RoutePlan, edits: List<NoteCorrection>): RoutePlan {
        val valid = edits.filter { validate(base, it) == null }.associateBy { it.baseIndex }
        val notes = base.notes.mapIndexed { i, original ->
            val edit = valid[i] ?: return@mapIndexed original
            val moved = edit.correctedMetres - original.metresFromStart
            original.copy(
                metresFromStart = edit.correctedMetres,
                endMetres = (original.endMetres + moved).coerceAtMost(base.totalMetres)
                    .coerceAtLeast(edit.correctedMetres),
                side = edit.side,
                grade = edit.grade,
                tightensTo = edit.tightensTo,
                opensTo = edit.opensTo,
                manuallyLong = edit.longCurve,
                confidence = 1.0
            )
        }
        return base.copy(notes = notes)
    }

    fun correction(base: RoutePlan, index: Int, edited: PaceNote): NoteCorrection {
        val original = base.notes[index]
        return NoteCorrection(index, original.metresFromStart, edited.metresFromStart,
            edited.side, edited.grade, edited.tightensTo, edited.opensTo, edited.manuallyLong)
    }
}
