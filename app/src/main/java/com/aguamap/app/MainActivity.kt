package com.aguamap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aguamap.app.ui.LoginScreen
import com.aguamap.app.ui.theme.AguaMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita el modo de borde a borde para que el gradiente ocupe toda la pantalla
        enableEdgeToEdge()
        setContent {
            AguaMapTheme {
                LoginScreen()
            }
        }
    }
}
