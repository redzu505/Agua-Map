package com.aguamap.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * CAPA DE DATOS - DTOs REMOTOS (PostgREST de Supabase)
 *
 * Estos objetos representan EXACTAMENTE las filas tal como viajan en el JSON
 * de la API REST de Supabase. Los nombres de campo coinciden con las columnas
 * de las tablas (ver SUPABASE_TABLAS.md). Todos los campos son nullable por
 * seguridad para que Gson no falle si alguna columna viene vacía.
 */

// ---------- PUNTOS DE AGUA ----------
data class PuntoDto(
    val id: String? = null,
    val nombre: String? = null,
    val direccion: String? = null,
    val calificacion: Double? = null,
    val horario: String? = null,
    val estado: String? = null,   // guarda el name() del enum: OPERATIVO / MANTENIMIENTO
    val tipo: String? = null,     // guarda el name() del enum: FUENTE / POZO / FILTRADA / GRIFO
    val latitud: Double? = null,
    val longitud: Double? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)

// ---------- COMENTARIOS ----------
data class ComentarioDto(
    val id: String? = null,
    @SerializedName("punto_id") val puntoId: String? = null,
    val autor: String? = null,
    val contenido: String? = null,
    val calificacion: Int? = null,
    val fecha: String? = null
)

// ---------- REPORTES ----------
data class ReporteDto(
    val id: String? = null,
    @SerializedName("punto_id") val puntoId: String? = null,
    val tipo: String? = null,     // guarda el name() del enum ReportType
    val descripcion: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    val fecha: String? = null
)

// ---------- NOTICIAS DE LA COMUNIDAD ----------
data class NoticiaDto(
    val id: Int? = null,
    val titulo: String? = null,
    val contenido: String? = null,
    val fecha: String? = null
)
