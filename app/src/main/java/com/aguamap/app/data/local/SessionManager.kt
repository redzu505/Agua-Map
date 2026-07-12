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
        val USER_ID = stringPreferencesKey("user_id")
        val NOMBRE = stringPreferencesKey("nombre")
        val USUARIO = stringPreferencesKey("usuario")
        val EMAIL = stringPreferencesKey("email")
        val TELEFONO = stringPreferencesKey("telefono")
        val DNI = stringPreferencesKey("dni")
        val ROL = stringPreferencesKey("rol")
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
                        id = prefs[Keys.USER_ID] ?: "",
                        nombre = prefs[Keys.NOMBRE] ?: "",
                        usuario = prefs[Keys.USUARIO] ?: "",
                        email = prefs[Keys.EMAIL] ?: "",
                        telefono = prefs[Keys.TELEFONO] ?: "",
                        dni = prefs[Keys.DNI] ?: "",
                        rol = prefs[Keys.ROL] ?: "usuario"
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
            prefs[Keys.USER_ID] = usuario.id
            prefs[Keys.NOMBRE] = usuario.nombre
            prefs[Keys.USUARIO] = usuario.usuario
            prefs[Keys.EMAIL] = usuario.email
            prefs[Keys.TELEFONO] = usuario.telefono
            prefs[Keys.DNI] = usuario.dni
            prefs[Keys.ROL] = usuario.rol
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
     * Devuelve el UUID del usuario logueado (o null). Necesario para consultar
     * datos propios como "mi valoración" de un punto.
     */
    suspend fun obtenerUserId(): String? {
        return context.sessionDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[Keys.USER_ID]
    }

    /** Devuelve el refresh_token guardado (o null). Sirve para renovar el access_token vencido. */
    suspend fun obtenerRefreshToken(): String? {
        return context.sessionDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[Keys.REFRESH_TOKEN]
    }

    /** Actualiza solo los tokens (tras renovar), sin tocar los datos del usuario. */
    suspend fun actualizarTokens(accessToken: String, refreshToken: String?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            if (refreshToken != null) prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
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
