package ar.com.rallygpsai

/** Three-second pre-start call. GO is the only step that starts GPS/DEMO. */
object RallyStartSequence {
    fun screen(step: Int): String = when(step) {
        0 -> "3"
        1 -> "2"
        2 -> "1"
        3 -> "GO!"
        else -> ""
    }
    fun voice(step: Int, language: String): String = when (step) {
        0 -> if (language.startsWith("en")) "Three" else if (language.startsWith("pt")) "Três" else "Tres"
        1 -> if (language.startsWith("en")) "Two" else if (language.startsWith("pt")) "Dois" else "Dos"
        2 -> if (language.startsWith("en")) "One" else if (language.startsWith("pt")) "Um" else "Uno"
        3 -> if (language.startsWith("en")) "Go!" else if (language.startsWith("pt")) "Vai!" else "¡Vamos!"
        else -> ""
    }
}
