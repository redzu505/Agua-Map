package com.aguamap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aguamap.app.data.local.DatabaseHelper
import com.aguamap.app.data.local.FavoritosManager
import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.data.local.SessionManager
import com.aguamap.app.data.local.UserPreferencesRepository
import com.aguamap.app.data.remote.RemoteDataSource // para autenctiación
import com.aguamap.app.data.remote.RetrofitClient   // para autenctiación
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.domain.UserPreferences
import com.aguamap.app.navigation.AppNavigation
import com.aguamap.app.navigation.Screen
import com.aguamap.app.ui.theme.AguaMapTheme
import com.aguamap.app.viewmodel.AuthCheckState
import com.aguamap.app.viewmodel.AuthViewModel   // para autenctiación
import com.aguamap.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicialización de Dependencias (Manual DI para la Fase 1)
        val dbHelper = DatabaseHelper(this)
        val localDataSource = LocalDataSource(dbHelper)
        // 1. Inicializamos la parte remota usando el motor de Retrofit
        val remoteDataSource = RemoteDataSource(RetrofitClient.supabaseApi)

        // 2. Gestor de sesión (persiste token + usuario para el auto-login)
        val sessionManager = SessionManager(applicationContext)

        // 2.1 Gestor de puntos guardados (favoritos)
        val favoritosManager = FavoritosManager(applicationContext)

        // 3. Pasamos los DataSources y los gestores al repositorio unificado
        val appRepository = AppRepository(localDataSource, remoteDataSource, sessionManager, favoritosManager)

        // 4. Creamos las instancias de los ViewModels compartiendo el mismo repositorio
        val userPrefsRepo = UserPreferencesRepository(applicationContext)
        val homeViewModel = HomeViewModel(appRepository)
        val authViewModel = AuthViewModel(appRepository, userPrefsRepo, sessionManager) // para lo de autenticación

        setContent {
            val prefs by userPrefsRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
            val authCheck by authViewModel.authCheckState.collectAsState()

            AguaMapTheme(darkTheme = prefs.isDarkMode, highContrast = prefs.isHighContrast) {
                when (authCheck) {
                    // Mientras revisamos si hay sesión guardada, mostramos un splash de carga
                    AuthCheckState.CHECKING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Si hay sesión, arrancamos directo en Home; si no, en Login
                    else -> {
                        val startDestination = if (authCheck == AuthCheckState.AUTHENTICATED) {
                            Screen.Home.route
                        } else {
                            Screen.Login.route
                        }
                        AppNavigation(homeViewModel, authViewModel, startDestination)
                    }
                }
            }
        }
    }
}
