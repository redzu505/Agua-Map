package com.aguamap.app.domain

/**
 * CAPA DE DOMINIO - MODELOS
 * Representan las entidades de negocio puras.
 */
data class WaterPoint(
    val id: String,
    val name: String,
    val address: String,
    val rating: Double,
    val distance: String,
    val hours: String,
    val status: WaterPointStatus,
    val type: WaterPointType,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class WaterPointStatus {
    OPERATIVO, MANTENIMIENTO
}

enum class WaterPointType {
    FUENTE, POZO, FILTRADA, GRIFO
}
