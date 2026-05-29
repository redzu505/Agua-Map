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
import androidx.compose.runtime.collectAsState
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

        /*composable(Screen.Home.route) {
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
        }*/
        composable(Screen.Home.route) {
            // 1. Obtenemos si el usuario actual es un invitado desde el ViewModel de autenticación
            val isGuestUser = authViewModel.isGuest.collectAsState().value

            // 2.Obtenemos el objeto completo del usuario desde el AuthViewModel
            val usuario = authViewModel.usuarioLogueado.collectAsState().value

            //Línea de control: BORRRAR
            println("AGUAMAP_DEBUG: El valor de isGuestUser en el Home es: $isGuestUser")
            println("AGUAMAP_DEBUG: El usuario en sesión es: ${usuario?.nombre ?: "Ninguno (Invitado)"}")

            HomeScreen(
                homeViewModel = homeViewModel,
                isGuest = isGuestUser, // Le enviamos el estado al HomeScreen
                userName = usuario?.nombre ?: "Usuario",   // ◄ NUEVO: Si es nulo (invitado), usa "Usuario" por defecto
                userEmail = usuario?.email ?: "",           // ◄ NUEVO: Si es nulo, queda vacío
                // 2. Pasamos el valor dinámicamente en la ruta del perfil ◄ AQUÍ SE HACE LA MAGIA
                onNavigateToProfile = { navController.navigate("profile/$isGuestUser") },
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

            //Obtenemos el usuario aquí también para la ruta directa
            val usuario = authViewModel.usuarioLogueado.collectAsState().value

            //pruebas: BORRRAR LINEA LN

            println("AGUAMAP_DEBUG: El perfil recibió el argumento isGuest = $isGuest")

            ProfileScreen(
                isGuest = isGuest,
                userName = usuario?.nombre ?: "Usuario", // Pasamos el nombre real o default
                userEmail = usuario?.email ?: "",       // Pasamos el correo real o vacío
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