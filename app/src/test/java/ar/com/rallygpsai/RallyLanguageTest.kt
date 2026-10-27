package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

/** Language selection and pace-note tests. Do not require GPS or network. */
class RallyLanguageTest {
    private fun sample(side: PaceNote.Side = PaceNote.Side.RIGHT, grade: Int = 4) =
        PaceNote(100.0, side, grade, 85, 95.0, 160.0, confidence = 0.8)

    @Test fun phoneLocaleChoosesUsableLanguage() {
        assertEquals("es-AR", RallyLanguage.defaultFor(Locale("es", "AR")))
        assertEquals("es-ES", RallyLanguage.defaultFor(Locale("es", "ES")))
        assertEquals("en-US", RallyLanguage.defaultFor(Locale("en", "GB")))
        assertEquals("pt-BR", RallyLanguage.defaultFor(Locale("pt", "PT")))
        assertEquals("es-AR", RallyLanguage.defaultFor(Locale("de", "DE")))
    }

    @Test fun unknownCodesAreSafeAndSpanishIsDefault() {
        assertEquals("es-AR", RallyLanguage.normalized("xx-XX"))
        assertEquals("es-AR", RallyLanguage.normalized(null))
        assertTrue(RallyLanguage.isSpanish("es-AR"))
        assertTrue(RallyLanguage.isSpanish("es-ES"))
        assertFalse(RallyLanguage.isSpanish("en-US"))
        assertFalse(RallyLanguage.isSpanish("pt-BR"))
    }

    @Test fun onboardingUiStringsExistInEachLocale() {
        for (code in RallyLanguage.codes) {
            for (key in listOf("welcome", "select", "language", "start", "change",
                "voice_hint", "route", "go", "settings", "next", "destination_point")) {
                val translation = RallyLanguage.text(code, key)
                assertFalse(translation.isBlank())
                assertNotEquals(key, translation)
            }
        }
    }

    @Test fun spokenPacenotesFollowSelectedLanguage() {
        assertTrue(RallyLanguage.call("es-AR", sample()).contains("derecha"))
        assertTrue(RallyLanguage.call("en-US", sample()).contains("right"))
        assertTrue(RallyLanguage.call("pt-BR", sample(PaceNote.Side.LEFT)).contains("esquerda"))
        assertTrue(RallyLanguage.distanceCall("en-US", 100.0).contains("meters"))
        assertTrue(RallyLanguage.distanceCall("pt-BR", 100.0).contains("metros"))
    }
}
