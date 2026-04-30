package com.aguamap.app.navigation

/**
 * CAPA DE NAVEGACIÓN - RUTAS
 * Define los destinos de la aplicación.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Map : Screen("map")
    object WaterPointDetail : Screen("water_point_detail/{pointId}") {
        fun createRoute(pointId: String) = "water_point_detail/$pointId"
    }
    object AddWaterPoint : Screen("add_water_point")
}
