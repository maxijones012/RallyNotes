package ar.com.rallygpsai

import kotlin.math.*

object PaceNotes {
    fun phrase(note: PaceNote, next: PaceNote? = null): String {
        val side = if (note.side == PaceNote.Side.LEFT) "izquierda" else "derecha"
        val curve = if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) "horquilla " else ""
        val extra = buildString {
            if (note.tightensTo != null && note.tightensTo < note.grade) append(", se cierra a ${note.tightensTo}")
            else if (note.opensTo != null && note.opensTo > note.grade) append(", abre a ${note.opensTo}")
            if (note.endMetres - note.metresFromStart >= 95.0 || (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) append(", larga")
            if (next != null && next.metresFromStart - note.endMetres in 0.0..85.0) {
                append(", enlaza ${if (next.side == PaceNote.Side.LEFT) "izquierda" else "derecha"} ${next.grade}")
            }
        }
        return "$curve$side ${note.grade}$extra"
    }

    fun warningDistance(speedMs: Float, factor: Int): Double {
        val seconds = when(factor) { 0 -> 4.0; 2 -> 10.0; else -> 7.0 }
        return max(65.0, speedMs.coerceAtLeast(0f).toDouble() * seconds).coerceAtMost(400.0)
    }

    fun color(grade: Int): String = when(grade) {
        1 -> "#F0475A"; 2 -> "#FE7850"; 3 -> "#FFAC43"
        4 -> "#F3D14A"; 5 -> "#43D9A6"; else -> "#46C8EA"
    }

    /** Solo demostración: no coincide con una calle o camino y jamás sirve para navegar. */
    fun synthetic(origin: GeoPoint): List<GeoPoint> {
        val route = ArrayList<GeoPoint>()
        var x = 0.0; var y = 0.0; var heading = -Math.PI / 2.0
        for (i in 0..180) {
            val curve = when (i) {
                in 25..44 -> Math.PI / 2.0 / 20.0
                in 66..84 -> -Math.PI * 0.7 / 19.0
                in 110..121 -> Math.PI / 2.0 / 12.0
                in 143..166 -> -Math.PI / 2.0 / 24.0
                else -> 0.0
            }
            heading += curve
            x += cos(heading) * 9.0; y += sin(heading) * 9.0
            route.add(GeoPoint(origin.latitude + y / 111120.0,
                origin.longitude + x / (111120.0 * cos(Math.toRadians(origin.latitude)))))
        }
        return route
    }
}
