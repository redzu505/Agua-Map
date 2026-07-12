package com.aguamap.app.util

import android.content.Context
import com.aguamap.app.domain.Tip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * TIP DEL DÍA
 *
 * Lee la lista de tips desde assets/tips.json (una sola vez, queda en caché) y
 * elige el tip de HOY según el día del año (1..366). Si la lista tiene menos de
 * 365 tips, simplemente se repite en ciclo. Mismo tip para todos ese día.
 *
 * Si el archivo falla por cualquier motivo, devuelve un tip por defecto para que
 * la pantalla NUNCA quede vacía ni se rompa.
 */
object TipsProvider {

    private var cache: List<Tip>? = null

    private val tipPorDefecto = Tip(
        titulo = "Duchas de 5 minutos",
        descripcion = "Ahorra hasta 40 litros de agua por sesión. ¡Cuidemos SJL!"
    )

    fun tipDelDia(context: Context): Tip {
        val lista = obtenerTips(context)
        if (lista.isEmpty()) return tipPorDefecto

        val diaDelAnio = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) // 1..366
        val indice = (diaDelAnio - 1).mod(lista.size) // mod evita índices negativos
        return lista[indice]
    }

    private fun obtenerTips(context: Context): List<Tip> {
        cache?.let { return it }
        return try {
            val json = context.assets.open("tips.json").bufferedReader().use { it.readText() }
            val tipo = object : TypeToken<List<Tip>>() {}.type
            val lista: List<Tip> = Gson().fromJson(json, tipo) ?: emptyList()
            cache = lista
            lista
        } catch (e: Exception) {
            // Si no se puede leer/parsear el archivo, se usará el tip por defecto
            emptyList()
        }
    }
}
