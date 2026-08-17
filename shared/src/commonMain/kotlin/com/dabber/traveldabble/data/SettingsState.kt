package com.dabber.traveldabble.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Theme mode selection.
 */
@Serializable
enum class ThemeMode(val label: String, val description: String) {
    System("System default", "Follow your device's appearance settings"),
    Dark("Dark mode", "Deep obsidian canvas with subtle aurora glow"),
    Light("Light mode", "Crisp, airy layout with high readability"),
}

/**
 * Map default style selection.
 */
@Serializable
enum class MapStyleSetting(val label: String) {
    Liberty("Liberty"),
    Positron("Positron"),
    Bright("Bright"),
    Dark("Dark"),
    Fiord("Fiord"),
}

/**
 * Unit system preference.
 */
@Serializable
enum class UnitSystem(val label: String) {
    Metric("Metric (km, kg)"),
    Imperial("Imperial (mi, lb)"),
}

/**
 * Language preference.
 */
@Serializable
enum class LanguageSetting(val label: String, val code: String) {
    English("English", "en"),
    Vietnamese("Tiếng Việt", "vi"),
    Japanese("日本語", "ja"),
    Korean("한국어", "ko"),
    French("Français", "fr"),
}

/**
 * All user preferences in one serializable bundle.
 */
@Serializable
data class UserPreferences(
    // Theme
    val themeMode: ThemeMode = ThemeMode.System,

    // Demo Mode: when true, provides sample trips & destinations; when false, starts clean
    val demoMode: Boolean = true,

    // Notifications
    val tripReminders: Boolean = true,
    val collaborationUpdates: Boolean = true,
    val dealAlerts: Boolean = false,
    val checkInReminders: Boolean = true,

    // Map
    val defaultMapStyle: MapStyleSetting = MapStyleSetting.Liberty,
    val map3DEnabled: Boolean = true,
    val defaultToShowLocation: Boolean = true,

    // AI
    val openRouterApiKey: String? = null,
    val selectedAiModel: String = "google/gemma-4-26b-a4b-it:free",
    val aiSuggestionsEnabled: Boolean = true,

    // Preferences
    val unitSystem: UnitSystem = UnitSystem.Metric,
    val language: LanguageSetting = LanguageSetting.English,

    // Privacy
    val telemetryOptOut: Boolean = false,
    val analyticsEnabled: Boolean = true,

    // Network / Server Connection
    val customServerUrl: String? = null,

    // Onboarding
    val hasCompletedOnboarding: Boolean = false,
    val hasSeenNotificationsPrompt: Boolean = false,
)

/**
 * Persistent settings state using multiplatform-settings (Android SharedPreferences).
 * Token and settings reliably survive app restarts.
 */
