package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class ReroutePolicyUnitTest {
    @Test fun repeatedGpsFixesRequired() {
        val policy = ReroutePolicy()
        assertFalse(policy.observe(true, 100.0, 10f, 1_000))
        assertFalse(policy.observe(true, 110.0, 10f, 2_000))
        assertTrue(policy.observe(true, 120.0, 10f, 4_500))
        assertTrue(policy.isBusy())
    }

    @Test fun noRerouteForBadGpsAndShortDrift() {
        val policy = ReroutePolicy()
        assertFalse(policy.observe(true, 300.0, 80f, 1_000))
        assertFalse(policy.observe(true, 70.0, 5f, 2_000))
        assertFalse(policy.observe(false, 0.0, 5f, 3_000))
        assertFalse(policy.isBusy())
    }

    @Test fun cooldownPreventsFrequentServerRequests() {
        val policy = ReroutePolicy()
        policy.observe(true, 100.0, 5f, 0)
        policy.observe(true, 100.0, 5f, 1_500)
        assertTrue(policy.observe(true, 100.0, 5f, 4_500))
        policy.completeAttempt()
        policy.observe(true, 100.0, 5f, 8_000)
        policy.observe(true, 100.0, 5f, 10_000)
        assertFalse(policy.observe(true, 100.0, 5f, 12_000))
        policy.observe(false, 0.0, 5f, 12_500)
        policy.observe(true, 100.0, 5f, 55_000)
        policy.observe(true, 100.0, 5f, 56_000)
        assertTrue(policy.observe(true, 100.0, 5f, 59_000))
    }
}
