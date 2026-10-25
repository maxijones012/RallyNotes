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

    fun search(place: String): GeoPoint? {
        val q = URLEncoder.encode(place.trim(), "UTF-8")
        val arr = JSONArray(get("https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=1"))
        if (arr.length() == 0) return null
        val item = arr.getJSONObject(0)
        return GeoPoint(item.getString("lat").toDouble(), item.getString("lon").toDouble())
    }

    fun route(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        // Non-commercial demo server. Do not call more than once a second or run as a production backend.
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
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
        check(out.size >= 2) { "Geometría de ruta vacía" }
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
