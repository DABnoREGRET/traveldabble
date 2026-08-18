package com.dabber.traveldabble.ui.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.PlaceCategory
import com.dabber.traveldabble.routing.RouteManager
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

// Vietnam default center coordinates (Da Nang / Central Vietnam)
private val VIETNAM_DEFAULT_CENTER = LatLng(16.0544, 108.2022)

/**
 * Generates an authentic Google Maps style teardrop pin marker with a pointed tip.
 * Anchor is at the bottom tip.
 */
private fun createGoogleMapsPinBitmap(pinColorHex: String, innerDotColorHex: String? = null): Bitmap {
    val width = 48
    val height = 64
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val pinColor = try {
        android.graphics.Color.parseColor(pinColorHex)
    } catch (_: Throwable) {
        android.graphics.Color.RED
    }
    val strokeColor = android.graphics.Color.parseColor("#0F172A")

    // Shadow at bottom tip
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(70, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(RectF(12f, 56f, 36f, 63f), shadowPaint)

    // Pin Body Path (Teardrop)
    val pinPath = Path().apply {
        arcTo(RectF(6f, 4f, 42f, 40f), 140f, 260f, false)
        lineTo(24f, 58f)
        close()
    }

    // Fill Pin
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinPaint)

    // Subtle dark border for high contrast on any map background
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    canvas.drawPath(pinPath, borderPaint)

    // Inner White Disc
    val innerWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(24f, 22f, 9f, innerWhitePaint)

    // Inner Accent Dot
    val dotColor = innerDotColorHex?.let {
        try { android.graphics.Color.parseColor(it) } catch (_: Throwable) { pinColor }
    } ?: pinColor
    val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(24f, 22f, 5.5f, innerDotPaint)

    return bmp
}

/**
 * Controller managing MapLibre managers and lifecycle cleanly across style switches and updates.
 */
private class MapFeatureHolder(val mapView: MapView) {
    var symbolManager: SymbolManager? = null
    var casingLineManager: LineManager? = null
    var lineManager: LineManager? = null
    var activeStyleUrl: String? = null

    fun resetManagers(map: MapLibreMap, loadedStyle: Style) {
        try {
            symbolManager?.onDestroy()
        } catch (_: Throwable) {}
        try {
            lineManager?.onDestroy()
        } catch (_: Throwable) {}
        try {
            casingLineManager?.onDestroy()
        } catch (_: Throwable) {}

        // Add category pin bitmaps into the newly loaded style
        val pinMap = mapOf(
            "pin_stay" to "#2563EB",       // Blue for Stay
            "pin_sight" to "#059669",      // Emerald for Sights
            "pin_food" to "#EA580C",       // Orange for Food
            "pin_activity" to "#7C3AED",   // Violet for Activities
            "pin_transit" to "#0D9488",    // Teal for Transit
            "pin_default" to "#DC2626",    // Red Google Maps pin default
        )
        for ((key, hex) in pinMap) {
            try {
                if (loadedStyle.getImage(key) == null) {
                    loadedStyle.addImage(key, createGoogleMapsPinBitmap(hex), false)
                }
            } catch (_: Throwable) {}
        }

        // Initialize casing line underneath main line for Google Maps road outline look
        casingLineManager = LineManager(mapView, map, loadedStyle).apply {
            lineCap = "round"
        }
        lineManager = LineManager(mapView, map, loadedStyle).apply {
            lineCap = "round"
        }

        symbolManager = SymbolManager(mapView, map, loadedStyle).apply {
            iconAllowOverlap = true
            iconIgnorePlacement = true
            addClickListener { symbol ->
                val placeId = symbol.data?.asString
                currentPlaces.firstOrNull { it.id == placeId }?.let { onMarkerClickListener?.invoke(it) }
                true
            }
        }
        activeStyleUrl = loadedStyle.uri
    }

    private var currentPlaces: List<Place> = emptyList()
    private var onMarkerClickListener: ((Place) -> Unit)? = null

