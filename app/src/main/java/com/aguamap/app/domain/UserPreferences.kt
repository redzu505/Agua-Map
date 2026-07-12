package com.aguamap.app.domain

data class UserPreferences(
    val selectedSector: String = "Todos",
    val isHighContrast: Boolean = false,
    val searchRadius: Float = 1.0f,
    val isAnonymous: Boolean = false,
    val isDarkMode: Boolean = false   // Modo oscuro (con nuestra paleta Ocean & Cloud)
)

val SJL_SECTORS = listOf(
    "Todos",
    "Zárate",
    "Las Flores",
    "Canto Grande",
    "San Carlos",
    "Huáscar",
    "Campoy",
    "Caja de Agua",
    "Mangomarca"
)
