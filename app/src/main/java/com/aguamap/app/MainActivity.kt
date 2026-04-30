package com.aguamap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aguamap.app.data.local.UserPreferencesRepository
import com.aguamap.app.domain.UserPreferences
import com.aguamap.app.navigation.AppNavigation
import com.aguamap.app.ui.theme.AguaMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { UserPreferencesRepository(applicationContext) }
            val prefs by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())
            
            AguaMapTheme(highContrast = prefs.isHighContrast) {
                AppNavigation()
            }
        }
    }
}
