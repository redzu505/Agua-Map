package com.aguamap.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aguamap.app.domain.WaterPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    waterPoints: List<WaterPoint> = emptyList(),
    userLocation: LatLng? = null,
    routeDestination: WaterPoint? = null,
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

    // 4. Actualización reactiva de marcadores y ruta
    LaunchedEffect(waterPoints, userLocation, routeDestination, mapLibreMapInstance) {
        val map = mapLibreMapInstance ?: return@LaunchedEffect
        
        // Esperamos a que el estilo esté cargado antes de manipular el mapa
        if (map.style?.isFullyLoaded == true) {
            updateMarkers(map, waterPoints)
            
            // Si hay ubicación de usuario y destino, dibujamos la ruta
            if (userLocation != null && routeDestination != null) {
                drawRoute(map, userLocation, routeDestination)
            }
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
                                @SuppressLint("MissingPermission")
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

/**
 * Petición asíncrona a OSRM para dibujar la ruta por calles.
 */
private suspend fun drawRoute(map: MapLibreMap, userLoc: LatLng, dest: WaterPoint) {
    val url = "https://router.project-osrm.org/route/v1/foot/${userLoc.longitude},${userLoc.latitude};${dest.longitude},${dest.latitude}?overview=full&geometries=geojson"
    
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string() ?: return@withContext
            val jsonObject = JSONObject(jsonData)
            val routes = jsonObject.optJSONArray("routes")
            
            if (routes != null && routes.length() > 0) {
                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val points = mutableListOf<LatLng>()
                
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
                }

                withContext(Dispatchers.Main) {
                    // Dibujar la polilínea
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(android.graphics.Color.parseColor("#FF5722")) // Naranja llamativo
                            .width(6f)
                    )

                    // Ajustar cámara para encuadrar ambos puntos
                    val bounds = LatLngBounds.Builder()
                        .include(userLoc)
                        .include(LatLng(dest.latitude, dest.longitude))
                        .build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
