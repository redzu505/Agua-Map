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
    val longitude: Double = 0.0,
    val imageUrl: String? = null
)

enum class WaterPointStatus(val displayName: String) {
    OPERATIVO("Operativo"), 
    MANTENIMIENTO("En Mantenimiento")
}

enum class WaterPointType(val displayName: String) {
    FUENTE("Fuente"), 
    POZO("Pozo"), 
    FILTRADA("Agua Filtrada"), 
    GRIFO("Grifo Público")
}

data class Comment(
    val id: String,
    val author: String,
    val content: String,
    val rating: Int,
    val date: String
)

data class WaterPointReport(
    val id: String,
    val pointId: String,
    val type: ReportType,
    val description: String,
    val date: String
)

enum class ReportType(val displayName: String) {
    AVERIA("No funciona / Averiado"),
    SUCIO("Sucio o en mal estado"),
    CERRADO("Cerrado temporalmente"),
    OTRO("Otro problema")
}

data class CommunityNews(
    val id: Int = 0,
    val title: String,
    val content: String,
    val date: String
)
