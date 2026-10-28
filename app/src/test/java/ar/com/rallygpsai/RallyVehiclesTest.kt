package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

/** Catalog contracts. Android Canvas rendering is visually validated on a device. */
class RallyVehiclesTest {
    @Test fun containsRallyStreetPickupsAndFourWheelDrive() {
        val ids = RallyVehicles.models.map { it.id }
        assertTrue(ids.size >= 20)
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.contains(RallyVehicles.DEFAULT))
        assertTrue(ids.contains(RallyVehicles.DOT))
        val groups = RallyVehicles.models.map { it.group }.toSet()
        assertTrue(groups.containsAll(listOf("Rally", "Calle", "Pickup", "4x4", "Especial", "Clásico")))
    }

    @Test fun allModelsHaveDrawableLabelsAndValidColors() {
        for (vehicle in RallyVehicles.models) {
            assertTrue(vehicle.name.isNotBlank())
            assertTrue(vehicle.group.isNotBlank())
            assertTrue(vehicle.id.matches(Regex("[a-z0-9_]+")))
            assertTrue(vehicle.color.matches(Regex("#[0-9a-fA-F]{6}")))
            assertTrue(vehicle.accent.matches(Regex("#[0-9a-fA-F]{6}")))
            assertTrue(vehicle.number >= 0)
        }
    }

    @Test fun forwardFacingSpriteContractAndSafeFallback() {
        assertEquals("subaru", RallyVehicles.DEFAULT)
        assertEquals("dot", RallyVehicles.DOT)
        assertEquals("subaru", RallyVehicles.get("unknown").id)
        assertEquals(RallyVehicles.Body.PICKUP, RallyVehicles.get("hilux").body)
        assertEquals(RallyVehicles.Body.OFFROAD, RallyVehicles.get("jimny").body)
        assertEquals(RallyVehicles.Body.UTV, RallyVehicles.get("utv").body)
    }
}
