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
import com.aguamap.app.ui.LoginScreen
import com.aguamap.app.ui.ProfileScreen
import com.aguamap.app.ui.WaterPointDetailScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToCommunity = { navController.navigate("community") },
                onNavigateToDetail = { pointId ->
                    navController.navigate(Screen.WaterPointDetail.createRoute(pointId))
                },
                onNavigateToAddPoint = {
                    navController.navigate(Screen.AddWaterPoint.route)
                }
            )
        }

        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        composable("community") {
            CommunityScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.WaterPointDetail.route,
            arguments = listOf(navArgument("pointId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pointId = backStackEntry.arguments?.getString("pointId") ?: ""
            WaterPointDetailScreen(pointId = pointId, onBack = { navController.popBackStack() })
        }

        composable(Screen.AddWaterPoint.route) {
            AddWaterPointScreen(onBack = { navController.popBackStack() })
        }
    }
}
