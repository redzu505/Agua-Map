package com.aguamap.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades de fecha/hora para sellar comentarios y reportes con la fecha REAL
 * del momento en que se crean (en vez del texto fijo "Hoy").
 */
object DateUtils {

    /**
     * Devuelve la fecha y hora actual del dispositivo, ej: "16/06/2026 14:30".
     */
    fun fechaHoraActual(): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
        return formato.format(Date())
    }
}
