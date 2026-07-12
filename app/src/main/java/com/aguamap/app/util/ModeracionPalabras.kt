package com.aguamap.app.util

/**
 * MODERACIÓN DE PALABRAS (filtro local)
 *
 * Reemplaza las palabras no permitidas por asteriscos antes de guardar
 * comentarios y reportes. Es offline (no necesita internet) y centralizado:
 * para agregar/quitar palabras, edita la lista `palabrasProhibidas`.
 *
 * Nota: compara por palabra completa, sin distinguir mayúsculas ni tildes
 * (ej. "MIÉRDA" también se detecta).
 */
object ModeracionPalabras {

    // 👇 El equipo puede agregar o quitar palabras aquí (en minúscula y sin tildes).
    private val palabrasProhibidas = setOf(
        "mierda", "puta", "puto", "cabron", "concha", "conchatumadre", "conchadetumadre",
        "carajo", "pendejo", "pendeja", "huevon", "huevona", "imbecil", "idiota",
        "estupido", "estupida", "maricon", "verga", "pija", "culiao", "culiado",
        "ctm", "csm", "ctmr", "mrd", "joder", "coño", "gilipollas", "zorra", "perra",
        "malparido", "hijueputa", "hdp", "cojudo", "cojuda", "tarado", "tarada",
        "baboso", "cornudo", "chucha", "mocoso", "pelotudo", "boludo", "forro", "tonto"
    )

    private val regexPalabras = Regex("[\\p{L}]+")

    /**
     * Devuelve el texto con las palabras prohibidas reemplazadas por asteriscos
     * del mismo largo. Ej: "es una mierda" -> "es una ******".
     */
    fun censurar(texto: String): String {
        if (texto.isBlank()) return texto
        return regexPalabras.replace(texto) { match ->
            val palabra = match.value
            if (normalizar(palabra) in palabrasProhibidas) {
                "*".repeat(palabra.length)
            } else {
                palabra
            }
        }
    }

    /**
     * Indica si el texto contiene alguna palabra prohibida (por si quieres
     * mostrar un aviso o bloquear en lugar de censurar).
     */
    fun contienePalabrasProhibidas(texto: String): Boolean {
        return regexPalabras.findAll(texto).any { normalizar(it.value) in palabrasProhibidas }
    }

    // Pasa a minúsculas y quita tildes para comparar de forma flexible.
    private fun normalizar(palabra: String): String {
        return palabra.lowercase()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i')
            .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
    }
}
