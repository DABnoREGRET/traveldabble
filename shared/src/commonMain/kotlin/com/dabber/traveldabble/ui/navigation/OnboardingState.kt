package com.dabber.traveldabble.ui.navigation

import com.dabber.traveldabble.data.SettingsState

/**
 * Onboarding state backed by SettingsState for persistence.
 * Survives app restarts via JSON file storage.
 */
object OnboardingState {
    var hasCompletedOnboarding: Boolean
        get() = SettingsState.hasCompletedOnboarding
        private set(value) {
            if (value) SettingsState.completeOnboarding()
            else SettingsState.resetOnboarding()
        }

    fun complete() {
        hasCompletedOnboarding = true
    }

    fun reset() {
        hasCompletedOnboarding = false
    }
}
