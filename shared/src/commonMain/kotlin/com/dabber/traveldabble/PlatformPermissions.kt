package com.dabber.traveldabble

/**
 * Requests the runtime location permissions (ACCESS_FINE_LOCATION /
 * ACCESS_COARSE_LOCATION) on platforms that support them.
 *
 * The actual implementation lives in androidMain and fires the runtime
 * permission dialog through the Activity registered via [activityReference];
 * the jvmMain implementation is a no-op (desktop has no runtime permissions).
 */
expect fun requestLocationPermissionFromContext()
