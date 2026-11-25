package com.rahulbaberwal.marg

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

data class ParsedMapLocation(
    val lat: Double,
    val lon: Double,
    val label: String
)

data class ParsedRouteData(
    val start: P,
    val end: P?,
    val waypoints: List<P> = emptyList(),
    val startLabel: String = "",
    val endLabel: String = "",
    val waypointLabels: List<String> = emptyList(),
    val mode: Mode = Mode.DRIVE
)

sealed class ParsedMapIntent {
    data class SingleLocation(val location: ParsedMapLocation) : ParsedMapIntent()
    data class FullRoute(val route: ParsedRouteData) : ParsedMapIntent()
}

object MapLinkParser {

    private val COORD_REGEX = Regex("""([+-]?\d{1,2}(?:\.\d+)?)[,\s]+([+-]?\d{1,3}(?:\.\d+)?)""")
    private val AT_COORD_REGEX = Regex("""@([+-]?\d{1,2}(?:\.\d+)?),([+-]?\d{1,3}(?:\.\d+)?)""")
    private val PROTOBUF_COORD_REGEX = Regex("""!3d([+-]?\d{1,2}(?:\.\d+)?)(?:!4d|.*!4d)([+-]?\d{1,3}(?:\.\d+)?)""")
    private val OSM_HASH_REGEX = Regex("""#map=\d+/([+-]?\d{1,2}(?:\.\d+)?)/([+-]?\d{1,3}(?:\.\d+)?)""")
    private val URL_IN_TEXT_REGEX = Regex("""https?://[^\s"'<>]+""")

