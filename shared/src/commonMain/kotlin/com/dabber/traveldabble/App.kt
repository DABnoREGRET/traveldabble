package com.dabber.traveldabble

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.glass.AuroraBackground
import com.dabber.traveldabble.ui.navigation.GlassBottomBar
import com.dabber.traveldabble.ui.navigation.OnboardingState
import com.dabber.traveldabble.ui.navigation.Routes
import com.dabber.traveldabble.ui.navigation.ScrollState
import com.dabber.traveldabble.ui.navigation.bottomTabs
import com.dabber.traveldabble.ui.screens.AiChatScreen
import com.dabber.traveldabble.ui.screens.AppearanceSettingsScreen
import com.dabber.traveldabble.ui.screens.AppInfoScreen
import com.dabber.traveldabble.ui.screens.BudgetScreen
import com.dabber.traveldabble.ui.screens.CreateTripScreen
import com.dabber.traveldabble.ui.screens.ExploreScreen
import com.dabber.traveldabble.ui.screens.GroupTripScreen
import com.dabber.traveldabble.ui.screens.HomeScreen
import com.dabber.traveldabble.ui.screens.ItineraryScreen
import com.dabber.traveldabble.ui.screens.LoginScreen
import com.dabber.traveldabble.ui.screens.MapScreen
import com.dabber.traveldabble.ui.screens.MapSettingsScreen
import com.dabber.traveldabble.ui.screens.NotificationSettingsScreen
import com.dabber.traveldabble.ui.screens.OnboardingScreen
import com.dabber.traveldabble.ui.screens.PlaceDetailScreen
import com.dabber.traveldabble.ui.screens.PrivacySettingsScreen
import com.dabber.traveldabble.ui.screens.AccountSettingsScreen
import com.dabber.traveldabble.ui.screens.ProfileScreen
import com.dabber.traveldabble.ui.screens.TripDetailScreen
import com.dabber.traveldabble.ui.screens.TripsScreen
import com.dabber.traveldabble.ui.theme.TravelDabbleTheme

@Composable
fun App() {
    // Sync settings with AuthState on startup
    androidx.compose.runtime.LaunchedEffect(Unit) {
        SettingsState.syncWithAuthState()
    }

    TravelDabbleTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = isBottomTabRoute(currentRoute)

        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground()
            TravelNavHost(navController)
            if (showBottomBar) {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        if (currentRoute == route) return@GlassBottomBar
                        ScrollState.show()
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = ScrollState.isBarVisible || currentRoute == Routes.Ai,
                )
            }
        }
    }
}

