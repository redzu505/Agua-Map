package com.aguamap.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView

@Composable
fun MapLibreView(modifier: Modifier = Modifier) {
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
                        
                        // Una vez cargado el estilo, añadimos un marcador en SJL
                        val sjl = LatLng(-11.9763, -77.0002)
                        
                        map.addMarker(MarkerOptions()
                            .position(sjl)
                            .title("AguaMap - SJL")
                            .snippet("Punto de hidratación disponible"))
                        
                        // Zoom inicial centrado
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sjl, 14.0))
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