object SettingsState {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val kvSettings by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            null
        }
    }

    private const val PREFS_KEY = "traveldabble_preferences"

    // Observable state
    var preferences by mutableStateOf(loadPreferences())
        private set

    // Convenience accessors
    val themeMode: ThemeMode get() = preferences.themeMode
    val demoMode: Boolean get() = preferences.demoMode
    val tripReminders: Boolean get() = preferences.tripReminders
    val collaborationUpdates: Boolean get() = preferences.collaborationUpdates
    val dealAlerts: Boolean get() = preferences.dealAlerts
    val checkInReminders: Boolean get() = preferences.checkInReminders
    val defaultMapStyle: MapStyleSetting get() = preferences.defaultMapStyle
    val map3DEnabled: Boolean get() = preferences.map3DEnabled
    val defaultToShowLocation: Boolean get() = preferences.defaultToShowLocation
    val openRouterApiKey: String? get() = preferences.openRouterApiKey
    val selectedAiModel: String get() = preferences.selectedAiModel
    val aiSuggestionsEnabled: Boolean get() = preferences.aiSuggestionsEnabled
    val unitSystem: UnitSystem get() = preferences.unitSystem
    val language: LanguageSetting get() = preferences.language
    val telemetryOptOut: Boolean get() = preferences.telemetryOptOut
    val analyticsEnabled: Boolean get() = preferences.analyticsEnabled
    val customServerUrl: String? get() = preferences.customServerUrl
    val hasCompletedOnboarding: Boolean get() = preferences.hasCompletedOnboarding
    val hasSeenNotificationsPrompt: Boolean get() = preferences.hasSeenNotificationsPrompt

    private fun loadPreferences(): UserPreferences {
        return try {
            val stored = kvSettings?.getStringOrNull(PREFS_KEY)
            if (!stored.isNullOrBlank()) {
                json.decodeFromString<UserPreferences>(stored)
            } else {
                UserPreferences()
            }
        } catch (_: Exception) {
            UserPreferences()
        }
    }

    private fun savePreferences() {
        try {
            val serialized = json.encodeToString(preferences)
            kvSettings?.putString(PREFS_KEY, serialized)
        } catch (_: Exception) {
            // Settings persistence must never crash the app
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        preferences = preferences.copy(themeMode = mode)
        savePreferences()
    }

    fun updateDemoMode(enabled: Boolean) {
        preferences = preferences.copy(demoMode = enabled)
        savePreferences()
    }

    fun updateTripReminders(enabled: Boolean) {
        preferences = preferences.copy(tripReminders = enabled)
        savePreferences()
    }

    fun updateCollaborationUpdates(enabled: Boolean) {
        preferences = preferences.copy(collaborationUpdates = enabled)
        savePreferences()
    }

    fun updateDealAlerts(enabled: Boolean) {
        preferences = preferences.copy(dealAlerts = enabled)
        savePreferences()
    }

    fun updateCheckInReminders(enabled: Boolean) {
        preferences = preferences.copy(checkInReminders = enabled)
        savePreferences()
    }

    fun updateDefaultMapStyle(style: MapStyleSetting) {
        preferences = preferences.copy(defaultMapStyle = style)
        savePreferences()
    }

    fun updateMap3DEnabled(enabled: Boolean) {
        preferences = preferences.copy(map3DEnabled = enabled)
        savePreferences()
    }

    fun updateDefaultToShowLocation(enabled: Boolean) {
        preferences = preferences.copy(defaultToShowLocation = enabled)
        savePreferences()
    }

    fun updateOpenRouterApiKey(key: String?) {
        preferences = preferences.copy(openRouterApiKey = key?.takeIf { it.isNotBlank() })
        savePreferences()
        AuthState.updateOpenRouterApiKey(key)
    }

    fun updateSelectedAiModel(model: String) {
        preferences = preferences.copy(selectedAiModel = model)
        savePreferences()
        AuthState.updateSelectedAiModel(model)
    }

    fun updateAiSuggestionsEnabled(enabled: Boolean) {
        preferences = preferences.copy(aiSuggestionsEnabled = enabled)
        savePreferences()
    }

    fun updateUnitSystem(system: UnitSystem) {
        preferences = preferences.copy(unitSystem = system)
        savePreferences()
    }

    fun updateLanguage(lang: LanguageSetting) {
        preferences = preferences.copy(language = lang)
        savePreferences()
    }

    fun updateTelemetryOptOut(enabled: Boolean) {
        preferences = preferences.copy(telemetryOptOut = enabled)
        savePreferences()
        AuthState.updateTelemetryOptOut(enabled)
    }

    fun updateAnalyticsEnabled(enabled: Boolean) {
        preferences = preferences.copy(analyticsEnabled = enabled)
        savePreferences()
    }

    fun updateCustomServerUrl(url: String?) {
        val cleanUrl = url?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
        preferences = preferences.copy(customServerUrl = cleanUrl)
        savePreferences()
    }

    fun completeOnboarding() {
        preferences = preferences.copy(hasCompletedOnboarding = true)
        savePreferences()
    }

    fun resetOnboarding() {
        preferences = preferences.copy(hasCompletedOnboarding = false)
        savePreferences()
    }

    fun markNotificationsPromptSeen() {
        preferences = preferences.copy(hasSeenNotificationsPrompt = true)
        savePreferences()
    }

    /**
     * Sync settings state with AuthState on app startup.
     */
    fun syncWithAuthState() {
        AuthState.updateTelemetryOptOut(preferences.telemetryOptOut)
        AuthState.updateOpenRouterApiKey(preferences.openRouterApiKey)
        AuthState.updateSelectedAiModel(preferences.selectedAiModel)
    }
}

const val DEFAULT_AI_MODEL = "google/gemma-4-26b-a4b-it:free"

data class AiModelOption(
    val id: String,
    val name: String,
    val description: String = "",
    val isFree: Boolean = false,
)
