package ar.com.rallygpsai

/** The route progress is the only source of truth for the rolling pace-note queue. */
data class RollingNoteWindow(
    val current: PaceNote?,
    val next: PaceNote?,
    val afterNext: PaceNote?,
    val metresToCurrent: Double,
    val metresToNext: Double,
    val linked: Boolean
)

object RollingNotes {
    fun at(route: RoutePlan, noteIndex: Int, progressM: Double): RollingNoteWindow {
        val index = noteIndex.coerceAtLeast(0)
        val current = route.notes.getOrNull(index)
        val next = route.notes.getOrNull(index + 1)
        val afterNext = route.notes.getOrNull(index + 2)
        val safeProgress = if (progressM.isFinite()) progressM.coerceAtLeast(0.0) else 0.0
        return RollingNoteWindow(
            current = current,
            next = if (current != null) next else null,
            afterNext = if (current != null) afterNext else null,
            metresToCurrent = (current?.metresFromStart?.minus(safeProgress) ?: 0.0).coerceAtLeast(0.0),
            metresToNext = (next?.metresFromStart?.minus(safeProgress) ?: 0.0).coerceAtLeast(0.0),
            linked = current != null && next != null &&
                next.metresFromStart - current.endMetres in 0.0..85.0
        )
    }
}
