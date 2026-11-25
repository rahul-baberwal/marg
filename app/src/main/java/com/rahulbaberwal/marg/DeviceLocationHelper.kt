package com.rahulbaberwal.marg

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object DeviceLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission(context)) return@withContext null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        // 1. Try to get best last known location first (fast instant response)
        val gpsLast = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: SecurityException) { null }
        val netLast = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: SecurityException) { null }
        val passiveLast = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: SecurityException) { null }

        val bestLast = listOfNotNull(gpsLast, netLast, passiveLast).maxByOrNull { it.time }

        // If last known location is very fresh (< 45 seconds old), return it immediately
        if (bestLast != null && (System.currentTimeMillis() - bestLast.time) < 45_000L) {
            return@withContext bestLast
        }

        // 2. Request a fresh location fix with a 6-second timeout
        val freshLocation = withTimeoutOrNull(6000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val cancellationSignal = CancellationSignal()
                    cont.invokeOnCancellation { cancellationSignal.cancel() }
                    try {
                        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            LocationManager.NETWORK_PROVIDER
                        } else {
                            LocationManager.PASSIVE_PROVIDER
                        }
                        lm.getCurrentLocation(
                            provider,
                            cancellationSignal,
                            context.mainExecutor
                        ) { loc ->
                            if (cont.isActive) cont.resume(loc ?: bestLast)
                        }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(bestLast)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            try { lm.removeUpdates(this) } catch (_: Exception) {}
                            if (cont.isActive) cont.resume(loc)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    cont.invokeOnCancellation {
                        try { lm.removeUpdates(listener) } catch (_: Exception) {}
                    }
                    try {
                        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else {
                            LocationManager.NETWORK_PROVIDER
                        }
                        lm.requestSingleUpdate(provider, listener, null)
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(bestLast)
                    }
                }
            }
        }

        freshLocation ?: bestLast
    }
}
