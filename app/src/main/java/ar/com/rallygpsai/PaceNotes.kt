package ar.com.rallygpsai

import kotlin.math.*

object PaceNotes {
    fun phrase(note: PaceNote, next: PaceNote? = null): String {
        val side = if (note.side == PaceNote.Side.LEFT) "izquierda" else "derecha"
        val curve = if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) "horquilla " else ""
        val long = if (note.cumulativeTurnDegrees >= 95 && note.grade >= 3) ", larga" else ""
        val join = if (next != null && next.metresFromStart - note.metresFromStart < 85) {
            ", enlaza ${if (next.side == PaceNote.Side.LEFT) "izquierda" else "derecha"} ${next.grade}"
        } else ""
        return "$curve$side ${note.grade}$long$join"
    }

    fun warningDistance(speedMs: Float, factor: Int): Double {
        val seconds = when(factor) { 0 -> 4.0; 2 -> 10.0; else -> 7.0 }
        return max(60.0, speedMs.coerceAtLeast(0f) * seconds.toFloat().toDouble()).coerceAtMost(350.0)
    }

    fun color(grade: Int): String = when(grade) {
        1 -> "#E94C58"; 2 -> "#FF7752"; 3 -> "#FFAF47"
        4 -> "#FAD247"; 5 -> "#42D9AC"; else -> "#4CCBED"
    }

    /** Example-only simulated route. NOT verified OSM road geometry: never navigate it in a vehicle. */
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
            x += cos(heading) * 9.0
            y += sin(heading) * 9.0
            route.add(GeoPoint(origin.latitude + y / 111120.0, origin.longitude + x / (111120.0 * cos(Math.toRadians(origin.latitude)))))
        }
        return route
    }
}
