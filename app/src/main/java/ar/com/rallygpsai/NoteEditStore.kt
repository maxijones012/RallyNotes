package ar.com.rallygpsai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** All edits remain on this device; versioned by exact route geometry fingerprint. */
class NoteEditStore(context: Context) {
    private val prefs = context.getSharedPreferences("rally_pacenote_corrections_v1", Context.MODE_PRIVATE)

    fun load(base: RoutePlan): List<NoteCorrection> {
        val raw = prefs.getString(key(base), null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val j = arr.getJSONObject(i)
                    val side = PaceNote.Side.valueOf(j.getString("side"))
                    val edit = NoteCorrection(j.getInt("index"),j.getDouble("original"),
                        j.getDouble("position"),side,j.getInt("grade"),
                        j.optInt("tightens",0).takeIf { it in 1..6 },
                        j.optInt("opens",0).takeIf { it in 1..6 },
                        if (j.has("long") && !j.isNull("long")) j.getBoolean("long") else null)
                    edit.takeIf { EditablePacenotes.validate(base,it) == null }
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun save(base: RoutePlan, edits: List<NoteCorrection>): Boolean {
        require(edits.size <= 500) { "Demasiadas notas modificadas" }
        val valid = edits.filter { EditablePacenotes.validate(base,it) == null }
        val json = JSONArray()
        valid.distinctBy { it.baseIndex }.forEach { e ->
            json.put(JSONObject().put("index", e.baseIndex).put("original",e.originalMetres)
                .put("position",e.correctedMetres).put("side",e.side.name).put("grade",e.grade)
                .put("tightens",e.tightensTo).put("opens",e.opensTo).put("long",e.longCurve))
        }
        val editor = prefs.edit()
        if (valid.isEmpty()) editor.remove(key(base)) else editor.putString(key(base),json.toString())
        return editor.commit()
    }

    private fun key(base: RoutePlan) = "route_" + EditablePacenotes.fingerprint(base.points)
}
