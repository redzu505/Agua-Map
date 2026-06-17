package com.aguamap.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore propio para los puntos guardados (favoritos). No usa SQLite, así
// NO hay que cambiar la versión de la base de datos (no se borra nada existente).
private val Context.favoritosDataStore: DataStore<Preferences> by preferencesDataStore(name = "favoritos_prefs")

/**
 * CAPA DE DATOS - FAVORITOS (caché local)
 * Guarda en el teléfono el conjunto de IDs de puntos de agua que el usuario marcó
 * como guardados. Sirve de caché offline; la fuente "oficial" es la tabla
 * `favoritos` de Supabase (ver AppRepository).
 */
class FavoritosManager(private val context: Context) {

    private val KEY_FAVORITOS = stringSetPreferencesKey("favoritos_ids")

    /** Flujo reactivo con los IDs guardados (para que la UI reaccione). */
    val favoritosFlow: Flow<Set<String>> = context.favoritosDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_FAVORITOS] ?: emptySet() }

    /** Lectura puntual del conjunto actual. */
    suspend fun favoritosActuales(): Set<String> {
        return context.favoritosDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[KEY_FAVORITOS] ?: emptySet()
    }

    /** Reemplaza por completo el conjunto guardado. */
    suspend fun guardar(ids: Set<String>) {
        context.favoritosDataStore.edit { prefs ->
            prefs[KEY_FAVORITOS] = ids
        }
    }

    /** Borra todos los favoritos locales (al cerrar sesión). */
    suspend fun limpiar() {
        context.favoritosDataStore.edit { prefs ->
            prefs.remove(KEY_FAVORITOS)
        }
    }
}
