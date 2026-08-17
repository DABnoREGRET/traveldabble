package com.dabber.traveldabble.notifications

/**
 * Expect/actual notification manager for push notifications.
 * Android uses Firebase Cloud Messaging.
 * JVM is a placeholder for desktop.
 */
expect class NotificationManager {
    suspend fun initialize()
    suspend fun requestPermission(): Boolean
    suspend fun registerToken(token: String)
    suspend fun unregisterToken()
    fun showLocalNotification(title: String, body: String, data: Map<String, String> = emptyMap())
}
