package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class NavigationUiPolicyTest {
    @Test fun planningShowsSearchAndTools() {
        val state=NavigationUiPolicy.state(false,false,false,false)
        assertTrue(state.showSearch)
        assertTrue(state.showEditingTools)
        assertFalse(state.showSpeed)
    }
    @Test fun realNavigationNeverShowsSearchEvenWhenToolsExpanded() {
        for (expanded in listOf(true,false)) {
            val state=NavigationUiPolicy.state(true,false,expanded,true)
            assertFalse(state.showSearch)
            assertEquals(expanded,state.showEditingTools)
            assertTrue(state.showDistance)
            assertTrue(state.showSpeed)
        }
    }
    @Test fun demoAndGpsShareVisibilityPolicy() {
        for (expanded in listOf(true,false)) {
            assertEquals(NavigationUiPolicy.state(true,false,expanded,true),
                NavigationUiPolicy.state(false,true,expanded,true))
        }
    }
    @Test fun finishingRestoresPlanning() {
        assertTrue(NavigationUiPolicy.state(false,false,false,true).showSearch)
    }
}
