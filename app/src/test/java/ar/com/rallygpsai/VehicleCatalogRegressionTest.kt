package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class VehicleCatalogRegressionTest {
    @Test fun carsHaveUniqueIdsNamesAndValidColors() {
        val vehicles=RallyVehicles.models
        assertTrue(vehicles.size >= 20)
        assertEquals(vehicles.size, vehicles.map { it.id }.toSet().size)
        assertTrue(vehicles.all { it.name.isNotBlank() && it.group.isNotBlank() })
        assertTrue(vehicles.all { it.color.matches(Regex("^#[0-9a-fA-F]{6}$")) })
        assertTrue(vehicles.all { it.accent.matches(Regex("^#[0-9a-fA-F]{6}$")) })
        assertEquals(RallyVehicles.DEFAULT,RallyVehicles.models.first().id)
        assertTrue(vehicles.any { it.id == RallyVehicles.DOT })
    }
    @Test fun allCarsHaveTopDownSpriteType() {
        assertTrue(RallyVehicles.models.all { it.body in RallyVehicles.Body.entries })
        assertTrue(RallyVehicles.models.filter { it.group=="Rally" }.size >= 8)
    }
}
