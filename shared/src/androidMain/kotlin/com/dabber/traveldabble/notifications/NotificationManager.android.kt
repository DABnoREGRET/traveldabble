package com.dabber.traveldabble.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dabber.traveldabble.data.ApiClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class FcmTokenRequest(val token: String, val platform: String = "android")

actual class NotificationManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val channelId = "traveldabble_notifications"
    private val channelName = "TravelDabble"

    actual suspend fun initialize() {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                AndroidNotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Trip reminders, collaboration updates, and travel alerts"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    actual suspend fun requestPermission(): Boolean {
        return true
    }

    actual suspend fun registerToken(token: String) {
        try {
            val body = json.encodeToString(FcmTokenRequest(token))
            ApiClient.httpClient.post("${ApiClient.baseUrl}/api/fcm/register") {
                contentType(ContentType.Application.Json)
                setBody(body)
                val authToken = ApiClient.getToken()
                if (authToken != null) {
                    header("Authorization", "Bearer $authToken")
                }
            }
        } catch (_: Exception) {
            // FCM registration must never crash the app
        }
    }

    actual suspend fun unregisterToken() {
        try {
            ApiClient.httpClient.delete("${ApiClient.baseUrl}/api/fcm/unregister") {
                val authToken = ApiClient.getToken()
                if (authToken != null) {
                    header("Authorization", "Bearer $authToken")
                }
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    actual fun showLocalNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
