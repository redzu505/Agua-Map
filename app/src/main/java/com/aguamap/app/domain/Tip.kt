package com.aguamap.app.domain

/**
 * CAPA DE DOMINIO - TIP DEL DÍA
 * Cada tip tiene un título y una descripción. La lista vive en assets/tips.json.
 */
data class Tip(
    val titulo: String = "",
    val descripcion: String = ""
)