@Composable
private fun TravelNavHost(navController: NavHostController) {
    val startDestination = when {
        !OnboardingState.hasCompletedOnboarding -> Routes.Onboarding
        else -> Routes.Home // Always start at Home, guest or logged in
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            val isTabSwitch = isBottomTabRoute(initialState.destination.route) && isBottomTabRoute(targetState.destination.route)
            if (isTabSwitch) {
                fadeIn(animationSpec = tween(durationMillis = 180))
            } else {
                slideInHorizontally(
                    initialOffsetX = { (it * 0.15f).toInt() },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            }
        },
        exitTransition = {
            val isTabSwitch = isBottomTabRoute(initialState.destination.route) && isBottomTabRoute(targetState.destination.route)
            if (isTabSwitch) {
                fadeOut(animationSpec = tween(durationMillis = 140))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -(it * 0.10f).toInt() },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        },
        popEnterTransition = {
            val isTabSwitch = isBottomTabRoute(initialState.destination.route) && isBottomTabRoute(targetState.destination.route)
            if (isTabSwitch) {
                fadeIn(animationSpec = tween(durationMillis = 180))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -(it * 0.10f).toInt() },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            }
        },
        popExitTransition = {
            val isTabSwitch = isBottomTabRoute(initialState.destination.route) && isBottomTabRoute(targetState.destination.route)
            if (isTabSwitch) {
                fadeOut(animationSpec = tween(durationMillis = 140))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            }
        },
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    OnboardingState.complete()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
                onLogin = {
                    OnboardingState.complete()
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
                onRequestLocationPermission = {
                    requestLocationPermissionFromContext()
                },
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                onTripClick = { navController.navigate(Routes.tripDetail(it)) },
                onCreateTrip = { navController.navigate(Routes.CreateTrip) },
                onAskAi = { navController.navigate(Routes.Ai) },
                onOpenMap = { navController.navigate(Routes.Map) },
                onExplore = { navController.navigate(Routes.Explore) },
                onSeeAllTrips = { navController.navigate(Routes.Trips) },
                onDestinationClick = { navController.navigate(Routes.placeDetail(it)) },
                onSearch = { navController.navigate(Routes.Explore) },
                onProfile = { navController.navigate(Routes.Profile) },
            )
        }
        composable(Routes.Trips) {
            TripsScreen(
                onTripClick = { navController.navigate(Routes.tripDetail(it)) },
                onCreateTrip = { navController.navigate(Routes.CreateTrip) },
                onRequireLogin = { navController.navigate(Routes.Login) },
            )
        }
        composable(Routes.Map) {
            MapScreen(onPlaceClick = { navController.navigate(Routes.placeDetail(it)) })
        }
        composable(
            route = "map?lat={lat}&lng={lng}&placeId={placeId}",
        ) { entry ->
            val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = entry.arguments?.getString("lng")?.toDoubleOrNull()
            val placeId = entry.arguments?.getString("placeId")
            MapScreen(
                initialLat = lat,
                initialLng = lng,
                focusPlaceId = placeId,
                onPlaceClick = { navController.navigate(Routes.placeDetail(it)) },
            )
        }
        composable(Routes.Ai) {
            AiChatScreen(
                tripId = "general",
                onNavigate = { screen, tripId ->
                    when (screen) {
                        "home" -> {
                            ScrollState.show()
                            navController.navigate(Routes.Home) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "trips" -> {
                            ScrollState.show()
                            navController.navigate(Routes.Trips) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "map" -> {
                            ScrollState.show()
                            navController.navigate(Routes.Map) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "explore" -> {
                            ScrollState.show()
                            navController.navigate(Routes.Explore)
                        }
                        "profile" -> {
                            ScrollState.show()
                            navController.navigate(Routes.Profile) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "ai_settings" -> navController.navigate(Routes.SettingsAccount)
                        "account" -> navController.navigate(Routes.SettingsAccount)
                        "trip_detail" -> {
                            val targetTripId = tripId ?: "demo_hanoi_01"
                            navController.navigate(Routes.tripDetail(targetTripId))
                        }
                        "itinerary" -> {
                            val targetTripId = tripId ?: "demo_hanoi_01"
                            navController.navigate(Routes.itinerary(targetTripId))
                        }
                        "budget" -> {
                            val targetTripId = tripId ?: "demo_hanoi_01"
                            navController.navigate(Routes.budget(targetTripId))
                        }
                        "group_trip" -> {
                            val targetTripId = tripId ?: "demo_hanoi_01"
                            navController.navigate(Routes.groupTrip(targetTripId))
                        }
                        "create_trip" -> navController.navigate(Routes.CreateTrip)
                    }
                },
            )
        }
        composable(Routes.Profile) {
            ProfileScreen(
                onLogout = { AuthState.onLogout() },
                onSignIn = { navController.navigate(Routes.Login) },
                onNavigateToAppearance = { navController.navigate(Routes.SettingsAppearance) },
                onNavigateToNotifications = { navController.navigate(Routes.SettingsNotifications) },
                onNavigateToMap = { navController.navigate(Routes.SettingsMap) },
                onNavigateToPrivacy = { navController.navigate(Routes.SettingsPrivacy) },
                onNavigateToAccount = { navController.navigate(Routes.SettingsAccount) },
                onNavigateToAppInfo = { navController.navigate(Routes.SettingsAppInfo) },
                onReplayOnboarding = { navController.navigate(Routes.Onboarding) },
            )
        }
        composable(Routes.Explore) {
            ExploreScreen(
                onBack = { navController.popBackStack() },
                onDestinationClick = { navController.navigate(Routes.placeDetail(it)) },
            )
        }
        composable(Routes.CreateTrip) {
            CreateTripScreen(
                onBack = { navController.popBackStack() },
                onCreated = { newTripId ->
                    navController.navigate(Routes.tripDetail(newTripId)) {
                        popUpTo(Routes.CreateTrip) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TripDetail) { entry ->
            val tripId = entry.stringArg("tripId").orEmpty()
            TripDetailScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onOpenItinerary = { navController.navigate(Routes.itinerary(it)) },
                onOpenMap = { navController.navigate(Routes.tripMap(it)) },
                onOpenBudget = { navController.navigate(Routes.budget(it)) },
                onAskAi = { navController.navigate(Routes.Ai) },
                onOpenGroup = { navController.navigate(Routes.groupTrip(it)) },
            )
        }
        composable(Routes.Itinerary) { entry ->
            val tripId = entry.stringArg("tripId").orEmpty()
            ItineraryScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onPlaceClick = { navController.navigate(Routes.placeDetail(it)) },
            )
        }
        composable(Routes.Budget) { entry ->
            val tripId = entry.stringArg("tripId").orEmpty()
            BudgetScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TripMap) { entry ->
            val tripId = entry.stringArg("tripId").orEmpty()
            MapScreen(
                tripId = tripId,
                onPlaceClick = { navController.navigate(Routes.placeDetail(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PlaceDetail) { entry ->
            val placeId = entry.stringArg("placeId").orEmpty()
            PlaceDetailScreen(
                placeId = placeId,
                onBack = { navController.popBackStack() },
                onPlaceClick = { targetPlaceId ->
                    navController.navigate(Routes.placeDetail(targetPlaceId))
                },
                onNavigateToMap = { lat, lng, targetPlaceId ->
                    navController.navigate(Routes.mapWithLocation(lat, lng, targetPlaceId)) {
                        popUpTo(Routes.Home)
                    }
                },
                onNavigateToPlanTrip = { _ ->
                    navController.navigate(Routes.CreateTrip)
                },
                onNavigateToTripDetail = { newTripId ->
                    navController.navigate(Routes.tripDetail(newTripId))
                },
            )
        }
        composable(Routes.GroupTrip) { entry ->
            val tripId = entry.stringArg("tripId").orEmpty()
            GroupTripScreen(
                tripId = tripId,
                tripTitle = "Trip", // Could load from API
                onBack = { navController.popBackStack() },
            )
        }

        // Settings routes
        composable(Routes.SettingsAppearance) {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsNotifications) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsMap) {
            MapSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsPrivacy) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsAccount) {
            AccountSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SettingsAppInfo) {
            AppInfoScreen(
                onBack = { navController.popBackStack() },
                onReplayOnboarding = { navController.navigate(Routes.Onboarding) },
            )
        }
    }
}

/**
 * Cross-platform read of a string route argument (e.g. "trip/{tripId}").
 *
 * The underlying [androidx.navigation.NavBackStackEntry.arguments] is an
 * [androidx.savedstate.SavedState]; on Android this is Bundle-backed and on
 * desktop/JVM it is map-backed. Reading through [androidx.savedstate.read] keeps
 * the extraction working on every target.
 */
private fun NavBackStackEntry.stringArg(key: String): String? =
    arguments?.getString(key)

/**
 * The bottom bar is visible only on the five tab destinations (Home, Trips,
 * Map, AI, Profile).
 */
private fun isBottomTabRoute(route: String?): Boolean =
    route != null && (bottomTabs.any { tab -> route == tab.route } || route.startsWith("map?"))
