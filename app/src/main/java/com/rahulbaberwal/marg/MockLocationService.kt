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
        val ch = NotificationChannel(CHANNEL, "Mock location", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin()
            ACTION_PAUSE -> if (state.value == SimState.RUNNING) state.value = SimState.PAUSED
            ACTION_RESUME -> if (state.value == SimState.PAUSED) state.value = SimState.RUNNING
            ACTION_STOP -> shutdown(SimState.IDLE)
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun begin() {
        startForegroundCompat()
        val pts = route
        if (pts.size < 2) {
            error.tryEmit("No route loaded")
            shutdown(SimState.IDLE); return
        }
        if (!setupProviders()) {
            error.tryEmit("mock_not_selected")
            shutdown(SimState.IDLE); return
        }
        job?.cancel()
        job = scope.launch { simulate(pts) }
    }

    private suspend fun simulate(pts: List<P>) {
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + haversine(pts[i - 1], pts[i])
        val total = cum.last()
        var dist = 0.0
        var i = 0
        var lastBearing = bearing(pts[0], pts[1])
        state.value = SimState.RUNNING
        progress.value = 0f
        push(pts[0], lastBearing, 0f)

        while (true) {
            delay(1000)
            if (state.value != SimState.RUNNING) continue
            val mps = speedKmh / 3.6
            dist += mps
            if (dist >= total) {
                push(pts.last(), lastBearing, 0f)
                progress.value = 1f
                shutdown(SimState.FINISHED)
                return
            }
            while (i < pts.size - 2 && cum[i + 1] < dist) i++
            val a = pts[i]
            val b = pts[i + 1]
            val seg = cum[i + 1] - cum[i]
            val t = if (seg > 0) (dist - cum[i]) / seg else 0.0
            val cur = P(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
            if (seg > 0.5) lastBearing = bearing(a, b)
            progress.value = (dist / total).toFloat()
            push(cur, lastBearing, mps.toFloat())
        }
    }

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

    private fun push(p: P, bearing: Float, speedMps: Float) {
        var lat = p.lat
        var lon = p.lon
        if (jitter) {
            lat += (Random.nextDouble() - 0.5) * 1.4e-5
            lon += (Random.nextDouble() - 0.5) * 1.4e-5
        }
        for (name in providers) {
            val l = Location(name).apply {
                latitude = lat
                longitude = lon
                altitude = 0.0
                accuracy = if (jitter) 3f + Random.nextFloat() * 4f else 3f
                speed = speedMps
                this.bearing = bearing
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            try { lm.setTestProviderLocation(name, l) } catch (_: Exception) {}
        }
        position.value = p
    }

    private fun shutdown(final: SimState) {
        job?.cancel()
        removeProviders()
        position.value = null
        state.value = final
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeProviders() {
        for (p in providers) try { lm.removeTestProvider(p) } catch (_: Exception) {}
    }

    private fun startForegroundCompat() {
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_marg)
            .setContentTitle("Marg is mocking your location")
            .setContentText("Tap to open, or stop the simulation")
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
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
        position.value = null
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
        private const val CHANNEL = "marg_mock"
        private const val NOTIF_ID = 1

        val state = MutableStateFlow(SimState.IDLE)
        val position = MutableStateFlow<P?>(null)
        val progress = MutableStateFlow(0f)
        val error = MutableSharedFlow<String>(extraBufferCapacity = 1)

        @Volatile var speedKmh: Double = 40.0
        @Volatile var jitter: Boolean = true
        @Volatile var route: List<P> = emptyList()
    }
}
