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
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    waterPoints: List<WaterPoint> = emptyList(),
    onMarkerClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Referencia segura para actualizar los marcadores de forma reactiva
    var mapLibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // 1. Inicialización síncrona del motor.
    // Importante: MapLibre.getInstance DEBE llamarse antes de instanciar cualquier MapView.
    remember(context) {
        MapLibre.getInstance(context)
    }

    // 2. Crear el MapView y gestionar su ciclo de vida manualmente
    val mapView = remember {
        MapView(context)
    }

    // 3. Vincular el ciclo de vida de Compose con el de MapView
    DisposableEffect(lifecycleOwner, mapView) {
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
            // No destruimos el MapView aquí si se va a reutilizar, 
            // pero en este caso al ser un remember simple sin llaves externas, 
            // se destruirá cuando el Composable salga de la composición definitivamente.
        }
    }

    // 4. Actualización reactiva de marcadores
    LaunchedEffect(waterPoints, mapLibreMapInstance) {
        val map = mapLibreMapInstance ?: return@LaunchedEffect
        
        // Esperamos a que el estilo esté cargado antes de manipular marcadores
        if (map.style?.isFullyLoaded == true) {
            updateMarkers(map, waterPoints)
        }
    }

    // 5. Renderizar la vista
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapLibreMapInstance = map
                    
                    // Configuración del estilo
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                        
                        // Añadir marcadores iniciales
                        updateMarkers(map, waterPoints)

                        // Configurar listener de clicks
                        map.setOnMarkerClickListener { marker ->
                            marker.snippet?.let { onMarkerClick(it) }
                            true
                        }

                        // Capa de ubicación
                        if (com.aguamap.app.util.LocationUtils.hasLocationPermissions(context)) {
                            try {
                                val locationComponent = map.locationComponent
                                locationComponent.activateLocationComponent(
                                    LocationComponentActivationOptions.builder(context, style).build()
                                )
                                locationComponent.isLocationComponentEnabled = true
                                locationComponent.cameraMode = CameraMode.TRACKING
                                locationComponent.renderMode = RenderMode.COMPASS
                            } catch (e: Exception) {
                                // Evitar crashes si falla la activación
                            }
                        }

                        // Centrado inicial
                        val center = if (waterPoints.isNotEmpty()) {
                            LatLng(waterPoints.first().latitude, waterPoints.first().longitude)
                        } else {
                            LatLng(-11.9763, -77.0002)
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13.0))
                    }

                    // Habilitar gestos
                    map.uiSettings.isZoomGesturesEnabled = true
                    map.uiSettings.isScrollGesturesEnabled = true
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = {
            // El control se delega a los LaunchedEffect y al factory inicial
        }
    )
}

/**
 * Función auxiliar para limpiar y añadir marcadores de forma segura.
 */
private fun updateMarkers(map: MapLibreMap, waterPoints: List<WaterPoint>) {
    map.clear()
    waterPoints.forEach { point ->
        map.addMarker(
            MarkerOptions()
                .position(LatLng(point.latitude, point.longitude))
                .title(point.name)
                .snippet(point.id)
        )
    }
}