    fun updatePlacesAndRoutes(
        places: List<Place>,
        showPolylines: Boolean,
        coroutineScope: CoroutineScope,
        onMarkerClick: (Place) -> Unit,
    ) {
        this.currentPlaces = places
        this.onMarkerClickListener = onMarkerClick
        val sm = symbolManager ?: return
        try {
            sm.deleteAll()
            places.forEach { place ->
                val iconKey = when (place.category) {
                    PlaceCategory.STAY -> "pin_stay"
                    PlaceCategory.SIGHT -> "pin_sight"
                    PlaceCategory.FOOD -> "pin_food"
                    PlaceCategory.ACTIVITY -> "pin_activity"
                    PlaceCategory.TRANSIT -> "pin_transit"
                    else -> "pin_default"
                }

                sm.create(
                    SymbolOptions()
                        .withLatLng(LatLng(place.lat, place.lng))
                        .withIconImage(iconKey)
                        .withIconAnchor("bottom")
                        .withIconSize(1.0f)
                ).also { it.data = JsonPrimitive(place.id) }
            }
        } catch (_: Throwable) {}

        // Roadway navigation route rendering
        val lm = lineManager
        val clm = casingLineManager
        if (lm != null && clm != null) {
            try {
                lm.deleteAll()
                clm.deleteAll()

                if (showPolylines && places.size >= 2) {
                    val waypoints = places.map { it.lat to it.lng }
                    coroutineScope.launch {
                        // Resolve actual turn-by-turn road geometry
                        val roadwayCoords = withContext(Dispatchers.Default) {
                            RouteManager.getRoadwayCoordinates(waypoints, profile = "driving")
                        }
                        val mapPoints = roadwayCoords.map { LatLng(it.first, it.second) }

                        // Draw Google Maps style road route with outer casing + glowing emerald road fill
                        try {
                            clm.deleteAll()
                            lm.deleteAll()

                            // 1. Darker casing outline
                            clm.create(
                                LineOptions()
                                    .withLatLngs(mapPoints)
                                    .withLineColor("#0F172A")
                                    .withLineWidth(7.0f)
                                    .withLineOpacity(0.85f)
                                    .withLineJoin("round")
                            )

                            // 2. High-contrast emerald navigation road line
                            lm.create(
                                LineOptions()
                                    .withLatLngs(mapPoints)
                                    .withLineColor("#10B981")
                                    .withLineWidth(4.5f)
                                    .withLineOpacity(0.95f)
                                    .withLineJoin("round")
                            )
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
actual fun TravelMap(
    places: List<Place>,
    style: MapStyle,
    modifier: Modifier,
    tilt3d: Boolean,
    showPolylines: Boolean,
    showUserLocation: Boolean,
    autoCenterOnLocation: Boolean,
    onMarkerClick: (Place) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val mapView = remember {
        try {
            MapLibre.getInstance(context)
            MapView(context)
        } catch (_: Throwable) {
            null
        }
    }

    val featureHolder = remember(mapView) {
        mapView?.let { MapFeatureHolder(it) }
    }

    if (mapView == null || featureHolder == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Map view unavailable on this device architecture",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = try { mapView.onStart() } catch (_: Throwable) {}
            override fun onResume(owner: LifecycleOwner) = try { mapView.onResume() } catch (_: Throwable) {}
            override fun onPause(owner: LifecycleOwner) = try { mapView.onPause() } catch (_: Throwable) {}
            override fun onStop(owner: LifecycleOwner) = try { mapView.onStop() } catch (_: Throwable) {}
            override fun onDestroy(owner: LifecycleOwner) = try { mapView.onDestroy() } catch (_: Throwable) {}
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mapView.onDestroy() } catch (_: Throwable) {}
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                try {
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isAttributionEnabled = false

                    map.setStyle(style.styleUrl) { loadedStyle ->
                        featureHolder.resetManagers(map, loadedStyle)

                        val firstPlace = places.firstOrNull()
                        val targetCenter = firstPlace?.let { LatLng(it.lat, it.lng) } ?: VIETNAM_DEFAULT_CENTER
                        val zoomLevel = if (firstPlace != null) 12.0 else 6.0

                        map.cameraPosition = CameraPosition.Builder()
                            .target(targetCenter)
                            .zoom(zoomLevel)
                            .tilt(if (tilt3d) 45.0 else 0.0)
                            .bearing(0.0)
                            .build()

                        featureHolder.updatePlacesAndRoutes(places, showPolylines, scope, onMarkerClick)

                        if (showUserLocation) {
                            try {
                                val locationComponent = map.locationComponent
                                val locationComponentOptions = LocationComponentOptions.builder(context)
                                    .pulseEnabled(true)
                                    .pulseColor(android.graphics.Color.parseColor("#10B981"))
                                    .foregroundTintColor(android.graphics.Color.WHITE)
                                    .backgroundTintColor(android.graphics.Color.parseColor("#10B981"))
                                    .build()
                                val activationOptions = LocationComponentActivationOptions
                                    .builder(context, loadedStyle)
                                    .locationComponentOptions(locationComponentOptions)
                                    .useDefaultLocationEngine(true)
                                    .build()
                                locationComponent.activateLocationComponent(activationOptions)
                                locationComponent.isLocationComponentEnabled = true
                                locationComponent.cameraMode = CameraMode.NONE
                                locationComponent.renderMode = RenderMode.NORMAL
                            } catch (_: Throwable) {}
                        }
                    }
                } catch (_: Throwable) {}
            }
            mapView
        },
        update = { view ->
            view.getMapAsync { map ->
                try {
                    val currentStyle = map.style
                    val targetStyleUrl = style.styleUrl

                    if (currentStyle == null || featureHolder.activeStyleUrl != targetStyleUrl) {
                        map.setStyle(targetStyleUrl) { loadedStyle ->
                            featureHolder.resetManagers(map, loadedStyle)
                            featureHolder.updatePlacesAndRoutes(places, showPolylines, scope, onMarkerClick)
                        }
                    } else {
                        featureHolder.updatePlacesAndRoutes(places, showPolylines, scope, onMarkerClick)
                    }

                    // Camera position updates
                    if (places.isNotEmpty()) {
                        if (places.size == 1) {
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(places.first().lat, places.first().lng),
                                    13.5
                                )
                            )
                        } else if (autoCenterOnLocation) {
                            val boundsBuilder = LatLngBounds.Builder()
                            places.forEach { boundsBuilder.include(LatLng(it.lat, it.lng)) }
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                        }
                    }
                } catch (_: Throwable) {}
            }
        },
    )
}
