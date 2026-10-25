package ar.com.rallygpsai

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.*

/** Only for noncommercial development: the public OSRM DEMO has no availability guarantee. */
object RouteServices {
    private fun get(url: String): String {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 12000
        con.readTimeout = 18000
        con.setRequestProperty("User-Agent", "RallyGPSAI/0.1 (Android; map demonstration; contact: github.com/maxijones012/RallyNotes)")
        con.setRequestProperty("Accept", "application/json")
        try {
            check(con.responseCode in 200..299) { "HTTP ${con.responseCode}: servicio temporalmente no disponible" }
            return con.inputStream.bufferedReader().use { it.readText() }
        } finally { con.disconnect() }
    }

    /** Coincidencia geográfica: no asumir que una calle sin altura es el domicilio pedido. */
    data class PlaceCandidate(val point: GeoPoint, val address: String, val distanceKm: Double?,
                              val approximate: Boolean, val sourceType: String)

    private val numberPattern = Regex("(?:^|\\s)(\\d{1,5})(?:\\s|$)")
    private val explicitCountry = Regex("\\b(uruguay|chile|brasil|brazil|bolivia|paraguay|perú|peru)\\b", RegexOption.IGNORE_CASE)
    private var lastRequestAt = 0L
    private var localityCache: Triple<GeoPoint, String, String>? = null

    @Synchronized private fun queryNominatim(url: String): JSONArray {
        // Política del endpoint público: máximo una consulta por segundo por cliente.
        val wait = 1150L - (System.currentTimeMillis() - lastRequestAt)
        if (wait > 0) Thread.sleep(wait)
        lastRequestAt = System.currentTimeMillis()
        return JSONArray(get(url))
    }

    @Synchronized private fun queryNominatimText(url: String): String {
        val wait = 1150L - (System.currentTimeMillis() - lastRequestAt)
        if (wait > 0) Thread.sleep(wait)
        lastRequestAt = System.currentTimeMillis()
        return get(url)
    }

    fun searchResults(place: String, nearby: GeoPoint?): List<PlaceCandidate> {
        val q = place.trim().take(140)
        require(q.length >= 3) { "Ingresá una calle, altura o localidad" }
        // Comas no implican ciudad: "25 de Mayo, 456" sigue siendo dirección local.
        val explicitLocation = q.count { it == ',' } >= 2 || q.contains(Regex("\\b(trelew|rawson|madryn|comodoro|neuqu[eé]n|buenos aires|montevideo|córdoba|argentina|uruguay|chubut)\\b", RegexOption.IGNORE_CASE))
        val requestedNumber = numberPattern.findAll(q).lastOrNull()?.groupValues?.get(1)
        // Preferir la región GPS, nunca una coincidencia arbitraria mundial.
        val viewbox = nearby?.let { p ->
            val dy = 0.65; val dx = 0.65 / cos(Math.toRadians(p.latitude)).coerceAtLeast(0.25)
            "&viewbox=${p.longitude-dx},${p.latitude+dy},${p.longitude+dx},${p.latitude-dy}&bounded=0"
        } ?: ""
        val country = when {
            q.contains(Regex("\\b(uruguay|montevideo|punta del este)\\b", RegexOption.IGNORE_CASE)) -> "&countrycodes=uy"
            q.contains(Regex("\\b(chile|santiago de chile|valpara[ií]so)\\b", RegexOption.IGNORE_CASE)) -> "&countrycodes=cl"
            explicitCountry.containsMatchIn(q) -> ""
            q.contains(Regex("\\b(argentina|trelew|rawson|chubut|madryn|comodoro|neuqu[eé]n)\\b", RegexOption.IGNORE_CASE)) -> "&countrycodes=ar"
            nearby != null && nearby.latitude in -35.2..-30.0 && nearby.longitude in -59.0..-53.0 -> "&countrycodes=uy"
            nearby == null || nearby.latitude in -56.0..-21.0 && nearby.longitude in -74.0..-53.0 -> "&countrycodes=ar"
            else -> ""
        }
        fun find(text: String): JSONArray {
            val encoded = URLEncoder.encode(text,"UTF-8")
            return queryNominatim("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=10&dedupe=1$viewbox$country")
        }
        // Sin ciudad explícita, localizar la localidad real del GPS una vez
        // y consultar la dirección como la escribe el usuario, pero contextualizada.
        var arr: JSONArray
        var fallback = false
        var contextual = q
        if (nearby != null && !explicitLocation) {
            val cached = localityCache
            val locationName = if (cached != null && DrivingCamera.distance(cached.first, nearby) < 20_000) {
                "${cached.second}, ${cached.third}"
            } else {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=${nearby.latitude}&lon=${nearby.longitude}&zoom=10&addressdetails=1"
                val address = JSONObject(queryNominatimText(url)).optJSONObject("address")
                val city = listOf("city", "town", "village", "municipality", "county").firstNotNullOfOrNull { key ->
                    address?.optString(key)?.takeIf { it.isNotBlank() }
                }.orEmpty()
                val state = address?.optString("state").orEmpty()
                if (city.isNotBlank()) localityCache = Triple(nearby,city,state)
                "$city, $state"
            }
            if (locationName.trim(' ', ',').isNotEmpty()) contextual = "$q, $locationName"
        }
        arr = find(contextual)
        if (arr.length() == 0 && contextual != q) arr = find(q)
        val nearbyExact = nearby == null || (0 until arr.length()).any { index ->
            val match = arr.getJSONObject(index)
            val lat = match.optDouble("lat",Double.NaN); val lon = match.optDouble("lon",Double.NaN)
            lat.isFinite() && lon.isFinite() && DrivingCamera.distance(nearby,GeoPoint(lat,lon)) < 160_000
        }
        if ((arr.length()==0 || (!explicitLocation && !nearbyExact)) && requestedNumber != null) {
            // Si OSM no tiene numeración local, ofrecer la calle cercana como aproximada.
            val simplified = q.replace(Regex("\\b${Regex.escape(requestedNumber)}\\b"), "")
                .replace(Regex("\\s+"), " ").trim(' ', ',')
            if (simplified.length >= 3) {
                val suffix = if (contextual != q) contextual.removePrefix(q) else ""
                arr = find(simplified + suffix)
                fallback = true
            }
        }
        val out = mutableListOf<PlaceCandidate>()
        for (i in 0 until arr.length()) {
            val item=arr.getJSONObject(i)
            val lat=item.optDouble("lat",Double.NaN); val lon=item.optDouble("lon",Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) continue
            val point=GeoPoint(lat,lon)
            val distance=nearby?.let { DrivingCamera.distance(it,point)/1000.0 }
            val address=item.optJSONObject("address")
            val house=address?.optString("house_number", "").orEmpty()
            val approximate=fallback || (requestedNumber != null && house != requestedNumber)
            out.add(PlaceCandidate(point,item.optString("display_name","Ubicación"),distance,approximate,
                item.optString("type", "")))
        }
        // El orden favorece geográficamente el GPS, sin falsear la calidad del dato.
        // No sugerir como destino local una calle al otro lado del continente.
        val filtered = if (nearby != null && !explicitLocation)
            out.filter { (it.distanceKm ?: 1e6) <= 160.0 }
        else out
        return filtered.sortedWith(compareBy<PlaceCandidate> { if (nearby != null && !explicitLocation) {
            when { (it.distanceKm ?: 1e6) < 15 -> 0; (it.distanceKm ?: 1e6) < 80 -> 1; (it.distanceKm ?: 1e6) < 250 -> 2; else -> 3 }
        } else 0 }.thenBy { it.approximate }.thenBy { it.distanceKm ?: 0.0 })
    }