    suspend fun parseIntent(intent: Intent): ParsedMapIntent? = withContext(Dispatchers.IO) {
        try {
            // Check Uri in intent.data
            val uri = intent.data
            if (uri != null) {
                // Check if this is a shared Marg route (scheme: marg, or path has route/route.html, or has start=)
                val route = parseRouteUri(uri)
                if (route != null) return@withContext ParsedMapIntent.FullRoute(route)

                val single = parseUri(uri)
                if (single != null) return@withContext ParsedMapIntent.SingleLocation(single)
            }

            // Check Shared text via ACTION_SEND
            val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.clipData?.getItemAt(0)?.text?.toString()
            if (!extraText.isNullOrBlank()) {
                val urlMatch = URL_IN_TEXT_REGEX.find(extraText)
                if (urlMatch != null) {
                    val parsedUri = Uri.parse(urlMatch.value)
                    val route = parseRouteUri(parsedUri)
                    if (route != null) return@withContext ParsedMapIntent.FullRoute(route)

                    val single = parseUri(parsedUri)
                    if (single != null) return@withContext ParsedMapIntent.SingleLocation(single)
                }

                val single = parseText(extraText)
                if (single != null) return@withContext ParsedMapIntent.SingleLocation(single)
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    fun parseRouteUri(uri: Uri): ParsedRouteData? {
        val scheme = uri.scheme?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""

        val isMargRoute = scheme == "marg" ||
                host.contains("github.io") ||
                uri.getQueryParameter("start") != null

        if (!isMargRoute) return null

        val startStr = uri.getQueryParameter("start") ?: return null
        val startCoord = COORD_REGEX.find(startStr) ?: return null
        val startLat = startCoord.groupValues[1].toDoubleOrNull() ?: return null
        val startLon = startCoord.groupValues[2].toDoubleOrNull() ?: return null
        val start = P(startLat, startLon)

        val endStr = uri.getQueryParameter("end")
        val end = endStr?.let {
            val endCoord = COORD_REGEX.find(it) ?: return@let null
            val lat = endCoord.groupValues[1].toDoubleOrNull() ?: return@let null
            val lon = endCoord.groupValues[2].toDoubleOrNull() ?: return@let null
            P(lat, lon)
        }

        val modeStr = uri.getQueryParameter("mode")?.lowercase()
        val mode = when (modeStr) {
            "walk" -> Mode.WALK
            "bike" -> Mode.BIKE
            "train" -> Mode.TRAIN
            "fixed" -> Mode.FIXED
            else -> Mode.DRIVE
        }

        val sname = uri.getQueryParameter("sname")?.let {
            runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
        } ?: ""

        val ename = uri.getQueryParameter("ename")?.let {
            runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
        } ?: ""

        val waypoints = mutableListOf<P>()
        val stopsStr = uri.getQueryParameter("stops")
        if (!stopsStr.isNullOrBlank()) {
            val decodedStops = runCatching { URLDecoder.decode(stopsStr, "UTF-8") }.getOrDefault(stopsStr)
            val stopParts = decodedStops.split(";")
            for (part in stopParts) {
                val match = COORD_REGEX.find(part.trim()) ?: continue
                val lat = match.groupValues[1].toDoubleOrNull() ?: continue
                val lon = match.groupValues[2].toDoubleOrNull() ?: continue
                waypoints.add(P(lat, lon))
            }
        }

        val waypointLabels = mutableListOf<String>()
        val snamesStr = uri.getQueryParameter("snames")
        if (!snamesStr.isNullOrBlank()) {
            val decodedNames = runCatching { URLDecoder.decode(snamesStr, "UTF-8") }.getOrDefault(snamesStr)
            waypointLabels.addAll(decodedNames.split(";"))
        }

        return ParsedRouteData(
            start = start,
            end = end,
            waypoints = waypoints,
            startLabel = sname,
            endLabel = ename,
            waypointLabels = waypointLabels,
            mode = mode
        )
    }

    suspend fun parse(intent: Intent): ParsedMapLocation? {
        val result = parseIntent(intent)
        return when (result) {
            is ParsedMapIntent.SingleLocation -> result.location
            is ParsedMapIntent.FullRoute -> ParsedMapLocation(result.route.start.lat, result.route.start.lon, result.route.startLabel.ifEmpty { "Start location" })
            null -> null
        }
    }

    suspend fun parseText(text: String): ParsedMapLocation? {
        val trimmed = text.trim()

        // 1. Check if text contains a web URL (e.g. from Google Maps share)
        val urlMatch = URL_IN_TEXT_REGEX.find(trimmed)
        if (urlMatch != null) {
            val parsedUri = Uri.parse(urlMatch.value)
            val res = parseUri(parsedUri)
            if (res != null) return res
        }

        // 2. Check if text contains raw coordinates: e.g. "28.6139, 77.2090"
        val coordMatch = COORD_REGEX.find(trimmed)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lon = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoord(lat, lon)) {
                val label = GeocodingRepository.reverse(lat, lon) ?: "%.5f, %.5f".format(lat, lon)
                return ParsedMapLocation(lat, lon, label)
            }
        }

        // 3. Fallback: Treat as search query if within reasonable length
        if (trimmed.length in 3..120 && !trimmed.startsWith("http")) {
            val searchResults = GeocodingRepository.search(trimmed)
            val first = searchResults.firstOrNull()
            if (first != null) {
                return ParsedMapLocation(first.lat, first.lon, first.name)
            }
        }

        return null
    }

    suspend fun parseUri(uri: Uri): ParsedMapLocation? {
        val scheme = uri.scheme?.lowercase() ?: return null

        return when (scheme) {
            "geo" -> parseGeoUri(uri)
            "http", "https" -> parseWebMapUri(uri)
            else -> null
        }
    }

    private suspend fun parseGeoUri(uri: Uri): ParsedMapLocation? {
        val ssp = uri.schemeSpecificPart ?: ""
        val qParam = uri.getQueryParameter("q")

        // 1. Query parameter "q" e.g. geo:0,0?q=28.6139,77.2090(Title) or geo:0,0?q=India+Gate
        if (!qParam.isNullOrBlank()) {
            val decodedQ = runCatching { URLDecoder.decode(qParam, "UTF-8") }.getOrDefault(qParam)

            // Extract optional label in parentheses: e.g. "28.6139,77.2090(Hotel Taj)"
            val labelInParen = Regex("""\((.*?)\)""").find(decodedQ)?.groupValues?.get(1)?.trim()
            val cleanedQuery = decodedQ.replace(Regex("""\(.*?\)"""), "").trim()

            val coordMatch = COORD_REGEX.find(cleanedQuery)
            if (coordMatch != null) {
                val lat = coordMatch.groupValues[1].toDoubleOrNull()
                val lon = coordMatch.groupValues[2].toDoubleOrNull()
                if (lat != null && lon != null && isValidCoord(lat, lon)) {
                    val label = labelInParen?.takeIf { it.isNotBlank() }
                        ?: GeocodingRepository.reverse(lat, lon)
                        ?: "%.5f, %.5f".format(lat, lon)
                    return ParsedMapLocation(lat, lon, label)
                }
            }

            // If not coordinates, geocode text query: e.g. "geo:0,0?q=India+Gate"
            val results = GeocodingRepository.search(cleanedQuery)
            val first = results.firstOrNull()
            if (first != null) {
                val label = labelInParen?.takeIf { it.isNotBlank() } ?: first.name
                return ParsedMapLocation(first.lat, first.lon, label)
            }
        }

        // 2. Scheme specific part before '?' e.g. geo:28.6139,77.2090 or geo:28.6139,77.2090?z=16
        val coordPart = ssp.substringBefore("?").trim()
        val match = COORD_REGEX.find(coordPart)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoord(lat, lon)) {
                val label = GeocodingRepository.reverse(lat, lon) ?: "%.5f, %.5f".format(lat, lon)
                return ParsedMapLocation(lat, lon, label)
            }
        }

