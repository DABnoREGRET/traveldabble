package com.dabber.traveldabble

import android.app.Activity

/**
 * Held by the Android [Activity] (see MainActivity) so the shared UI layer can
 * trigger the runtime permission flow without coupling commonMain to
 * android.app.Activity.
 */
var activityReference: Activity? = null

actual fun requestLocationPermissionFromContext() {
    activityReference?.let { LocationPermissionHelper.requestLocationPermission(it) }
}
