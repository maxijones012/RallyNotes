package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallyStartSequenceTest {
    @Test fun orderAlwaysCountsThreeTwoOneGo() {
        assertEquals(listOf("3","2","1","GO!"),(0..3).map(RallyStartSequence::screen))
        assertEquals("",RallyStartSequence.screen(4))
    }
    @Test fun spokenCountdownUsesSelectedLanguage() {
        assertEquals("Tres",RallyStartSequence.voice(0,"es-AR"))
        assertEquals("Three",RallyStartSequence.voice(0,"en-US"))
        assertEquals("Três",RallyStartSequence.voice(0,"pt-BR"))
        assertEquals("¡Vamos!",RallyStartSequence.voice(3,"es-AR"))
    }
}
