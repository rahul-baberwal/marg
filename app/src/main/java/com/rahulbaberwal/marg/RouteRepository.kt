package com.rahulbaberwal.marg

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class P(val lat: Double, val lon: Double)

data class RouteResult(val points: List<P>, val distanceM: Double, val durationS: Double)

/** Public OSM-hosted OSRM instances (OpenStreetMap data). Fair-use only; self-host for heavy use. */
enum class Mode(val server: String, val defaultKmh: Int) {
    WALK("routed-foot", 5),
    BIKE("routed-bike", 15),
    DRIVE("routed-car", 40)
}

object RouteRepository {
    fun fetch(mode: Mode, s: P, e: P): RouteResult {
        val url = URL(
            "https://routing.openstreetmap.de/${mode.server}/route/v1/driving/" +
                "${s.lon},${s.lat};${e.lon},${e.lat}?overview=full&geometries=geojson"
        )
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Marg/0.1 (com.rahulbaberwal.marg)")
        c.connectTimeout = 15000
        c.readTimeout = 30000
        try {
            if (c.responseCode != 200) error("Routing server returned ${c.responseCode}")
            val json = JSONObject(c.inputStream.bufferedReader().readText())
            if (json.optString("code") != "Ok") error(json.optString("message", "No route found"))
            val r = json.getJSONArray("routes").getJSONObject(0)
            val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
            val pts = List(coords.length()) {
                val c2 = coords.getJSONArray(it)
                P(c2.getDouble(1), c2.getDouble(0))
            }
            return RouteResult(pts, r.getDouble("distance"), r.getDouble("duration"))
        } finally {
            c.disconnect()
        }
    }
}
