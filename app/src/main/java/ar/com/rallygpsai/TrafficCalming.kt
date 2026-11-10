package ar.com.rallygpsai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.math.*

/** Only mapped/manual traffic calming features. Never infer road hazards from curves. */
enum class BumpSource { OSM, MANUAL }
data class RawBump(val id: String, val point: GeoPoint, val kind: String, val source: BumpSource)
data class RouteBump(val raw: RawBump, val routeMetres: Double, val lateralMetres: Double)

object TrafficCalming {
    private val knownKinds = setOf("bump", "hump", "table", "cushion", "rumble_strip", "mini_bumps")
    fun normalize(value: String): String? =
        value.split(';').map { it.trim().lowercase() }.firstOrNull { it in knownKinds }

    fun readOverpassJson(json: String): List<RawBump> {
        val array = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val results = ArrayList<RawBump>()
        for (i in 0 until min(300, array.length())) {
            val obj = array.optJSONObject(i) ?: continue
            val lat = obj.optDouble("lat", Double.NaN)
            val lon = obj.optDouble("lon", Double.NaN)
            val tags = obj.optJSONObject("tags") ?: continue
            val kind = normalize(tags.optString("traffic_calming")) ?: continue
            if (!lat.isFinite() || !lon.isFinite() ||
                lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
            results.add(RawBump("osm_" + obj.optLong("id",i.toLong()),
                GeoPoint(lat,lon),kind,BumpSource.OSM))
        }
        return results
    }

    /**
     * Meter-distance orthogonal projection onto individual route segments.
     * This avoids flagging unrelated bumps on nearby parallel roads.
     */
    fun onRoute(plan: RoutePlan, candidates: List<RawBump>, maxLateralM: Double = 16.0): List<RouteBump> {
        if (plan.points.size < 2) return emptyList()
        val result = mutableListOf<RouteBump>()
        val segments = plan.points.zipWithNext()
        for (candidate in candidates) {
            val lat = Math.toRadians(candidate.point.latitude)
            val mx = 111_320.0 * cos(lat).coerceAtLeast(0.05)
            val my = 111_195.0
            var best = Double.POSITIVE_INFINITY
            var along = 0.0
            for ((i, segment) in segments.withIndex()) {
                val (a,b) = segment
                val ax = (a.longitude - candidate.point.longitude) * mx
                val ay = (a.latitude - candidate.point.latitude) * my
                val bx = (b.longitude - candidate.point.longitude) * mx
                val by = (b.latitude - candidate.point.latitude) * my
                val dx=bx-ax;val dy=by-ay
                val d2=dx*dx+dy*dy
                if (d2 <= 0.1) continue
                val t = ((-ax*dx-ay*dy)/d2).coerceIn(0.0,1.0)
                val dist = hypot(ax+t*dx,ay+t*dy)
                if (dist < best) {
                    best=dist
                    along=plan.metresAtPoint.getOrElse(i) { 0.0 } +
                        (plan.metresAtPoint.getOrElse(i+1) { 0.0 } -
                         plan.metresAtPoint.getOrElse(i) { 0.0 })*t
                }
            }
            if (best <= maxLateralM) result.add(RouteBump(candidate,along,best))
        }
        val dedup = mutableListOf<RouteBump>()
        for (bump in result.sortedBy { it.routeMetres }) {
            if (dedup.none { abs(it.routeMetres - bump.routeMetres) < 16.0 }) dedup.add(bump)
        }
        return dedup.take(150)
    }

    fun upcoming(bumps: List<RouteBump>, progressM: Double): RouteBump? =
        bumps.firstOrNull { it.routeMetres >= progressM - 8.0 }

    fun voiceName(kind: String): String = when (kind) {
        "rumble_strip" -> "bandas reductoras"
        else -> "reductor de velocidad"
    }
}

/** Optional read-only OSM query. No results is not evidence that a road is clear. */
object OSMTrafficCalmingService {
    fun fetchFor(plan: RoutePlan): List<RawBump> {
        if (plan.points.size < 2 || plan.points.size > 30000) return emptyList()
        val south = plan.points.minOf { it.latitude } - 0.002
        val north = plan.points.maxOf { it.latitude } + 0.002
        val west = plan.points.minOf { it.longitude } - 0.002
        val east = plan.points.maxOf { it.longitude } + 0.002
        if ((north-south)*(east-west) > 0.03 || north-south > 0.20 || east-west > 0.20) return emptyList()
        val bbox = listOf(south,west,north,east).joinToString(",") { "%.6f".format(java.util.Locale.US,it) }
        val q = "[out:json][timeout:12];node[traffic_calming~\"^(bump|hump|table|cushion|rumble_strip|mini_bumps)$\"]($bbox);out body;"
        val url = URL("https://overpass.private.coffee/api/interpreter")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod="POST"
        con.doOutput=true
        con.connectTimeout=6000
        con.readTimeout=13000
        con.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8")
        con.setRequestProperty("User-Agent","RallyGPSAI/0.3 (optional OSM traffic calming overlay)")
        try {
            con.outputStream.bufferedWriter().use { it.write("data=" + URLEncoder.encode(q,"UTF-8")) }
            if (con.responseCode !in 200..299) return emptyList()
            val body=con.inputStream.bufferedReader().use { it.readText().take(1_000_000) }
            return TrafficCalming.readOverpassJson(body)
        } finally { con.disconnect() }
    }
}

/** Private manual points (no GPS tracks uploaded or shared). */
class ManualTrafficCalmingStore(context: Context) {
    private val file = File(context.filesDir, "rally_manual_reductores.json")
    fun list(): List<RawBump> = runCatching {
        if (!file.exists()) return@runCatching emptyList()
        val a = JSONArray(file.readText(Charsets.UTF_8))
        (0 until min(a.length(),500)).mapNotNull { index ->
            val obj=a.optJSONObject(index) ?: return@mapNotNull null
            val lat=obj.optDouble("lat", Double.NaN);val lon=obj.optDouble("lon", Double.NaN)
            if (!lat.isFinite()||!lon.isFinite()||lat !in -90.0..90.0||lon !in -180.0..180.0)
                return@mapNotNull null
            RawBump("manual_"+index,GeoPoint(lat,lon),"hump",BumpSource.MANUAL)
        }
    }.getOrElse { emptyList() }

    fun add(point: GeoPoint) {
        require(point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0)
        val stored=list()
        if (stored.any { DrivingCamera.distance(it.point,point) < 12.0 }) return
        val data=JSONArray()
        for (b in stored.takeLast(499)+RawBump("",point,"hump",BumpSource.MANUAL)) {
            data.put(JSONObject().put("lat",b.point.latitude).put("lon",b.point.longitude))
        }
        file.writeText(data.toString(),Charsets.UTF_8)
    }
    fun clear() { if (file.exists()) file.delete() }
}
