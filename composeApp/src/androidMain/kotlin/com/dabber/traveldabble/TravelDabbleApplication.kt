package com.dabber.traveldabble

import android.app.Application
import org.maplibre.android.MapLibre

/**
 * Android Application class to guarantee safe early initialization
 * and prevent native library / SDK startup crashes in release builds.
 */
class TravelDabbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MapLibre.getInstance(this)
        } catch (_: Throwable) {
            // Graceful fallback if native binaries (.so) fail to load on a specific device
        }
    }
}
