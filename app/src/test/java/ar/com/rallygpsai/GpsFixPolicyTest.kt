package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class GpsFixPolicyTest {
    @Test fun validAndInvalidAccuracy() {
        assertTrue(GpsFixPolicy.usable(-43.25,-65.31,12f,1000))
        assertFalse(GpsFixPolicy.usable(-43.25,-65.31,null,1000))
        assertFalse(GpsFixPolicy.usable(-43.25,-65.31,68f,1000))
        assertFalse(GpsFixPolicy.usable(Double.NaN,-65.31,4f,1000))
        assertFalse(GpsFixPolicy.usable(-43.25,-65.31,3f,18000))
    }
    @Test fun invalidSpeedDoesNotTriggerFalseNavigationAdvance() {
        assertEquals(0f,GpsFixPolicy.safeSpeed(200f,true),0.001f)
        assertEquals(0f,GpsFixPolicy.safeSpeed(null,false),0.001f)
        assertEquals(12f,GpsFixPolicy.safeSpeed(12f,true),0.001f)
    }
}
