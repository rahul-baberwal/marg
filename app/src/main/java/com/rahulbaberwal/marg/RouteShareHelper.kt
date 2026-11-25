package com.rahulbaberwal.marg

import android.content.Context
import android.content.Intent
import java.net.URLEncoder

object RouteShareHelper {

    fun generateShareUrl(uiState: MargUiState): String {
        val modeStr = when (uiState.mode) {
            Mode.DRIVE -> "drive"
            Mode.WALK -> "walk"
            Mode.BIKE -> "bike"
            Mode.TRAIN -> "train"
            Mode.FIXED -> "fixed"
        }

        val start = uiState.startPoint
        val end = uiState.endPoint
        val waypoints = uiState.waypoints

        val params = mutableListOf<String>()
        params.add("mode=$modeStr")

        if (start != null) {
            params.add("start=%.5f,%.5f".format(start.lat, start.lon))
            if (uiState.startLabel.isNotBlank()) {
                params.add("sname=" + runCatching { URLEncoder.encode(uiState.startLabel, "UTF-8") }.getOrDefault(uiState.startLabel))
            }
        }

        if (end != null) {
            params.add("end=%.5f,%.5f".format(end.lat, end.lon))
            if (uiState.endLabel.isNotBlank()) {
                params.add("ename=" + runCatching { URLEncoder.encode(uiState.endLabel, "UTF-8") }.getOrDefault(uiState.endLabel))
            }
        }

        if (waypoints.isNotEmpty()) {
            val wpStr = waypoints.joinToString(";") { "%.5f,%.5f".format(it.lat, it.lon) }
            params.add("stops=" + runCatching { URLEncoder.encode(wpStr, "UTF-8") }.getOrDefault(wpStr))
            if (uiState.waypointLabels.any { it.isNotBlank() }) {
                val labelsStr = uiState.waypointLabels.joinToString(";") { it }
                params.add("snames=" + runCatching { URLEncoder.encode(labelsStr, "UTF-8") }.getOrDefault(labelsStr))
            }
        }

        val queryString = params.joinToString("&")
        return "https://rahulbaberwal.github.io/marg/route.html?$queryString"
    }

    fun shareRoute(context: Context, uiState: MargUiState) {
        val shareUrl = generateShareUrl(uiState)
        val modeName = when (uiState.mode) {
            Mode.DRIVE -> "🚗 Drive"
            Mode.WALK -> "🚶 Walk"
            Mode.BIKE -> "🚴 Cycle"
            Mode.TRAIN -> "🚆 Train"
            Mode.FIXED -> "🏠 Fixed Location"
        }

        val distText = uiState.route?.let { "• Distance: %.1f km\n".format(it.distanceM / 1000.0) } ?: ""
        val startText = uiState.startLabel.ifEmpty { uiState.startPoint?.let { "%.5f, %.5f".format(it.lat, it.lon) } } ?: "Not set"
        val endText = uiState.endLabel.ifEmpty { uiState.endPoint?.let { "%.5f, %.5f".format(it.lat, it.lon) } } ?: ""

        val routeTitle = if (uiState.mode == Mode.FIXED) {
            "Location: $startText"
        } else {
            "Route: $startText → $endText"
        }

        val shareMessage = buildString {
            append("📍 Marg $modeName Simulator\n")
            append("$routeTitle\n")
            if (distText.isNotEmpty()) append(distText)
            append("\n👉 Open in Marg app:\n")
            append(shareUrl)
            append("\n\n📱 Don't have Marg? Download here:\n")
            append("• Play Store: https://play.google.com/store/apps/details?id=com.rahulbaberwal.marg\n")
            append("• Indus Appstore: https://indusapp.store/3y8hrlz0")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Marg Route: $startText")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        context.startActivity(Intent.createChooser(intent, "Share Route via"))
    }
}
