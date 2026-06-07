package com.aguamap.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aguamap.app.domain.AuthSession
import com.aguamap.app.domain.UsuarioSesion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore independiente y exclusivo para la sesión (separado de las preferencias de UI)
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

/**
 * CAPA DE DATOS - GESTOR DE SESIÓN
 * Persiste de forma local (cifrada por DataStore) el token de acceso y los datos
 * del usuario logueado para mantener la sesión iniciada al reabrir la app.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val NOMBRE = stringPreferencesKey("nombre")
        val USUARIO = stringPreferencesKey("usuario")
        val EMAIL = stringPreferencesKey("email")
        val TELEFONO = stringPreferencesKey("telefono")
        val DNI = stringPreferencesKey("dni")
    }

    /**
     * Emite la sesión guardada o `null` si no hay nadie logueado.
     */
    val sessionFlow: Flow<AuthSession?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
            if (!isLoggedIn) {
                null
            } else {
                AuthSession(
                    usuario = UsuarioSesion(
                        nombre = prefs[Keys.NOMBRE] ?: "",
                        usuario = prefs[Keys.USUARIO] ?: "",
                        email = prefs[Keys.EMAIL] ?: "",
                        telefono = prefs[Keys.TELEFONO] ?: "",
                        dni = prefs[Keys.DNI] ?: ""
                    ),
                    accessToken = prefs[Keys.ACCESS_TOKEN],
                    refreshToken = prefs[Keys.REFRESH_TOKEN]
                )
            }
        }

    /**
     * Guarda la sesión completa tras un login o registro exitoso.
     */
    suspend fun guardarSesion(usuario: UsuarioSesion, accessToken: String?, refreshToken: String?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.NOMBRE] = usuario.nombre
            prefs[Keys.USUARIO] = usuario.usuario
            prefs[Keys.EMAIL] = usuario.email
            prefs[Keys.TELEFONO] = usuario.telefono
            prefs[Keys.DNI] = usuario.dni
            if (accessToken != null) prefs[Keys.ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Actualiza solo los datos del perfil (sin tocar los tokens), tras editar el perfil.
     */
    suspend fun actualizarUsuario(usuario: UsuarioSesion) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.NOMBRE] = usuario.nombre
            prefs[Keys.USUARIO] = usuario.usuario
            prefs[Keys.EMAIL] = usuario.email
            prefs[Keys.TELEFONO] = usuario.telefono
            prefs[Keys.DNI] = usuario.dni
        }
    }

    /**
     * Devuelve el token de acceso actual (o null si no hay sesión). Útil para
     * peticiones autenticadas a las tablas de Supabase protegidas con RLS.
     */
    suspend fun obtenerAccessToken(): String? {
        return context.sessionDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[Keys.ACCESS_TOKEN]
    }

    /**
     * Borra la sesión por completo (logout).
     */
    suspend fun limpiarSesion() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
