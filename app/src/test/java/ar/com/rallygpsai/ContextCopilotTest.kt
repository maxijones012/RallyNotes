package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

/** Pure Kotlin context-copilot regressions: no map, network, microphone or AI model. */
class ContextCopilotTest {
    private fun turn(m: Double, side: PaceNote.Side, end: Double = m + 45.0) =
        PaceNote(m, side, 4, 55, 85.0, end, confidence = 0.8)

    private fun route(vararg notes: PaceNote) = RoutePlan(
        listOf(GeoPoint(-43.0, -65.0), GeoPoint(-43.0, -64.95)),
        listOf(0.0, 4000.0),
        notes.toList()
    )

    private fun windingRoute() = route(
        turn(520.0, PaceNote.Side.LEFT),
        turn(690.0, PaceNote.Side.RIGHT),
        turn(870.0, PaceNote.Side.LEFT),
        turn(1040.0, PaceNote.Side.RIGHT)
    )

    @Test fun offModeAndStationaryNeverComment() {
        val engine = ContextCopilot()
        val r = windingRoute()
        assertNull(engine.evaluate(r, 0.0, 15.0, 1000, ContextMode.OFF, 520.0))
        assertNull(engine.evaluate(r, 0.0, 0.0, 80000, ContextMode.OCCASIONAL, 520.0))
        assertNull(engine.evaluate(r, 0.0, 2.0, 120000, ContextMode.OCCASIONAL, 520.0))
    }

    @Test fun waitsBeforeFirstObservationThenRecognizesWindingSection() {
        val engine = ContextCopilot()
        val r = windingRoute()
        assertNull(engine.evaluate(r, 0.0, 15.0, 1000, ContextMode.OCCASIONAL, 520.0))
        assertNull(engine.evaluate(r, 0.0, 15.0, 29000, ContextMode.OCCASIONAL, 520.0))
        val cue = engine.evaluate(r, 0.0, 15.0, 32000, ContextMode.OCCASIONAL, 520.0)
        assertEquals(ContextKind.WINDING, cue?.kind)
        assertNull(engine.evaluate(r, 0.0, 15.0, 400000, ContextMode.OCCASIONAL, 520.0))
    }

    @Test fun detectsLinkedPairButNotTwoUnconnectedTurns() {
        val linked = route(turn(520.0, PaceNote.Side.LEFT, 565.0),
            turn(610.0, PaceNote.Side.RIGHT, 660.0))
        val engine = ContextCopilot()
        engine.evaluate(linked, 0.0, 15.0, 1000, ContextMode.TALKATIVE, 520.0)
        assertEquals(ContextKind.LINKED,
            engine.evaluate(linked, 0.0, 15.0, 35000, ContextMode.TALKATIVE, 520.0)?.kind)

        val separate = route(turn(530.0, PaceNote.Side.LEFT, 560.0),
            turn(780.0, PaceNote.Side.RIGHT, 820.0))
        val another = ContextCopilot()
        another.evaluate(separate, 0.0, 15.0, 1000, ContextMode.TALKATIVE, 530.0)
        assertNull(another.evaluate(separate, 0.0, 15.0, 45000, ContextMode.TALKATIVE, 530.0))
    }

    @Test fun callsStraightOnlyAfterRealBendsAndWhenNoNearbyBend() {
        val r = route(turn(180.0, PaceNote.Side.LEFT, 240.0),
            turn(1500.0, PaceNote.Side.RIGHT, 1550.0))
        val engine = ContextCopilot()
        engine.evaluate(r, 380.0, 12.0, 1000, ContextMode.OCCASIONAL, 1120.0)
        assertEquals(ContextKind.STRAIGHT,
            engine.evaluate(r, 380.0, 12.0, 42000, ContextMode.OCCASIONAL, 1120.0)?.kind)
        val never = ContextCopilot()
        never.evaluate(r, 0.0, 12.0, 1000, ContextMode.OCCASIONAL, 180.0)
        assertNull(never.evaluate(r, 0.0, 12.0, 50000, ContextMode.OCCASIONAL, 180.0))
    }

    @Test fun upcomingCurveAndPriorityAudioBlockAllCommentary() {
        val r = windingRoute()
        val close = ContextCopilot()
        close.evaluate(r, 0.0, 15.0, 1000, ContextMode.OCCASIONAL, 100.0)
        assertNull(close.evaluate(r, 0.0, 15.0, 40000, ContextMode.OCCASIONAL, 100.0))
        val busy = ContextCopilot()
        busy.evaluate(r, 0.0, 15.0, 1000, ContextMode.OCCASIONAL, 520.0)
        assertNull(busy.evaluate(r, 0.0, 15.0, 40000, ContextMode.OCCASIONAL, 520.0, priorityBusy = true))
    }

    @Test fun cadenceAndModesRespectedWithoutRepeatedSection() {
        val r = route(
            turn(520.0, PaceNote.Side.LEFT),
            turn(690.0, PaceNote.Side.RIGHT),
            turn(870.0, PaceNote.Side.LEFT),
            turn(1040.0, PaceNote.Side.RIGHT),
            turn(1750.0, PaceNote.Side.LEFT),
            turn(1930.0, PaceNote.Side.RIGHT),
            turn(2110.0, PaceNote.Side.LEFT),
            turn(2290.0, PaceNote.Side.RIGHT)
        )
        val engine = ContextCopilot()
        engine.evaluate(r, 0.0, 15.0, 1000, ContextMode.OCCASIONAL, 520.0)
        assertNotNull(engine.evaluate(r, 0.0, 15.0, 34000, ContextMode.OCCASIONAL, 520.0))
        assertNull(engine.evaluate(r, 1300.0, 15.0, 60000, ContextMode.OCCASIONAL, 450.0))
        assertNotNull(engine.evaluate(r, 1300.0, 15.0, 280000, ContextMode.OCCASIONAL, 450.0))
    }

    @Test fun modeDefaultsAndThreeLocalizedComments() {
        assertEquals(ContextMode.OCCASIONAL, ContextMode.fromId("unexpected"))
        assertEquals(ContextMode.OFF, ContextMode.fromId("off"))
        assertEquals(ContextMode.TALKATIVE, ContextMode.fromId("talkative"))
        for (kind in ContextKind.values()) {
            val cue = ContextCue(kind, "test")
            for (language in RallyLanguage.codes) {
                val sentence = RallyLanguage.contextComment(language, cue)
                assertTrue(sentence.length > 25)
                assertFalse(sentence.contains("barro"))
                assertFalse(sentence.contains("traffic jam"))
            }
        }
    }
}
