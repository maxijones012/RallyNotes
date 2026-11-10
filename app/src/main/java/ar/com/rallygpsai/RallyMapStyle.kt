package ar.com.rallygpsai

/** Official publicly documented OpenFreeMap styles; satellite imagery is not included. */
enum class RallyMapStyle(val id: String, val url: String) {
    DARK("dark", "https://tiles.openfreemap.org/styles/dark"),
    DAY("day", "https://tiles.openfreemap.org/styles/liberty");

    companion object {
        fun fromStored(value: String?): RallyMapStyle =
            entries.firstOrNull { it.id == value } ?: DARK
    }
}
