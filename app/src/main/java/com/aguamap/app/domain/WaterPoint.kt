package com.aguamap.app.domain

/**
 * CAPA DE DOMINIO - MODELOS
 * Representan las entidades de negocio puras, independientes de la base de datos o API.
 */
data class WaterPoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val isPotable: Boolean
)
