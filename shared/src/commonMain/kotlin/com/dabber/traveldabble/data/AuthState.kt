package com.dabber.traveldabble.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class User(
    val id: String,
    val displayName: String,
    val email: String,
)

/**
 * Persistent auth state using multiplatform Settings (Android SharedPreferences).
 * Token and login state survive app restarts.
 */
object AuthState {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val kvSettings by lazy {
        try {
            Settings()
        } catch (_: Throwable) {
            null
        }
    }

    private const val AUTH_KEY = "traveldabble_auth_data"

    @Serializable
    private data class AuthData(
        val token: String? = null,
        val userId: String? = null,
        val displayName: String? = null,
        val email: String? = null,
    )

    var isLoggedIn by mutableStateOf(false)
        private set
    var isGuestMode by mutableStateOf(true)
        private set
    var currentUser by mutableStateOf<User?>(null)
        private set
    var authToken by mutableStateOf<String?>(null)
        private set

    /** Telemetry opt-out preference. When true, client sends X-Telemetry-Opt-Out header. */
    var telemetryOptOut by mutableStateOf(false)
        private set

    /** User-provided OpenRouter API key for BYOK AI. Null = use server-hosted key. */
    var openRouterApiKey by mutableStateOf<String?>(null)
        private set

    /** Selected AI Model for OpenRouter (used especially when BYOK is active). */
    var selectedAiModel by mutableStateOf("google/gemma-4-26b-a4b-it:free")
        private set

    init {
        restoreAuth()
    }

    private fun restoreAuth() {
        try {
            val stored = kvSettings?.getStringOrNull(AUTH_KEY)
            if (!stored.isNullOrBlank()) {
                val data = json.decodeFromString<AuthData>(stored)
                if (data.token != null && data.userId != null) {
                    authToken = data.token
                    currentUser = User(data.userId, data.displayName ?: "", data.email ?: "")
                    isLoggedIn = true
                    isGuestMode = false
                    ApiClient.setToken(data.token)
                }
            }
        } catch (_: Throwable) {
            // Start fresh if corrupted
        }
    }

    private fun saveAuth() {
        try {
            val data = AuthData(
                token = authToken,
                userId = currentUser?.id,
                displayName = currentUser?.displayName,
                email = currentUser?.email,
            )
            kvSettings?.putString(AUTH_KEY, json.encodeToString(data))
        } catch (_: Throwable) {
            // Auth persistence must never crash the app
        }
    }

    private fun clearAuth() {
        try {
            kvSettings?.remove(AUTH_KEY)
        } catch (_: Throwable) {}
    }

    fun updateTelemetryOptOut(enabled: Boolean) {
        telemetryOptOut = enabled
        ApiClient.telemetryOptOut = enabled
    }

    fun updateOpenRouterApiKey(key: String?) {
        openRouterApiKey = sanitizeApiKey(key)
    }

    fun updateSelectedAiModel(model: String) {
        selectedAiModel = model
    }

    fun onLoginSuccess(response: AuthResponse) {
        authToken = response.token
        currentUser = User(response.userId, response.displayName, response.email)
        isLoggedIn = true
        isGuestMode = false
        ApiClient.setToken(response.token)
        saveAuth()
    }

    fun onLogout() {
        authToken = null
        currentUser = null
        isLoggedIn = false
        isGuestMode = true
        ApiClient.setToken(null)
        clearAuth()
    }
}
