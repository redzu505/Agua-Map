package com.aguamap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aguamap.app.data.local.DatabaseHelper
import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.data.local.UserPreferencesRepository
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.domain.UserPreferences
import com.aguamap.app.navigation.AppNavigation
import com.aguamap.app.ui.theme.AguaMapTheme
import com.aguamap.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicialización de Dependencias (Manual DI para la Fase 1)
        val dbHelper = DatabaseHelper(this)
        val localDataSource = LocalDataSource(dbHelper)
        val appRepository = AppRepository(localDataSource)
        val homeViewModel = HomeViewModel(appRepository)

        setContent {
            val userPrefsRepo = remember { UserPreferencesRepository(applicationContext) }
            val prefs by userPrefsRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())
            
            AguaMapTheme(highContrast = prefs.isHighContrast) {
                AppNavigation(homeViewModel)
            }
        }
    }
}
