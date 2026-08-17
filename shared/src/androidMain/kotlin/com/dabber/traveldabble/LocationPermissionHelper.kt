package com.dabber.traveldabble

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

/**
 * Requests the runtime location permissions (ACCESS_FINE_LOCATION /
 * ACCESS_COARSE_LOCATION) from the given [activity].
 *
 * Uses the platform Activity API (`checkSelfPermission`/`requestPermissions`,
 * both available since API 23; minSdk is 24), so no extra androidx.core
 * dependency is required. Both permissions are requested together so a single
 * system dialog is shown.
 */
object LocationPermissionHelper {

    private const val LocationRequestCode = 1001

    fun requestLocationPermission(activity: Activity) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        val missing = permissions.filter {
            activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missing.isNotEmpty()) {
            activity.requestPermissions(missing, LocationRequestCode)
        }
    }
}
