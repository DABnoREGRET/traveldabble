package com.dabber.traveldabble.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardTravel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val Onboarding = "onboarding"
    const val Login = "login"
    const val Home = "home"
    const val Trips = "trips"
    const val Map = "map"
    const val Ai = "ai"
    const val Profile = "profile"
    const val Explore = "explore"
    const val CreateTrip = "createTrip"
    const val TripDetail = "trip/{tripId}"
    const val Itinerary = "itinerary/{tripId}"
    const val Budget = "budget/{tripId}"
    const val TripMap = "map/{tripId}"
    const val PlaceDetail = "place/{placeId}"
    const val GroupTrip = "group/{tripId}"

    // Settings routes
    const val SettingsAppearance = "settings/appearance"
    const val SettingsNotifications = "settings/notifications"
    const val SettingsMap = "settings/map"
    const val SettingsPrivacy = "settings/privacy"
    const val SettingsAccount = "settings/account"
    const val SettingsAppInfo = "settings/appinfo"

    fun tripDetail(tripId: String) = "trip/$tripId"
    fun itinerary(tripId: String) = "itinerary/$tripId"
    fun budget(tripId: String) = "budget/$tripId"
    fun tripMap(tripId: String) = "map/$tripId"
    fun placeDetail(placeId: String) = "place/$placeId"
    fun groupTrip(tripId: String) = "group/$tripId"
}

data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomTabs = listOf(
    BottomTab(Routes.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Routes.Trips, "Trips", Icons.Filled.CardTravel, Icons.Outlined.CardTravel),
    BottomTab(Routes.Map, "Map", Icons.Filled.Map, Icons.Outlined.Map),
    BottomTab(Routes.Ai, "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomTab(Routes.Profile, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)
