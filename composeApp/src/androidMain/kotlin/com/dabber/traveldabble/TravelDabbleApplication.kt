package com.dabber.traveldabble

import android.app.Application
import android.util.Log

/**
 * Android Application class to guarantee safe early initialization
 * and prevent native library / SDK startup crashes in release builds.
 */
class TravelDabbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Capture and log any unexpected top-level exceptions to Android Logcat
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TravelDabble", "CRITICAL UNCAUGHT EXCEPTION on thread '${thread.name}': ${throwable.message}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }

        try {
            org.maplibre.android.MapLibre.getInstance(this)
        } catch (t: Throwable) {
            Log.w("TravelDabble", "MapLibre early initialization notice: ${t.message}")
        }
    }
}