    fun route(start: GeoPoint, via: List<GeoPoint>, end: GeoPoint): List<GeoPoint> {
        require(via.size <= 4) { "Demasiadas paradas intermedias" }
        // Non-commercial demo server. Calls happen only via user interaction.
        val waypointsString = (listOf(start) + via + end).joinToString(";") { "${it.longitude},${it.latitude}" }
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            waypointsString +
            "?overview=full&geometries=geojson&steps=false"
        val result = JSONObject(get(url))
        check(result.optString("code") == "Ok") { "No se encontró un recorrido" }
        val routes = result.getJSONArray("routes")
        check(routes.length() > 0) { "No hay caminos entre los puntos" }
        val coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        val out = ArrayList<GeoPoint>(coords.length())
        for (i in 0 until coords.length()) {
            val item = coords.getJSONArray(i)
            out.add(GeoPoint(item.getDouble(1), item.getDouble(0)))
        }
        check(out.size >= 2 && out.size <= 30000) { "Ruta inválida o demasiado grande" }
        return out
    }
}

object GpxIO {
    fun read(input: InputStream): List<GeoPoint> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        val doc = factory.newDocumentBuilder().parse(input)
        val points = ArrayList<GeoPoint>()
        // Accept GPX trackpoints or routepoints. Avoid mixing trk/rte points in one run.
        val tracks = doc.getElementsByTagNameNS("*", "trkpt")
        val nodes = if (tracks.length > 0) tracks else doc.getElementsByTagNameNS("*", "rtept")
        for (i in 0 until nodes.length) {
            val p = nodes.item(i) as? Element ?: continue
            val lat = p.getAttribute("lat").toDoubleOrNull() ?: continue
            val lon = p.getAttribute("lon").toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lon in -180.0..180.0) points.add(GeoPoint(lat, lon))
            require(points.size <= 30000) { "Archivo demasiado grande (máximo 30.000 puntos)" }
        }
        require(points.size >= 2) { "El GPX necesita un track con al menos dos puntos" }
        return points
    }

    fun write(points: List<GeoPoint>): String {
        val body = points.joinToString("\n") { "    <trkpt lat=\"${it.latitude}\" lon=\"${it.longitude}\"/>" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Rally GPS AI" xmlns="http://www.topografix.com/GPX/1/1">
  <trk><name>Recorrido Rally GPS AI</name><trkseg>
$body
  </trkseg></trk>
</gpx>"""
    }
}

object RouteStore {
    fun serialize(points: List<GeoPoint>): String {
        val arr = JSONArray()
        points.forEach { p -> arr.put(JSONArray().put(p.latitude).put(p.longitude)) }
        return arr.toString()
    }
    fun deserialize(data: String): List<GeoPoint> {
        val arr = JSONArray(data)
        return (0 until arr.length()).map { index ->
            val p = arr.getJSONArray(index)
            GeoPoint(p.getDouble(0), p.getDouble(1))
        }
    }
}
