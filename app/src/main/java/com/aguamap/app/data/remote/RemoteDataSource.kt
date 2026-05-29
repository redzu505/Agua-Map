package com.aguamap.app.data.remote

import java.lang.Exception
/**
 * CAPA DE DATOS - REMOTA
 * Aquí se gestiona la comunicación con servidores externos o APIs (ej. Retrofit, Ktor).
 * 
 * TODO: Configurar Retrofit para obtener los puntos de agua desde una API REST.
 */
class RemoteDataSource(private val apiService: SupabaseApiService) {
    private val apiKey = "sb_publishable_45XgBmKYWXiYz_tn2V8uOg_VK6OsUe0"
    private val bearerToken = "Bearer sb_publishable_45XgBmKYWXiYz_tn2V8uOg_VK6OsUe0"

    suspend fun registrarUsuario(
        email: String,
        contrasenia: String,
        nombre: String,
        dni: String,
        telefono: String,
        usuario: String
    ): Result<AuthResponse> {
        return try {
            // 1. Empaquetamos los datos del formulario en el formato que Supabase entiende
            val metadata = UserMetadata(full_name = nombre, dni = dni, phone = telefono, username = usuario)
            val request = SignUpRequest(email = email, password = contrasenia, data = metadata)

            // 2. Hacemos la petición web a través de Retrofit
            val response = apiService.signUp(apiKey, bearerToken, request)

            // 3. Evaluamos si el servidor nos aceptó
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                Result.failure(Exception("Error en Supabase: $errorMsg"))
            }
        } catch (e: Exception) {
            // Captura errores como: "El celular no tiene internet" o "Servidor caído"
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, contrasenia: String): Result<AuthResponse> {
        return try {
            // 1. Armamos la petición de login
            val request = LoginRequest(email = email, password = contrasenia)

            // 2. Ejecutamos el POST contra Supabase
            val response = apiService.signIn(apiKey, bearerToken, request)

            // 3. Evaluamos el resultado del servidor
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                Result.failure(Exception("Error en Supabase: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e) // Captura fallas de red/conectividad
        }
    }
}
