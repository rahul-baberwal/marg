package com.rahulbaberwal.marg

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class SimState { IDLE, RUNNING, PAUSED, FINISHED }

class MockLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private lateinit var lm: LocationManager
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val ch = NotificationChannel(CHANNEL, "Marg Live Route", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Live route tracking and simulated location progress"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin()
            ACTION_PAUSE -> {
                if (state.value == SimState.RUNNING) {
                    state.value = SimState.PAUSED
                    updateNotification(if (isFixedMode) "Paused at fixed location" else "Paused at current location")
                }
            }
            ACTION_RESUME -> {
                if (state.value == SimState.PAUSED) {
                    state.value = SimState.RUNNING
                    updateNotification(if (isFixedMode) "Simulating indoor presence..." else "Simulating route...")
                }
            }
            ACTION_STOP -> {
                // When stopped while running, stop on pause (hold current location, do not wipe out)
                if (state.value == SimState.RUNNING) {
                    state.value = SimState.PAUSED
                    updateNotification(if (isFixedMode) "Stopped at fixed location" else "Stopped at current location")
                } else if (state.value == SimState.PAUSED) {
                    // Tapping stop while already paused resets to start
                    resetToStart()
                } else {
                    shutdown(SimState.IDLE, keepPosition = false)
                }
            }
            ACTION_RESET -> resetToStart()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun begin() {
        startForegroundCompat()
        if (!setupProviders()) {
            error.tryEmit("mock_not_selected")
            shutdown(SimState.IDLE, keepPosition = false)
            return
        }

        if (isFixedMode) {
            val center = fixedLocation ?: route.firstOrNull() ?: position.value
            if (center == null) {
                error.tryEmit("No fixed location set")
                shutdown(SimState.IDLE, keepPosition = false)
                return
            }
            job?.cancel()
            job = scope.launch { simulateFixed(center) }
            return
        }

        val pts = route
        if (pts.size < 2) {
            error.tryEmit("No route loaded")
            shutdown(SimState.IDLE, keepPosition = false)
            return
        }
        job?.cancel()
        job = scope.launch { simulate(pts) }
    }

    private suspend fun simulateFixed(center: P) {
        state.value = SimState.RUNNING
        progress.value = 0f
        val wanderer = IndoorWanderer(center, indoorRadiusM)
        position.value = center

        while (true) {
            delay(1000)
            if (state.value == SimState.PAUSED) {
                if (jitter) {
                    val s = wanderer.tickStationary()
                    pushIndoorState(s)
                } else {
                    push(position.value ?: center, 0f, 0f)
                }
                continue
            }
            if (state.value != SimState.RUNNING) break

            wanderer.maxRadiusM = indoorRadiusM
            if (jitter) {
                val s = wanderer.tick()
                pushIndoorState(s)
            } else {
                push(center, 0f, 0f)
            }
        }
    }

    private suspend fun simulate(pts: List<P>) {
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + haversine(pts[i - 1], pts[i])
        val total = cum.last()

        // Resume from current progress if paused, or start from 0
        var dist = (progress.value.toDouble() * total).coerceIn(0.0, total)
        var i = 0
        while (i < pts.size - 2 && cum[i + 1] < dist) i++
        var lastBearing = if (pts.size > 1) bearing(pts[i], pts[i + 1]) else 0f
        var cur = position.value ?: pts[0]

        state.value = SimState.RUNNING
        push(cur, lastBearing, 0f)

        var pausedWanderer: IndoorWanderer? = null
        var lastNotifTime = 0L

        while (true) {
            delay(1000)
            if (state.value == SimState.PAUSED) {
                // When paused or stopped on pause, keep holding the last location with organic drift
                if (jitter) {
                    if (pausedWanderer == null || haversine(pausedWanderer.center, cur) > 20.0) {
                        pausedWanderer = IndoorWanderer(cur, 8.0)
                    }
                    val s = pausedWanderer.tickStationary()
                    pushIndoorState(s)
                } else {
                    push(cur, lastBearing, 0f)
                }
                continue
            }
            pausedWanderer = null
            if (state.value != SimState.RUNNING) break

            val mps = speedKmh / 3.6
            dist += mps
            if (dist >= total) {
                cur = pts.last()
                push(cur, lastBearing, 0f)
                progress.value = 1f
                updateLiveNotification(
                    contentText = "Arrived at destination!",
                    currentProgress = 1f,
                    remainingKm = 0.0,
                    etaMin = 0
                )
                shutdown(SimState.FINISHED, keepPosition = true)
                return
            }
            while (i < pts.size - 2 && cum[i + 1] < dist) i++
            val a = pts[i]
            val b = pts[i + 1]
            val seg = cum[i + 1] - cum[i]
            val t = if (seg > 0) (dist - cum[i]) / seg else 0.0
            cur = P(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
            if (seg > 0.5) lastBearing = bearing(a, b)
            val prog = (dist / total).toFloat().coerceIn(0f, 1f)
            progress.value = prog
            push(cur, lastBearing, mps.toFloat())

            // Periodic live notification updates (like Zomato / Uber delivery progress)
            val now = System.currentTimeMillis()
            if (now - lastNotifTime >= 2000L) {
                lastNotifTime = now
                val remainingKm = (total - dist).coerceAtLeast(0.0) / 1000.0
                val etaMin = if (speedKmh > 0) ((remainingKm / speedKmh) * 60).toInt() else 0
                updateLiveNotification(
                    contentText = "ETA ~$etaMin min (${"%.1f".format(remainingKm)} km remaining)",
                    currentProgress = prog,
                    remainingKm = remainingKm,
                    etaMin = etaMin
                )
            }
        }
    }

    @android.annotation.SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    private fun setupProviders(): Boolean = try {
        for (p in providers) {
            try { lm.removeTestProvider(p) } catch (_: Exception) {}
            lm.addTestProvider(
                p, false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            lm.setTestProviderEnabled(p, true)
        }
        true
    } catch (e: SecurityException) {
        false
    }

    private fun pushIndoorState(s: IndoorLocationState) {
        for (name in providers) {
            val l = Location(name).apply {
                latitude = s.point.lat
                longitude = s.point.lon
                altitude = s.altitude
                accuracy = s.accuracy
                speed = s.speedMps
                bearing = s.bearing
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bearingAccuracyDegrees = 10f
                    speedAccuracyMetersPerSecond = 0.2f
                    verticalAccuracyMeters = 3f
                }
            }
            try { lm.setTestProviderLocation(name, l) } catch (_: Exception) {}
        }
        position.value = s.point
    }

    private fun push(p: P, bearing: Float, speedMps: Float) {
        var lat = p.lat
        var lon = p.lon
        if (jitter) {
            // Subtle road micro-drift (~0.8m)
            lat += (Random.nextDouble() - 0.5) * 0.8e-5
            lon += (Random.nextDouble() - 0.5) * 0.8e-5
        }
        for (name in providers) {
            val l = Location(name).apply {
                latitude = lat
                longitude = lon
                altitude = 15.0 + (Random.nextDouble() - 0.5) * 0.5
                accuracy = if (jitter) 4f + Random.nextFloat() * 4f else 3f
                speed = speedMps
                this.bearing = bearing
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bearingAccuracyDegrees = 8f
                    speedAccuracyMetersPerSecond = 0.25f
                    verticalAccuracyMeters = 3f
                }
            }
            try { lm.setTestProviderLocation(name, l) } catch (_: Exception) {}
        }
        position.value = p
    }

    private fun resetToStart() {
        job?.cancel()
        val pts = route
        val startP = if (isFixedMode) (fixedLocation ?: position.value) else if (pts.isNotEmpty()) pts[0] else null
        progress.value = 0f
        state.value = SimState.IDLE
        if (startP != null) {
            push(startP, 0f, 0f)
        }
        removeProviders()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun shutdown(final: SimState, keepPosition: Boolean = true) {
        job?.cancel()
        removeProviders()
        if (!keepPosition) {
            position.value = null
        }
        state.value = final
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeProviders() {
        for (p in providers) try { lm.removeTestProvider(p) } catch (_: Exception) {}
    }

    private fun getModeEmoji(): String = when (modeName.lowercase()) {
        "walk" -> "🚶"
        "cycle" -> "🚴"
        "train" -> "🚆"
        "fixed" -> "🏠"
        else -> "🚗"
    }

    private fun buildNotification(
        contentText: String,
        currentProgress: Float = progress.value,
        remainingKm: Double = lastRemainingKm,
        etaMin: Int = lastEtaMin
    ): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseResume = PendingIntent.getService(
            this, 2,
            Intent(this, MockLocationService::class.java).setAction(
                if (state.value == SimState.RUNNING) ACTION_PAUSE else ACTION_RESUME
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val emoji = getModeEmoji()
        val isPaused = state.value == SimState.PAUSED
        val isFinished = state.value == SimState.FINISHED
        val progressPercent = (currentProgress * 100).toInt().coerceIn(0, 100)

        val title = when {
            isFinished -> "🏁 Arrived at $endLabel"
            isFixedMode -> "$emoji Fixed Location Active"
            isPaused -> "⏸️ Paused: On way to $endLabel"
            else -> "$emoji $modeName to $endLabel"
        }

        val subText = when {
            isFinished -> "Completed"
            isPaused -> "Paused • $progressPercent%"
            isFixedMode -> "Marg • ~${indoorRadiusM.toInt()}m wander"
            else -> "Marg • $progressPercent%"
        }

        val bigText = buildString {
            if (isFixedMode) {
                append("📍 Location: $startLabel\n")
                append("🔄 Indoor radius: ~${indoorRadiusM.toInt()}m drift (jitter ${if (jitter) "on" else "off"})")
            } else {
                append("📍 From: $startLabel\n")
                append("🏁 To: $endLabel\n")
                val spd = "%.0f".format(speedKmh)
                if (remainingKm > 0 && etaMin > 0) {
                    append("⏱️ Speed: $spd km/h • ETA: ~$etaMin min (${"%.1f".format(remainingKm)} km left)")
                } else if (remainingKm > 0) {
                    append("⏱️ Speed: $spd km/h • ${"%.1f".format(remainingKm)} km left")
                } else {
                    append("⏱️ Speed: $spd km/h • $progressPercent% completed")
                }
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_marg)
            .setColor(0xFF0E3B43.toInt())
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(!isFinished)

        if (!isFixedMode && !isFinished) {
            builder.setProgress(100, progressPercent, false)
        }

        if (!isFinished) {
            val pauseResumeIcon = if (isPaused) R.drawable.ic_action_play else R.drawable.ic_action_pause
            val pauseResumeTitle = if (isPaused) "Resume" else "Pause"
            builder.addAction(
                NotificationCompat.Action.Builder(
                    pauseResumeIcon,
                    pauseResumeTitle,
                    pauseResume
                ).build()
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_action_stop,
                    "Stop",
                    stop
                ).build()
            )
        }

        return builder.build()
    }

    private fun updateNotification(contentText: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIF_ID, buildNotification(contentText))
    }

    private fun updateLiveNotification(
        contentText: String,
        currentProgress: Float,
        remainingKm: Double,
        etaMin: Int
    ) {
        lastRemainingKm = remainingKm
        lastEtaMin = etaMin
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIF_ID, buildNotification(contentText, currentProgress, remainingKm, etaMin))
    }

    private fun startForegroundCompat() {
        val n = buildNotification(if (isFixedMode) "Indoor walking presence active" else "Tap to open, or control the simulation")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onDestroy() {
        job?.cancel()
        removeProviders()
        scope.cancel()
        if (state.value == SimState.RUNNING || state.value == SimState.PAUSED) state.value = SimState.IDLE
        super.onDestroy()
    }

    private fun haversine(a: P, b: P): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun bearing(a: P, b: P): Float {
        val p1 = Math.toRadians(a.lat)
        val p2 = Math.toRadians(b.lat)
        val dl = Math.toRadians(b.lon - a.lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    companion object {
        const val ACTION_START = "com.rahulbaberwal.marg.START"
        const val ACTION_PAUSE = "com.rahulbaberwal.marg.PAUSE"
        const val ACTION_RESUME = "com.rahulbaberwal.marg.RESUME"
        const val ACTION_STOP = "com.rahulbaberwal.marg.STOP"
        const val ACTION_RESET = "com.rahulbaberwal.marg.RESET"
        private const val CHANNEL = "marg_live_route_v1"
        private const val NOTIF_ID = 1

        val state = MutableStateFlow(SimState.IDLE)
        val position = MutableStateFlow<P?>(null)
        val progress = MutableStateFlow(0f)
        val error = MutableSharedFlow<String>(extraBufferCapacity = 1)

        @Volatile var isFixedMode: Boolean = false
        @Volatile var fixedLocation: P? = null
        @Volatile var indoorRadiusM: Double = 10.0
        @Volatile var speedKmh: Double = 60.0
        @Volatile var jitter: Boolean = true
        @Volatile var route: List<P> = emptyList()

        @Volatile var startLabel: String = "Start"
        @Volatile var endLabel: String = "Destination"
        @Volatile var modeName: String = "Drive"
        @Volatile var lastRemainingKm: Double = 0.0
        @Volatile var lastEtaMin: Int = 0
    }
}
