package com.rahulbaberwal.marg

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings

/**
 * Helper to inspect Developer Options status, check Mock Location selection,
 * and direct the user straight to "Select mock location app" preference.
 */
object DeveloperSettingsHelper {

    /**
     * Checks if Developer Options is currently enabled on the device.
     */
    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if Marg is selected as the mock location app in Developer Options.
     * Uses AppOps check first, with a test-provider probe fallback.
     */
    @Suppress("DEPRECATION")
    fun isMockLocationAppSelected(context: Context): Boolean {
        // Fast check via AppOps
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        if (appOps != null) {
            try {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        context.packageName
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return true
                }
            } catch (_: Exception) {}
        }

        // Direct probe fallback via LocationManager test provider
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val probeProvider = "marg_probe"
        return try {
            try { lm.removeTestProvider(probeProvider) } catch (_: Exception) {}
            lm.addTestProvider(
                probeProvider,
                false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            lm.removeTestProvider(probeProvider)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Directly opens Developer Options targeting "Select mock location app" preference.
     * If developer options is disabled, opens "About Phone" so the user can enable it.
     */
    fun openMockLocationSettings(context: Context): Boolean {
        if (!isDeveloperOptionsEnabled(context)) {
            return openAboutPhoneSettings(context)
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", "mock_location_app")
            val bundle = Bundle().apply {
                putString(":settings:fragment_args_key", "mock_location_app")
            }
            putExtra(":settings:show_fragment_args", bundle)
            putExtra("extra_prefs_key", "mock_location_app")
            putExtra("extra_fragment_arg_key", "mock_location_app")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Opens "About phone" settings screen to allow user to tap "Build number" 7 times.
     */
    fun openAboutPhoneSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
