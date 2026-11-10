package ar.com.rallygpsai

/** Spoken obstacle alerts must yield to a rally note or busy voice engine. */
object RallySafetySpeech {
    fun mayAlert(
        distanceM: Double, speedMps: Double, bendDistanceM: Double,
        priorityNote: Boolean, audioBusy: Boolean, timeSincePriorityMs: Long
    ): Boolean = distanceM.isFinite() && distanceM in 45.0..160.0 &&
        speedMps.isFinite() && speedMps >= 2.0 &&
        !bendDistanceM.isNaN() && bendDistanceM >= 250.0 &&
        !priorityNote && !audioBusy && timeSincePriorityMs >= 15_000L
}