        return null
    }

    private suspend fun parseWebMapUri(rawUri: Uri): ParsedMapLocation? {
        var uri = rawUri
        val host = uri.host?.lowercase() ?: ""

        // Handle short URLs (e.g. maps.app.goo.gl or goo.gl/maps) by following redirect
        if (host.contains("maps.app.goo.gl") || host == "goo.gl") {
            val resolvedUrl = resolveRedirect(uri.toString())
            uri = Uri.parse(resolvedUrl)
        }

        val urlString = uri.toString()

        // 1. Check path for @lat,lon (Google Maps standard: /@28.6129,77.2295,17z)
        val atMatch = AT_COORD_REGEX.find(urlString)
        if (atMatch != null) {
            val lat = atMatch.groupValues[1].toDoubleOrNull()
            val lon = atMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoord(lat, lon)) {
                val label = extractPlaceNameFromGoogleUrl(uri)
                    ?: GeocodingRepository.reverse(lat, lon)
                    ?: "%.5f, %.5f".format(lat, lon)
                return ParsedMapLocation(lat, lon, label)
            }
        }

        // 2. Check protobuf format (!3dlat!4dlon in Google Maps URLs)
        val protoMatch = PROTOBUF_COORD_REGEX.find(urlString)
        if (protoMatch != null) {
            val lat = protoMatch.groupValues[1].toDoubleOrNull()
            val lon = protoMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoord(lat, lon)) {
                val label = extractPlaceNameFromGoogleUrl(uri)
                    ?: GeocodingRepository.reverse(lat, lon)
                    ?: "%.5f, %.5f".format(lat, lon)
                return ParsedMapLocation(lat, lon, label)
            }
        }

        // 3. Query parameters (q, query, daddr, ll, destination)
        val candidateParams = listOf("q", "query", "daddr", "destination", "ll")
        for (param in candidateParams) {
            val value = uri.getQueryParameter(param) ?: continue
            val decoded = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

            val coordMatch = COORD_REGEX.find(decoded)
            if (coordMatch != null) {
                val lat = coordMatch.groupValues[1].toDoubleOrNull()
                val lon = coordMatch.groupValues[2].toDoubleOrNull()
                if (lat != null && lon != null && isValidCoord(lat, lon)) {
                    val label = GeocodingRepository.reverse(lat, lon) ?: "%.5f, %.5f".format(lat, lon)
                    return ParsedMapLocation(lat, lon, label)
                }
            }

            // Search query text fallback
            if (decoded.length in 3..120) {
                val results = GeocodingRepository.search(decoded)
                val first = results.firstOrNull()
                if (first != null) {
                    return ParsedMapLocation(first.lat, first.lon, first.name)
                }
            }
        }

        // 4. OpenStreetMap coordinates: mlat & mlon or #map=...
        val mlat = uri.getQueryParameter("mlat")?.toDoubleOrNull()
        val mlon = uri.getQueryParameter("mlon")?.toDoubleOrNull()
        if (mlat != null && mlon != null && isValidCoord(mlat, mlon)) {
            val label = GeocodingRepository.reverse(mlat, mlon) ?: "%.5f, %.5f".format(mlat, mlon)
            return ParsedMapLocation(mlat, mlon, label)
        }

        val fragment = uri.fragment ?: ""
        val osmMatch = OSM_HASH_REGEX.find(fragment)
        if (osmMatch != null) {
            val lat = osmMatch.groupValues[1].toDoubleOrNull()
            val lon = osmMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidCoord(lat, lon)) {
                val label = GeocodingRepository.reverse(lat, lon) ?: "%.5f, %.5f".format(lat, lon)
                return ParsedMapLocation(lat, lon, label)
            }
        }

        return null
    }

    private fun extractPlaceNameFromGoogleUrl(uri: Uri): String? {
        val path = uri.path ?: return null
        val segments = path.split("/").filter { it.isNotBlank() }
        val placeIdx = segments.indexOf("place")
        if (placeIdx != -1 && placeIdx + 1 < segments.size) {
            val placeSegment = segments[placeIdx + 1]
            val cleaned = placeSegment.replace("+", " ").replace("%20", " ")
            if (cleaned.isNotBlank()) return cleaned
        }
        return null
    }

    private fun resolveRedirect(urlString: String): String {
        var currentUrl = urlString
        var redirects = 0
        while (redirects < 5) {
            val url = runCatching { URL(currentUrl) }.getOrNull() ?: return currentUrl
            val conn = (url.openConnection() as? HttpURLConnection) ?: return currentUrl
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = if (location.startsWith("/")) {
                            "${url.protocol}://${url.host}$location"
                        } else {
                            location
                        }
                        redirects++
                        continue
                    }
                }
                break
            } catch (_: Exception) {
                break
            } finally {
                conn.disconnect()
            }
        }
        return currentUrl
    }

    private fun isValidCoord(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)
    }
}
