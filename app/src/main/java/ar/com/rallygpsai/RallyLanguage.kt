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
        "ready" to "RUTA LISTA", "detected" to "NOTAS DETECTADAS", "approx" to "APROXIMADA",
        "warn_short" to "Aviso corto (4 s)", "warn_normal" to "Aviso normal (7 s)", "warn_early" to "Aviso anticipado (10 s)",
        "repeat" to "Volver a escuchar una nota", "all_notes" to "Mostrar notas completas",
        "copilot" to "Elegir voz de copiloto", "test_voice" to "Probar voz de rally",
        "android_voice" to "Elegir voz Android", "pitch" to "Velocidad y tono de voz",
        "engine" to "Instalar/cambiar motor TTS", "licenses" to "Licencias de voz",
        "vehicle" to "Elegir vehículo del GPS", "destination_point" to "Señalar destino en mapa",
        "origin_point" to "Señalar origen en mapa", "via_point" to "Agregar parada intermedia",
        "gps_origin" to "Usar GPS como origen", "clear_stops" to "Borrar todas las paradas",
        "points_title" to "ORIGEN · DESTINO · PARADAS",
        "context_menu" to "Copiloto contextual", "context_off" to "Desactivado · solo notas",
        "context_occasional" to "Ocasional · cada 3–5 min",
        "context_talkative" to "Conversador · cada 1–2 min",
        "settings_nav" to "NAVEGACIÓN Y NOTAS", "settings_nav_desc" to "Anticipación, repetir y ver notas",
        "settings_voice" to "VOZ Y COPILOTO", "settings_voice_desc" to "Voces, comentarios y ajustes de audio",
        "settings_map" to "MAPA Y VEHÍCULO", "settings_map_desc" to "Seleccionar auto y cambiar tamaño",
        "settings_app" to "IDIOMA E INFORMACIÓN", "settings_app_desc" to "Idioma y licencias",
        "vehicle_size" to "Tamaño del vehículo GPS", "size_normal" to "Normal",
        "size_large" to "Grande", "size_xl" to "Extra grande", "back" to "ATRÁS"
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
        "ready" to "ROUTE READY", "detected" to "NOTES DETECTED", "approx" to "APPROXIMATE",
        "warn_short" to "Short warning (4 s)", "warn_normal" to "Normal warning (7 s)", "warn_early" to "Early warning (10 s)",
        "repeat" to "Replay last note", "all_notes" to "Show all pace notes",
        "copilot" to "Choose co-driver voice", "test_voice" to "Test rally voice",
        "android_voice" to "Choose Android voice", "pitch" to "Voice rate and pitch",
        "engine" to "Install/change TTS engine", "licenses" to "Voice licenses",
        "vehicle" to "Choose GPS vehicle", "destination_point" to "Select destination on map",
        "origin_point" to "Select origin on map", "via_point" to "Add intermediate stop",
        "gps_origin" to "Use GPS as origin", "clear_stops" to "Clear all stops",
        "points_title" to "ORIGIN · DESTINATION · STOPS",
        "context_menu" to "Contextual co-driver", "context_off" to "Off · pace notes only",
        "context_occasional" to "Occasional · every 3–5 min",
        "context_talkative" to "Talkative · every 1–2 min",
        "settings_nav" to "NAVIGATION & PACE NOTES", "settings_nav_desc" to "Warning timing, replay and note list",
        "settings_voice" to "VOICE & CO-DRIVER", "settings_voice_desc" to "Voice packs, comments and audio",
        "settings_map" to "MAP & VEHICLE", "settings_map_desc" to "Vehicle selection and size",
        "settings_app" to "LANGUAGE & INFO", "settings_app_desc" to "Language and licenses",
        "vehicle_size" to "GPS vehicle size", "size_normal" to "Normal",
        "size_large" to "Large", "size_xl" to "Extra large", "back" to "BACK"
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
        "ready" to "ROTA PRONTA", "detected" to "NOTAS DETECTADAS", "approx" to "APROXIMADA",
        "warn_short" to "Aviso curto (4 s)", "warn_normal" to "Aviso normal (7 s)", "warn_early" to "Aviso antecipado (10 s)",
        "repeat" to "Repetir última nota", "all_notes" to "Mostrar todas as notas",
        "copilot" to "Escolher voz do copiloto", "test_voice" to "Testar voz de rali",
        "android_voice" to "Escolher voz do Android", "pitch" to "Velocidade e tom da voz",
        "engine" to "Instalar/alterar motor TTS", "licenses" to "Licenças de voz",
        "vehicle" to "Escolher veículo do GPS", "destination_point" to "Selecionar destino no mapa",
        "origin_point" to "Selecionar origem no mapa", "via_point" to "Adicionar parada",
        "gps_origin" to "Usar GPS como origem", "clear_stops" to "Limpar paradas",
        "points_title" to "ORIGEM · DESTINO · PARADAS",
        "context_menu" to "Copiloto contextual", "context_off" to "Desativado · apenas notas",
        "context_occasional" to "Ocasional · a cada 3–5 min",
        "context_talkative" to "Conversador · a cada 1–2 min",
        "settings_nav" to "NAVEGAÇÃO E NOTAS", "settings_nav_desc" to "Antecipação, repetir e mostrar notas",
        "settings_voice" to "VOZ E COPILOTO", "settings_voice_desc" to "Vozes, comentários e ajustes de áudio",
        "settings_map" to "MAPA E VEÍCULO", "settings_map_desc" to "Escolher veículo e tamanho",
        "settings_app" to "IDIOMA E INFORMAÇÕES", "settings_app_desc" to "Idioma e licenças",
        "vehicle_size" to "Tamanho do veículo no GPS", "size_normal" to "Normal",
        "size_large" to "Grande", "size_xl" to "Extra grande", "back" to "VOLTAR"
    )
    fun text(code: String, key: String): String = when {
        normalized(code).startsWith("en") -> en[key]
        normalized(code).startsWith("pt") -> pt[key]
        else -> es[key]
    } ?: es[key] ?: key
    /** Safe claims only: these describe mapped curves, never the actual road surface. */
    fun contextComment(code: String, cue: ContextCue, variation: Int = 0): String {
        val variant = if (variation % 2 == 0) 0 else 1
        return when {
            normalized(code).startsWith("en") -> when (cue.kind) {
                ContextKind.WINDING -> if (variant == 0)
                    "The mapped route ahead has several bends. Pay attention to the road."
                    else "Several changes of direction are coming up on the mapped route."
                ContextKind.LINKED -> if (variant == 0)
                    "The map shows a sequence of linked bends ahead."
                    else "A couple of consecutive bends are coming up."
                ContextKind.STRAIGHT -> if (variant == 0)
                    "The route looks straighter for a while after those bends."
                    else "The next mapped section is relatively straight."
            }
            normalized(code).startsWith("pt") -> when (cue.kind) {
                ContextKind.WINDING -> if (variant == 0)
                    "O mapa indica várias curvas à frente. Preste atenção à estrada."
                    else "O traçado à frente tem várias mudanças de direção."
                ContextKind.LINKED -> if (variant == 0)
                    "O mapa mostra uma sequência de curvas ligadas à frente."
                    else "Vêm aí algumas curvas consecutivas."
                ContextKind.STRAIGHT -> if (variant == 0)
                    "Depois dessas curvas, o traçado fica mais reto por um tempo."
                    else "O próximo trecho no mapa é relativamente reto."
            }
            else -> when (cue.kind) {
                ContextKind.WINDING -> if (variant == 0)
                    "Se viene un tramo con varias curvas seguidas, prestá atención al camino."
                    else "Según el mapa, se acercan varios cambios de dirección."
                ContextKind.LINKED -> if (variant == 0)
                    "Más adelante hay una serie de curvas enlazadas."
                    else "El trazado marca varias curvas consecutivas."
                ContextKind.STRAIGHT -> if (variant == 0)
                    "Después de esas curvas, el trazado viene bastante más recto."
                    else "Según el mapa, ahora sigue un tramo relativamente recto."
            }
        }
    }

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
        val m = RallySpokenDistance.roundMetres(distance).coerceIn(20, 1000)
        return when {
            normalized(code).startsWith("en") -> "In " + m + " meters"
            normalized(code).startsWith("pt") -> "Em " + m + " metros"
            else -> "En " + m + " metros"
        }
    }
}
