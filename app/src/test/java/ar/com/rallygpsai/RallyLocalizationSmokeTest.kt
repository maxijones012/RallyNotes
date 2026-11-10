package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyLocalizationSmokeTest {
    @Test fun newUiKeysExistForEverySupportedLanguage() {
        val keys = listOf("resume","map_style","map_dark","map_day",
            "gpx_import","gpx_export","manual_bump","clear_bumps","moving_lock")
        for (language in RallyLanguage.codes) {
            for (key in keys) {
                val translated=RallyLanguage.text(language,key)
                assertNotEquals("$language missing key $key",key,translated)
                assertTrue("$language blank $key",translated.isNotBlank())
            }
        }
    }
}
