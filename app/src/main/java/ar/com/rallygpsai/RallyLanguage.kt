package ar.com.rallygpsai

import java.util.Locale
import kotlin.math.roundToInt

/** Persistent locale for the welcome screen, navigation UI and spoken rally calls. */
object RallyLanguage {
    const val KEY = "app_language"
    const val SETUP_KEY = "onboarding_done"
    val codes = listOf("es-AR", "es-ES", "en-US", "pt-BR")
    val names = listOf("Español (Argentina)", "Español (España)", "English", "Português (Brasil)")
    fun normalized(value: String?): String = value?.takeIf { codes.contains(it) } ?: "es-AR"
    fun defaultFor(device: Locale): String = when (device.language.lowercase(Locale.ROOT)) {
        "en" -> "en-US"
        "pt" -> "pt-BR"
        "es" -> if (device.country.equals("ES", true)) "es-ES" else "es-AR"
        else -> "es-AR"
    }
    fun locale(code: String): Locale = Locale.forLanguageTag(normalized(code))
    fun isSpanish(code: String) = normalized(code).startsWith("es-")
    private val es = mapOf(
        "welcome" to "¡Bienvenido a Rally GPS AI!", "select" to "Elegí el idioma para navegar y escuchar al copiloto.",
        "language" to "IDIOMA", "start" to "EMPEZAR", "change" to "Idioma de la aplicación",
        "voice_hint" to "CarlFM grabado: solo español. En inglés y portugués se usa la voz del teléfono.",
        "search_hint" to "Destino: localidad, calle o dirección", "search" to "BUSCAR",
        "gps" to "GPS disponible · mantené pulsado el mapa para elegir destino",
        "dest" to "ELEGÍ UN DESTINO", "dest_sub" to "Las notas 1–6 se generan automáticamente",
        "voice_on" to "VOZ ✓", "voice_off" to "MUDO", "route" to "TRAZAR RUTA",
        "go" to "INICIAR GPS", "finish" to "FINALIZAR", "pause" to "PAUSA",
        "points" to "PUNTOS", "settings" to "AJUSTES", "next" to "HASTA LA PRÓXIMA CURVA",
        "note" to "NOTA DE RALLY", "left" to "IZQUIERDA", "right" to "DERECHA",
        "linked" to "ENLAZA", "long" to "LARGA", "close" to "CIERRA A", "open" to "ABRE A",
        "hairpin" to "HORQUILLA", "now" to "AHORA", "remaining" to "restantes",
        "destination_selected" to "DESTINO SELECCIONADO", "language_saved" to "Idioma guardado",
        "ready" to "RUTA LISTA", "detected" to "NOTAS DETECTADAS", "approx" to "APROXIMADA"
    )
    private val en = mapOf(
        "welcome" to "Welcome to Rally GPS AI!", "select" to "Choose your navigation and co-driver language.",
        "language" to "LANGUAGE", "start" to "GET STARTED", "change" to "App language",
        "voice_hint" to "Recorded CarlFM: Spanish only. English uses your phone's voice.",
        "search_hint" to "Destination: town, street or address", "search" to "SEARCH",
        "gps" to "GPS available · long-press map to choose destination",
        "dest" to "CHOOSE A DESTINATION", "dest_sub" to "Automatic 1–6 rally notes",
        "voice_on" to "VOICE ✓", "voice_off" to "MUTED", "route" to "PLAN ROUTE",
        "go" to "START GPS", "finish" to "FINISH", "pause" to "PAUSE",
        "points" to "POINTS", "settings" to "SETTINGS", "next" to "DISTANCE TO NEXT BEND",
        "note" to "PACE NOTE", "left" to "LEFT", "right" to "RIGHT",
        "linked" to "INTO", "long" to "LONG", "close" to "TIGHTENS TO", "open" to "OPENS TO",
        "hairpin" to "HAIRPIN", "now" to "NOW", "remaining" to "remaining",
        "destination_selected" to "DESTINATION SELECTED", "language_saved" to "Language saved",
        "ready" to "ROUTE READY", "detected" to "NOTES DETECTED", "approx" to "APPROXIMATE"
    )
    private val pt = mapOf(
        "welcome" to "Bem-vindo ao Rally GPS AI!", "select" to "Escolha o idioma da navegação e do copiloto.",
        "language" to "IDIOMA", "start" to "COMEÇAR", "change" to "Idioma do aplicativo",
        "voice_hint" to "CarlFM gravado: apenas espanhol. Português usa a voz do celular.",
        "search_hint" to "Destino: cidade, rua ou endereço", "search" to "BUSCAR",
        "gps" to "GPS disponível · toque e segure no mapa para escolher o destino",
        "dest" to "ESCOLHA UM DESTINO", "dest_sub" to "Notas automáticas de rali de 1 a 6",
        "voice_on" to "VOZ ✓", "voice_off" to "MUDO", "route" to "CALCULAR ROTA",
        "go" to "INICIAR GPS", "finish" to "FINALIZAR", "pause" to "PAUSAR",
        "points" to "PONTOS", "settings" to "AJUSTES", "next" to "ATÉ A PRÓXIMA CURVA",
        "note" to "NOTA DE RALI", "left" to "ESQUERDA", "right" to "DIREITA",
        "linked" to "EMENDA", "long" to "LONGA", "close" to "FECHA PARA", "open" to "ABRE PARA",
        "hairpin" to "COTOVELO", "now" to "AGORA", "remaining" to "restantes",
        "destination_selected" to "DESTINO SELECIONADO", "language_saved" to "Idioma salvo",
        "ready" to "ROTA PRONTA", "detected" to "NOTAS DETECTADAS", "approx" to "APROXIMADA"
    )
    fun text(code: String, key: String): String = when {
        normalized(code).startsWith("en") -> en[key]
        normalized(code).startsWith("pt") -> pt[key]
        else -> es[key]
    } ?: es[key] ?: key
    fun call(code: String, note: PaceNote, next: PaceNote? = null): String {
        if (isSpanish(code)) return PaceNotes.phrase(note, next)
        val l = normalized(code)
        val side = if (note.side == PaceNote.Side.LEFT) text(l, "left") else text(l, "right")
        val pieces = mutableListOf<String>()
        if (note.cumulativeTurnDegrees >= 145 && note.grade <= 2) pieces += text(l, "hairpin")
        pieces += side + " " + note.grade
        note.tightensTo?.takeIf { it < note.grade }?.let { pieces += text(l, "close") + " " + it }
        note.opensTo?.takeIf { it > note.grade }?.let { pieces += text(l, "open") + " " + it }
        if (note.endMetres - note.metresFromStart >= 95.0 || (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) pieces += text(l, "long")
        if (next != null && next.metresFromStart - note.endMetres in 0.0..85.0) {
            val nside = if (next.side == PaceNote.Side.LEFT) text(l, "left") else text(l, "right")
            pieces += text(l, "linked") + " " + nside + " " + next.grade
        }
        return pieces.joinToString(", ").lowercase(locale(l))
    }
    fun distanceCall(code: String, distance: Double): String {
        if (distance <= 22.0) return text(code, "now")
        val m = ((distance / 10.0).roundToInt() * 10).coerceIn(20, 400)
        return when {
            normalized(code).startsWith("en") -> "In " + m + " meters"
            normalized(code).startsWith("pt") -> "Em " + m + " metros"
            else -> "En " + m + " metros"
        }
    }
}
