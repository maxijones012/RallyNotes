package ar.com.rallygpsai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Private, compressed, offline trip history. GPS tracks stay on the phone.
 * No network service, account, or location sharing is involved.
 */
class TripHistoryStore(context: Context) {
    private val directory = File(context.filesDir, "rally_trip_history").apply { mkdirs() }

    fun save(record: TripRecord) {
        val json = JSONObject()
            .put("version", 1)
            .put("id", record.id)
            .put("startedAt", record.startedAtWallMs)
            .put("duration", record.durationMs)
            .put("distanceM", record.distanceM)
            .put("avgKmh", record.averageKmh)
            .put("maxKmh", record.maximumKmh)
            .put("route", asJson(record.route))
            .put("track", asJson(record.actualTrack))
        val tmp = File(directory, record.id + ".tmp")
        val dest = File(directory, record.id + ".json.gz")
        try {
            GZIPOutputStream(tmp.outputStream().buffered()).bufferedWriter(Charsets.UTF_8).use {
                it.write(json.toString())
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            files().drop(40).forEach { it.delete() }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    fun list(): List<TripRecord> =
        files().mapNotNull { file ->
            runCatching { read(file) }.getOrNull()
        }

    fun delete(id: String): Boolean {
        if (!id.matches(Regex("trip_[0-9]+"))) return false
        return File(directory, "$id.json.gz").delete()
    }

    private fun files(): List<File> = directory.listFiles()
        .orEmpty().filter { it.name.matches(Regex("trip_[0-9]+\\.json\\.gz")) }
        .sortedByDescending { it.name }

    private fun read(file: File): TripRecord {
        val body = GZIPInputStream(file.inputStream().buffered())
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val j = JSONObject(body)
        return TripRecord(
            j.getString("id"), j.getLong("startedAt"), j.getLong("duration"),
            j.getDouble("distanceM"), j.getDouble("avgKmh"), j.getDouble("maxKmh"),
            points(j.getJSONArray("route")), points(j.getJSONArray("track"))
        )
    }

    private fun asJson(points: List<GeoPoint>): JSONArray {
        val array = JSONArray()
        for (p in points) array.put(JSONArray().put(p.latitude).put(p.longitude))
        return array
    }

    private fun points(array: JSONArray): List<GeoPoint> =
        (0 until array.length()).map { n ->
            val p = array.getJSONArray(n)
            GeoPoint(p.getDouble(0), p.getDouble(1))
        }
}
