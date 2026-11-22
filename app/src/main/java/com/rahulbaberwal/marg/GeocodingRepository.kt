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

    private const val USER_AGENT = "Marg/1.0.1 (com.rahulbaberwal.marg)"

    fun search(query: String, userLat: Double = 20.5937, userLon: Double = 78.9629): List<SearchResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        // Match coordinate pattern e.g. "28.6139, 77.2090" or "28.6139 77.2090"
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

        // Try Photon first (fast, typo-tolerant, biased to location)
        val photonResults = searchPhoton(q, userLat, userLon)
        if (photonResults.isNotEmpty()) {
            return photonResults
        }

        // Fallback to Nominatim
        return searchNominatim(q)
    }

    private fun searchPhoton(query: String, biasLat: Double, biasLon: Double): List<SearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://photon.komoot.io/api/?q=$encoded&lat=$biasLat&lon=$biasLon&limit=10&lang=en")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 4000
        conn.readTimeout = 6000
        return try {
            if (conn.responseCode != 200) return emptyList()
            val text = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(text)
            val features = root.optJSONArray("features") ?: return emptyList()
            val list = mutableListOf<SearchResult>()
            for (i in 0 until features.length()) {
                val f = features.getJSONObject(i)
                val geom = f.optJSONObject("geometry") ?: continue
                val coords = geom.optJSONArray("coordinates") ?: continue
                val lon = coords.getDouble(0)
                val lat = coords.getDouble(1)
                val props = f.optJSONObject("properties") ?: JSONObject()

                val name = props.optString("name").ifEmpty {
                    props.optString("street").ifEmpty {
                        props.optString("city", "Location")
                    }
                }
                val detailsParts = listOfNotNull(
                    props.optString("city").takeIf { it.isNotEmpty() && it != name },
                    props.optString("state").takeIf { it.isNotEmpty() && it != name },
                    props.optString("country").takeIf { it.isNotEmpty() }
                )
                val details = if (detailsParts.isNotEmpty()) {
                    detailsParts.joinToString(", ")
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

    private fun searchNominatim(query: String): List<SearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=10&addressdetails=1&countrycodes=in")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 5000
        conn.readTimeout = 8000
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
        conn.setRequestProperty("User-Agent", USER_AGENT)
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
                displayName.isNotEmpty() -> displayName.split(",").take(2).joinToString(", ").trim()
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
