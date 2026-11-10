package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyMapStyleTest {
    @Test fun stylePreferencesAlwaysResolveToKnownSecureStyle() {
        assertEquals(RallyMapStyle.DARK,RallyMapStyle.fromStored(null))
        assertEquals(RallyMapStyle.DARK,RallyMapStyle.fromStored("satellite"))
        assertEquals(RallyMapStyle.DAY,RallyMapStyle.fromStored("day"))
        assertTrue(RallyMapStyle.entries.all { it.url.startsWith("https://") })
    }
}
