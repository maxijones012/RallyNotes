package ar.com.rallygpsai

import kotlin.math.max

/** An optional, offline, rule-based co-driver. No AI model, network, camera or sensors. */
enum class ContextMode(val id: String, val cooldownMs: Long) {
    OFF("off", Long.MAX_VALUE),
    OCCASIONAL("occasional", 240_000L),
    TALKATIVE("talkative", 105_000L);

    companion object {
        fun fromId(id: String?): ContextMode = entries.firstOrNull { it.id == id } ?: OCCASIONAL
    }
}

enum class ContextKind { WINDING, LINKED, STRAIGHT }

data class ContextCue(val kind: ContextKind, val featureId: String)

/**
 * Looks only at estimated pace notes derived from route shape.
 * Does not infer traffic, road surface, weather, safe speed or actual hazards.
 */
class ContextCopilot {
    private var startedMs: Long? = null
    private var lastCueMs: Long? = null
    private val usedFeatures = mutableSetOf<String>()

    fun reset() {
        startedMs = null
        lastCueMs = null
        usedFeatures.clear()
    }

    /**
     * Invoke with monotonic elapsed-realtime milliseconds.
     * priorityBusy means a turn is being spoken or a voice is still playing.
     */
    fun evaluate(
        route: RoutePlan,
        progressM: Double,
        speedMps: Double,
        nowMs: Long,
        mode: ContextMode,
        nextTurnM: Double,
        priorityBusy: Boolean = false,
    ): ContextCue? {
        if (mode == ContextMode.OFF || priorityBusy) return null
        if (!progressM.isFinite() || !speedMps.isFinite() || speedMps < 3.0) return null
        if (route.totalMetres < 600 || route.notes.isEmpty()) return null
        val start = startedMs ?: run { startedMs = nowMs; return null }
        if (nowMs < start || nowMs - start < 30_000L) return null
        if (lastCueMs != null && nowMs - lastCueMs!! < mode.cooldownMs) return null
        val distance = progressM.coerceIn(0.0, route.totalMetres)
        val remaining = route.totalMetres - distance
        if (remaining < 500.0) return null

        // Only speak in a meaningful gap; never alongside an upcoming actual pace note.
        val safeGap = max(320.0, speedMps * 25.0)
        if (nextTurnM.isNaN() || nextTurnM < safeGap) return null

        val future = route.notes.filter {
            it.metresFromStart >= distance + 220.0 &&
                it.metresFromStart <= distance + 1_250.0
        }
        val linkedPair = future.zipWithNext().firstOrNull { (a,b) ->
            b.metresFromStart - a.endMetres in 0.0..85.0 && a.side != b.side
        }
        val alternating = future.zipWithNext().count { (a,b) -> a.side != b.side }
        val recent = route.notes.filter { it.endMetres in (distance - 300.0)..(distance - 70.0) }
        val nextReal = route.notes.firstOrNull { it.metresFromStart >= distance - 10.0 }
        val nextRealGap = nextReal?.let { it.metresFromStart - distance } ?: Double.POSITIVE_INFINITY

        val cue = when {
            future.size >= 4 || (future.size >= 3 && alternating >= 2) -> {
                ContextCue(ContextKind.WINDING, "W:" + ((future.first().metresFromStart / 300).toInt()))
            }
            linkedPair != null -> {
                ContextCue(ContextKind.LINKED, "L:" + ((linkedPair.first.metresFromStart / 300).toInt()))
            }
            recent.isNotEmpty() && nextRealGap >= 800.0 -> {
                ContextCue(ContextKind.STRAIGHT, "S:" + ((recent.last().metresFromStart / 300).toInt()))
            }
            else -> null
        } ?: return null

        if (!usedFeatures.add(cue.featureId)) return null
        if (usedFeatures.size > 200) usedFeatures.clear().also { usedFeatures.add(cue.featureId) }
        lastCueMs = nowMs
        return cue
    }
}
