package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyVoicePackTest {
    private fun note(side: PaceNote.Side, grade: Int, start: Double = 100.0, end: Double = 130.0) =
        PaceNote(start, side, grade, 55, 60.0, end)

    @Test fun allDirectionsAndGradesHaveAudioKeys() {
        val all = RallyVoicePack.requiredTokens()
        for (g in 1..6) for (side in PaceNote.Side.values()) {
            val tokens = RallyVoicePack.tokens(note(side, g), null, 150.0)
            assertTrue(tokens.all { it in all })
            assertEquals("dist_150", tokens[1])
        }
    }
    @Test fun linkedTurnsAndTightenUseSupportedAssets() {
        val first = note(PaceNote.Side.RIGHT, 5).copy(tightensTo = 3)
        val linked = note(PaceNote.Side.LEFT, 4, 170.0)
        val tokens = RallyVoicePack.tokens(first, linked, 85.0)
        assertEquals(listOf("en", "dist_90", "metros", "derecha_5", "se_cierra_a_3", "enlaza", "izquierda_4"), tokens)
        assertTrue(tokens.all { it in RallyVoicePack.requiredTokens() })
    }
    @Test fun longDistanceWarningsUseDedicatedClips() {
        val n = note(PaceNote.Side.RIGHT, 3)
        val all = RallyVoicePack.requiredTokens()
        assertEquals("dist_800", RallyVoicePack.tokens(n, null, 796.0)[1])
        assertEquals("dist_700", RallyVoicePack.tokens(n, null, 680.0)[1])
        assertEquals("dist_1000", RallyVoicePack.tokens(n, null, 1000.0)[1])
        for (m in 500..1000 step 100) {
            assertTrue(RallyVoicePack.tokens(n, null, m.toDouble()).all { it in all })
        }
    }
    @Test fun immediateHairpinAndLongCurve() {
        val n = note(PaceNote.Side.LEFT, 2, 100.0, 240.0).copy(cumulativeTurnDegrees = 165)
        assertEquals(listOf("ahora", "horquilla_izquierda", "larga"), RallyVoicePack.tokens(n, null, 8.0))
    }
    @Test fun everyWarningDistanceTenMetresHasAnAsset() {
        val all = RallyVoicePack.requiredTokens()
        for (d in 20..400 step 10) assertTrue(RallyVoicePack.tokens(note(PaceNote.Side.LEFT, 4), null, d.toDouble()).all { it in all })
    }
}
