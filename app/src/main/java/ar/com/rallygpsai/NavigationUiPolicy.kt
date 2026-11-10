package ar.com.rallygpsai

/** Centralizes all UI visibility decisions so DEMO/GPS cannot expose edit/search controls. */
data class NavigationUiState(
    val showSearch: Boolean, val showEditingTools: Boolean,
    val showMapOptions: Boolean, val showSpeed: Boolean,
    val showDistance: Boolean, val showDrivingButtons: Boolean
)

object NavigationUiPolicy {
    fun state(navigating: Boolean, demo: Boolean, expanded: Boolean, hasRoute: Boolean): NavigationUiState {
        val running = navigating || demo
        return NavigationUiState(
            showSearch = !running,
            showEditingTools = !running || expanded,
            showMapOptions = true,
            showSpeed = running,
            showDistance = hasRoute,
            showDrivingButtons = running
        )
    }
}
