package ar.com.rallygpsai

import org.junit.Assert.*
import org.junit.Test

class RallySafetySpeechTest {
    @Test fun rallyNoteAlwaysTakesPriority() {
        assertFalse(RallySafetySpeech.mayAlert(100.0,15.0,500.0,true,false,30_000))
        assertFalse(RallySafetySpeech.mayAlert(100.0,15.0,50.0,false,false,30_000))
        assertFalse(RallySafetySpeech.mayAlert(100.0,15.0,500.0,false,true,30_000))
        assertTrue(RallySafetySpeech.mayAlert(100.0,15.0,500.0,false,false,30_000))
    }
    @Test fun badDistancesCannotTriggerSpeech() {
        assertFalse(RallySafetySpeech.mayAlert(Double.NaN,10.0,500.0,false,false,30_000))
        assertFalse(RallySafetySpeech.mayAlert(100.0,0.0,500.0,false,false,30_000))
    }
}
