package ar.com.rallygpsai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for off-route recovery before delivering the 0.2.3 APK. */
class RerouteRecoveryTest {
    @Test fun recalculatesOnlyAfterConsistentAccurateFixes() {
        val policy = ReroutePolicy()
        assertFalse(policy.observe(true, 160.0, 55f, 1_000))
        assertFalse(policy.observe(true, 160.0, 7f, 2_000))
        assertFalse(policy.observe(true, 160.0, 7f, 3_000))
        assertTrue(policy.observe(true, 160.0, 7f, 5_500))
        assertTrue(policy.isBusy())
        assertFalse(policy.observe(true, 160.0, 7f, 7_000))
        policy.completeAttempt()
        assertFalse(policy.isBusy())
    }

    @Test fun returningToRoadResetsStreakAndCooldownPreventsFlooding() {
        val policy = ReroutePolicy()
        assertFalse(policy.observe(true, 130.0, 6f, 1_000))
        assertFalse(policy.observe(true, 130.0, 6f, 2_000))
        assertFalse(policy.observe(false, 0.0, 6f, 2_100))
        assertFalse(policy.observe(true, 130.0, 6f, 3_000))
        assertFalse(policy.observe(true, 130.0, 6f, 5_000))
        assertTrue(policy.observe(true, 130.0, 6f, 6_200))
        policy.completeAttempt()
        assertFalse(policy.observe(true, 130.0, 6f, 9_000))
        assertFalse(policy.observe(true, 130.0, 6f, 10_100))
        assertFalse(policy.observe(true, 130.0, 6f, 12_500))
        assertFalse(policy.observe(true, 130.0, 6f, 55_000))
        assertFalse(policy.observe(true, 130.0, 6f, 56_000))
        assertTrue(policy.observe(true, 130.0, 6f, 58_200))
    }
}
