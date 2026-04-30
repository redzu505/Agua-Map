package com.aguamap.app.domain

data class UserPreferences(
    val selectedSector: String = "Todos",
    val isHighContrast: Boolean = false,
    val searchRadius: Float = 1.0f,
    val isAnonymous: Boolean = false
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
