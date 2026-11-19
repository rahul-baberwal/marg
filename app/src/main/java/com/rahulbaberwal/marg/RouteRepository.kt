package com.rahulbaberwal.marg

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class P(val lat: Double, val lon: Double)

data class RouteResult(val points: List<P>, val distanceM: Double, val durationS: Double)

/**
 * Routing modes supported by Marg:
 * - WALK: OpenStreetMap foot router (5 km/h)
 * - BIKE: OpenStreetMap bicycle router (15 km/h)
 * - DRIVE: OpenStreetMap car router (40 km/h)
 * - TRAIN: OpenRailRouting railway tracks with fallback to road router (80 km/h)
 */
enum class Mode(val defaultKmh: Int) {
    WALK(5),
    BIKE(15),
    DRIVE(40),
    TRAIN(80)
}

object RouteRepository {

    fun fetch(mode: Mode, s: P, e: P): RouteResult {
        return if (mode == Mode.TRAIN) {
            try {
                fetchTrainRoute(s, e)
            } catch (_: Exception) {
                // Graceful fallback to road route if points are not near rail tracks
                fetchRoadRoute(mode, s, e)
            }
        } else {
            fetchRoadRoute(mode, s, e)
        }
    }

    private fun fetchRoadRoute(mode: Mode, s: P, e: P): RouteResult {
        val server = when (mode) {
            Mode.WALK -> "routed-foot"
            Mode.BIKE -> "routed-bike"
            else -> "routed-car"
        }
        val url = URL(
            "https://routing.openstreetmap.de/$server/route/v1/driving/" +
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

    private fun fetchTrainRoute(s: P, e: P): RouteResult {
        val url = URL(
            "https://routing.openrailrouting.org/route?point=${s.lat},${s.lon}&point=${e.lat},${e.lon}&profile=all_tracks&points_encoded=false"
        )
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Marg/0.1 (com.rahulbaberwal.marg)")
        c.connectTimeout = 15000
        c.readTimeout = 30000
        try {
            if (c.responseCode != 200) {
                throw IllegalStateException("Railway routing server returned ${c.responseCode}")
            }
            val json = JSONObject(c.inputStream.bufferedReader().readText())
            val paths = json.optJSONArray("paths")
            if (paths == null || paths.length() == 0) {
                error("No railway route found between these points")
            }
            val path = paths.getJSONObject(0)
            val coords = path.getJSONObject("points").getJSONArray("coordinates")
            val pts = List(coords.length()) {
                val c2 = coords.getJSONArray(it)
                P(c2.getDouble(1), c2.getDouble(0))
            }
            val distanceM = path.optDouble("distance", 0.0)
            val durationS = path.optDouble("time", 0.0) / 1000.0
            return RouteResult(pts, distanceM, durationS)
        } finally {
            c.disconnect()
        }
    }
}
