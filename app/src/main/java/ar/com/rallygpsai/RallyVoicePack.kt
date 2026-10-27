package ar.com.rallygpsai

import android.content.Context
import android.media.MediaPlayer
import kotlin.math.roundToInt

/**
 * Clips originales sintetizados en etapa de compilación con CarlFM (voz es-ES, MIT,
 * corpus declarado dominio público); ninguna librería/modelo Piper va dentro del APK.
 */
object RallyVoicePack {
    const val SYSTEM = "system"
    const val CLEAR = "clear"
    const val RADIO = "radio"
    val modes = listOf(SYSTEM, CLEAR, RADIO)

    /** Secuencia determinista: la nota se construye de clips autocontenidos. */
    fun tokens(note: PaceNote, next: PaceNote?, distanceM: Double): List<String> = buildList {
        if (distanceM.isFinite() && distanceM <= 22.0) add("ahora")
        else {
            val metres = if (distanceM.isFinite()) ((distanceM / 10.0).roundToInt() * 10).coerceIn(20, 400) else 400
            add("en"); add("dist_$metres"); add("metros")
        }
        val left = note.side == PaceNote.Side.LEFT
        val hairpin = note.cumulativeTurnDegrees >= 145 && note.grade <= 2
        add(if (hairpin) "horquilla_${if (left) "izquierda" else "derecha"}"
            else "${if (left) "izquierda" else "derecha"}_${note.grade.coerceIn(1, 6)}")
        if (note.tightensTo != null && note.tightensTo < note.grade) {
            add("se_cierra_a_${note.tightensTo.coerceIn(1, 6)}")
        } else if (note.opensTo != null && note.opensTo > note.grade) {
            add("abre_a_${note.opensTo.coerceIn(1, 6)}")
        }
        if (note.endMetres - note.metresFromStart >= 95.0 ||
            (note.cumulativeTurnDegrees >= 95 && note.grade >= 3)) add("larga")
        if (next != null && next.metresFromStart - note.endMetres in 0.0..85.0) {
            add("enlaza")
            add("${if (next.side == PaceNote.Side.LEFT) "izquierda" else "derecha"}_${next.grade.coerceIn(1, 6)}")
        }
    }

    /** Clips que la etapa de síntesis debe producir; no puede faltar ninguno. */
    fun requiredTokens(): Set<String> = buildSet {
        addAll(listOf("en", "metros", "ahora", "larga", "enlaza", "horquilla_izquierda", "horquilla_derecha"))
        for (grade in 1..6) {
            add("izquierda_$grade"); add("derecha_$grade")
            add("se_cierra_a_$grade"); add("abre_a_$grade")
        }
        for (m in 20..400 step 10) add("dist_$m")
    }
}

/** Single MediaPlayer queue: a new pacenote cancels old clips, avoiding stale commands. */
class RallyAudioPlayer(private val context: Context) {
    private var current: MediaPlayer? = null
    private val queue = ArrayDeque<Int>()
    private var active = false
    private var generation = 0

    fun available(mode: String): Boolean {
        if (mode !in listOf(RallyVoicePack.CLEAR, RallyVoicePack.RADIO)) return false
        return id(mode, "derecha_5") != 0 && id(mode, "en") != 0 && id(mode, "metros") != 0
    }

    private fun id(mode: String, key: String): Int {
        val resource = "rally_carlfm_${mode}_$key"
        return context.resources.getIdentifier(resource, "raw", context.packageName)
    }

    /** False means pack is unavailable; caller must fall back to Android TTS. */
    fun play(mode: String, tokens: List<String>): Boolean {
        if (tokens.isEmpty() || !available(mode)) return false
        val ids = tokens.map { id(mode, it) }
        if (ids.any { it == 0 }) return false
        stop()
        queue.addAll(ids)
        active = true
        startNext(generation)
        return true
    }

    private fun startNext(expected: Int) {
        if (!active || expected != generation) return
        val resId = queue.removeFirstOrNull() ?: run { active = false; return }
        try {
            val player = MediaPlayer.create(context, resId) ?: run { stop(); return }
            current = player
            player.setOnCompletionListener { finished ->
                if (current === finished) current = null
                finished.release()
                if (active && expected == generation) startNext(expected)
            }
            player.setOnErrorListener { broken, _, _ ->
                if (current === broken) current = null
                broken.release()
                if (expected == generation) { active = false; queue.clear() }
                true
            }
            player.start()
        } catch (_: Exception) {
            stop()
        }
    }

    fun isBusy(): Boolean = active

    fun stop() {
        generation++
        active = false
        queue.clear()
        current?.let { player ->
            try { player.setOnCompletionListener(null); player.setOnErrorListener(null); player.stop() } catch (_: Exception) { }
            player.release()
        }
        current = null
    }
}
