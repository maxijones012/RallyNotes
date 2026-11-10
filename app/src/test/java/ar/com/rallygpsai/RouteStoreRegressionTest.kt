package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RouteStoreRegressionTest {
    @Test fun previousJsonRoutesStayReadableAfterUpdate() {
        val before=listOf(GeoPoint(-43.25,-65.31),GeoPoint(-43.251,-65.309),
            GeoPoint(-43.252,-65.308))
        val loaded=RouteStore.deserialize(RouteStore.serialize(before))
        assertEquals(before,loaded)
        assertEquals(before.last(),loaded.last())
    }
    @Test fun storedPointsDoNotChangeTheirDirection() {
        val before=listOf(GeoPoint(-43.25,-65.31),GeoPoint(-43.26,-65.305))
        val loaded=RouteStore.deserialize(RouteStore.serialize(before))
        assertEquals(before.first(),loaded.first())
        assertEquals(before.last(),loaded.last())
        assertEquals(2,loaded.size)
    }
}
