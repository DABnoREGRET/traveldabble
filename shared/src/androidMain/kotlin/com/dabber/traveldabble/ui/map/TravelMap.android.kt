package com.dabber.traveldabble.ui.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dabber.traveldabble.model.Place
import com.dabber.traveldabble.model.PlaceCategory
import com.google.gson.JsonPrimitive
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
 * Anchor is at the bottom tip (24, 60).
 */
private fun createGoogleMapsPinBitmap(pinColorHex: String, innerDotColorHex: String? = null): Bitmap {
    val width = 48
    val height = 64
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val pinColor = android.graphics.Color.parseColor(pinColorHex)
    val strokeColor = android.graphics.Color.parseColor("#1E293B")

    // Shadow at bottom tip
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(RectF(12f, 56f, 36f, 63f), shadowPaint)

    // Pin Body Path (Teardrop)
    val pinPath = Path().apply {
        // Circle center at (24, 22), radius 18
        arcTo(RectF(6f, 4f, 42f, 40f), 140f, 260f, false)
        // Line down to bottom point (24, 58)
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
        strokeWidth = 2f
    }
    canvas.drawPath(pinPath, borderPaint)

    // Inner White Disc
    val innerWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(24f, 22f, 9f, innerWhitePaint)

    // Inner Accent Dot
    val dotColor = innerDotColorHex?.let { android.graphics.Color.parseColor(it) } ?: pinColor
    val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(24f, 22f, 5.5f, innerDotPaint)

    return bmp
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

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    fun setupMapFeatures(map: MapLibreMap, loadedStyle: Style) {
        // Register Google Maps style category pins
        val categoryPinConfigs = mapOf(
            "pin_stay" to "#2563EB",       // Blue for Stay / Hotels
            "pin_sight" to "#059669",      // Emerald for Sights / Attractions
            "pin_food" to "#EA580C",       // Orange for Food / Restaurants
            "pin_activity" to "#7C3AED",   // Violet for Activities / Adventure
            "pin_transit" to "#0D9488",    // Teal for Transit / Transport
            "pin_default" to "#DC2626",    // Red Google Maps pin default
        )

        for ((key, colorHex) in categoryPinConfigs) {
            if (loadedStyle.getImage(key) == null) {
                loadedStyle.addImage(key, createGoogleMapsPinBitmap(colorHex), false)
            }
        }

        // Draw Pin Markers with bottom tip anchor
        val symbolManager = SymbolManager(mapView, map, loadedStyle)
        symbolManager.iconAllowOverlap = true
        symbolManager.iconIgnorePlacement = true

        places.forEach { place ->
            val iconKey = when (place.category) {
                PlaceCategory.STAY -> "pin_stay"
                PlaceCategory.SIGHT -> "pin_sight"
                PlaceCategory.FOOD -> "pin_food"
                PlaceCategory.ACTIVITY -> "pin_activity"
                PlaceCategory.TRANSIT -> "pin_transit"
                else -> "pin_default"
            }

            symbolManager.create(
                SymbolOptions()
                    .withLatLng(LatLng(place.lat, place.lng))
                    .withIconImage(iconKey)
                    .withIconAnchor("bottom") // Pin tip points directly at coordinate
                    .withIconSize(1.0f)
            ).also { it.data = JsonPrimitive(place.id) }
        }

        symbolManager.addClickListener { symbol ->
            val placeId = symbol.data?.asString
            places.firstOrNull { it.id == placeId }?.let(onMarkerClick)
            true
        }

        // Polyline connecting places in order (Google Maps style route)
        if (showPolylines && places.size >= 2) {
            val lineManager = LineManager(mapView, map, loadedStyle)
            val points = places.map { LatLng(it.lat, it.lng) }
            lineManager.create(
                LineOptions()
                    .withLatLngs(points)
                    .withLineColor("#059669")
                    .withLineWidth(4.5f)
                    .withLineOpacity(0.9f)
                    .withLineJoin("round")
            )
        }

        // User Location Tracking
        if (showUserLocation) {
            try {
                val locationComponent = map.locationComponent
                val locationComponentOptions = LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .pulseColor(android.graphics.Color.parseColor("#059669"))
                    .foregroundTintColor(android.graphics.Color.WHITE)
                    .backgroundTintColor(android.graphics.Color.parseColor("#059669"))
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
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false

                map.setStyle(style.styleUrl) { loadedStyle ->
                    val firstPlace = places.firstOrNull()
                    val targetCenter = firstPlace?.let { LatLng(it.lat, it.lng) } ?: VIETNAM_DEFAULT_CENTER
                    val zoomLevel = if (firstPlace != null) 12.0 else 6.0

                    map.cameraPosition = CameraPosition.Builder()
                        .target(targetCenter)
                        .zoom(zoomLevel)
                        .tilt(if (tilt3d) 45.0 else 0.0)
                        .bearing(0.0)
                        .build()

                    setupMapFeatures(map, loadedStyle)
                }
            }
            mapView
        },
        update = { view ->
            view.getMapAsync { map ->
                val currentStyle = map.style
                if (currentStyle == null || currentStyle.uri != style.styleUrl) {
                    map.setStyle(style.styleUrl) { loadedStyle ->
                        setupMapFeatures(map, loadedStyle)
                    }
                } else {
                    setupMapFeatures(map, currentStyle)
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
            }
        },
    )
}
