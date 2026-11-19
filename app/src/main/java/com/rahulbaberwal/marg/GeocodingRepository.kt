package com.rahulbaberwal.marg

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val name: String,
    val details: String,
    val lat: Double,
    val lon: Double
)

object GeocodingRepository {

    fun search(query: String): List<SearchResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        // Match patterns like "28.6139, 77.2090" or "28.6139 77.2090"
        val coordRegex = Regex("""^([+-]?\d+(?:\.\d+)?)[,\s]+([+-]?\d+(?:\.\d+)?)$""")
        val match = coordRegex.find(q)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return listOf(
                    SearchResult(
                        name = "Coordinates: %.5f, %.5f".format(lat, lon),
                        details = "Latitude $lat, Longitude $lon",
                        lat = lat,
                        lon = lon
                    )
                )
            }
        }

        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=10&addressdetails=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Marg/0.1 (com.rahulbaberwal.marg)")
        conn.connectTimeout = 8000
        conn.readTimeout = 15000
        return try {
            if (conn.responseCode != 200) return emptyList()
            val text = conn.inputStream.bufferedReader().readText()
            val array = JSONArray(text)
            val list = mutableListOf<SearchResult>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                val fullDisplayName = obj.optString("display_name", "")
                val name = obj.optString("name").ifEmpty {
                    fullDisplayName.substringBefore(",")
                }
                val details = if (fullDisplayName.contains(",")) {
                    fullDisplayName.substringAfter(",").trim()
                } else {
                    "%.5f, %.5f".format(lat, lon)
                }
                list.add(SearchResult(name, details, lat, lon))
            }
            list
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    fun reverse(lat: Double, lon: Double): String? {
        val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Marg/0.1 (com.rahulbaberwal.marg)")
        conn.connectTimeout = 5000
        conn.readTimeout = 8000
        return try {
            if (conn.responseCode != 200) return null
            val text = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(text)
            val name = obj.optString("name")
            val displayName = obj.optString("display_name")
            when {
                name.isNotEmpty() -> name
                displayName.isNotEmpty() -> displayName.split(",").take(2).joinToString(",").trim()
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}

