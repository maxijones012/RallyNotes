package ar.com.rallygpsai

/**
 * State machine for safe, bounded route recovery.
 * Requires repeated, sufficiently accurate GPS fixes off the geometry before requesting
 * a new road route. Service requests are rate limited and must never run on the UI thread.
 */
class ReroutePolicy(
    private val thresholdM: Double = 85.0,
    private val minConfirmedFixes: Int = 3,
    private val minimumDeviationMs: Long = 3_000L,
    private val retryCooldownMs: Long = 50_000L
) {
    private var count = 0
    private var firstDeviationMs = 0L
    private var lastAttemptMs = Long.MIN_VALUE
    private var waiting = false

    fun reset() {
        count = 0
        firstDeviationMs = 0L
        lastAttemptMs = Long.MIN_VALUE
        waiting = false
    }

    fun observe(offRoute: Boolean, separationM: Double, accuracyM: Float, nowMs: Long): Boolean {
        if (waiting) return false
        if (!offRoute || separationM < thresholdM || accuracyM > 30f || !separationM.isFinite()) {
            count = 0
            firstDeviationMs = 0L
            return false
        }
        if (count == 0) firstDeviationMs = nowMs
        count++
        if (count < minConfirmedFixes || nowMs - firstDeviationMs < minimumDeviationMs) return false
        if (lastAttemptMs != Long.MIN_VALUE && nowMs - lastAttemptMs < retryCooldownMs) return false
        lastAttemptMs = nowMs
        waiting = true
        count = 0
        firstDeviationMs = 0L
        return true
    }

    fun completeAttempt() { waiting = false }
    fun isBusy(): Boolean = waiting
}
