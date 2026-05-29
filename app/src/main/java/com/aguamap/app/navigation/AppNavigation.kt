package com.aguamap.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aguamap.app.ui.AddWaterPointScreen
import com.aguamap.app.ui.CommunityScreen
import com.aguamap.app.ui.HomeScreen
import com.aguamap.app.ui.ProfileScreen
import com.aguamap.app.ui.WaterPointDetailScreen
import com.aguamap.app.viewmodel.AuthViewModel
import com.aguamap.app.viewmodel.HomeViewModel

@Composable
fun AppNavigation(homeViewModel: HomeViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            // Nota: Si tu LoginScreen tiene un botón de "Entrar como Invitado",
            // debería navegar al Home pasando algún indicador, o el Home heredar ese estado.
            com.aguamap.app.ui.LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                homeViewModel = homeViewModel,
                // Supongamos que por defecto no es invitado tras loguearse con éxito.
                // Si estás probando el modo invitado, puedes cambiar el false por true temporalmente aquí:
                onNavigateToProfile = { navController.navigate("profile/false") },
                onNavigateToCommunity = { navController.navigate("community") },
                onNavigateToDetail = { pointId ->
                    navController.navigate(Screen.WaterPointDetail.createRoute(pointId))
                },
                onNavigateToAddPoint = {
                    navController.navigate(Screen.AddWaterPoint.route)
                }
            )
        }

        // MODIFICADO: Ahora la ruta acepta saber si es invitado o no ("profile/{isGuest}")
        composable(
            route = "profile/{isGuest}",
            arguments = listOf(navArgument("isGuest") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isGuest = backStackEntry.arguments?.getBoolean("isGuest") ?: false
            ProfileScreen(
                isGuest = isGuest,
                onBack = { navController.popBackStack() },
                onLoginClick = {
                    // Si es invitado y presiona iniciar sesión, lo regresamos a la pantalla de Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable("community") {
            CommunityScreen(homeViewModel = homeViewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.WaterPointDetail.route,
            arguments = listOf(navArgument("pointId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pointId = backStackEntry.arguments?.getString("pointId") ?: ""
            WaterPointDetailScreen(
                pointId = pointId,
                homeViewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddWaterPoint.route) {
            AddWaterPointScreen(onBack = { navController.popBackStack() })
        }
    }
}