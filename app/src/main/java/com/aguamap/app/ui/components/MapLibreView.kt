package com.aguamap.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aguamap.app.domain.WaterPoint
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView

@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    waterPoints: List<WaterPoint> = emptyList(),
    onMarkerClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Inicializar el motor
    remember {
        Mapbox.getInstance(context)
    }

    // 2. Crear el MapView
    val mapView = remember { MapView(context) }

    // 3. Gestionar el ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. Mostrar el mapa
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    // Usamos un estilo claro (liberty o bright)
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                        
                        // Añadimos marcadores dinámicos
                        waterPoints.forEach { point ->
                            map.addMarker(MarkerOptions()
                                .position(LatLng(point.latitude, point.longitude))
                                .title(point.name)
                                .snippet(point.id)) // Usamos snippet para guardar el ID
                        }

                        // Listener de clicks en marcadores
                        map.setOnMarkerClickListener { marker ->
                            marker.snippet?.let { onMarkerClick(it) }
                            true
                        }

                        // Activar capa de ubicación si hay permisos
                        if (com.aguamap.app.util.LocationUtils.hasLocationPermissions(context)) {
                            try {
                                val locationComponent = map.locationComponent
                                locationComponent.activateLocationComponent(
                                    LocationComponentActivationOptions.builder(context, style).build()
                                )
                                locationComponent.isLocationComponentEnabled = true
                                locationComponent.cameraMode = CameraMode.TRACKING
                                locationComponent.renderMode = RenderMode.COMPASS
                            } catch (e: SecurityException) {
                                // Ignorar si falla por permisos en runtime no detectados
                            }
                        }
                        
                        // Centrar en SJL (o en el primer punto si existe)
                        val center = if (waterPoints.isNotEmpty()) {
                            LatLng(waterPoints.first().latitude, waterPoints.first().longitude)
                        } else {
                            LatLng(-11.9763, -77.0002)
                        }
                        
                        // Zoom inicial centrado
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13.0))
                    }
                    
                    // Asegurar que los gestos sigan activos
                    map.uiSettings.isZoomGesturesEnabled = true
                    map.uiSettings.isScrollGesturesEnabled = true
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
