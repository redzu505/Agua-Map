package com.aguamap.app.data.remote

// Lo que le enviamos a Supabase para registrarse
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: UserMetadata
)

//añadido para login
data class LoginRequest(
    val email: String,
    val password: String
)

// Para renovar el access_token vencido usando el refresh_token
data class RefreshRequest(
    val refresh_token: String
)

// Lo que enviamos para actualizar el perfil del usuario logueado (PUT /auth/v1/user)
data class UpdateUserRequest(
    val data: UserMetadata
)

// Los campos extra que se captura en  RegisterView
// Nota: Les ponemos "? = null" por seguridad para que Gson no rompa si alguno falta
data class UserMetadata(
    val full_name: String? = null,
    val dni: String? = null,
    val phone: String? = null,
    val username: String? = null
)

// Lo que Supabase responde en Login y Registro (Estructura real completa)
data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user: SupabaseUser? = null // ◄ Esto remueve el error en rojo del repositorio
)

// El objeto intermedio que contiene la información del usuario en Supabase
data class SupabaseUser(
    val id: String? = null,
    val email: String? = null,
    val user_metadata: UserMetadata? = null // ◄ Aquí viaja la bolsita con nombre, dni, etc.
)