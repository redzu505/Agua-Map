package com.aguamap.app.data.remote

// Lo que le enviamos a Supabase para registrarse
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: UserMetadata
)

// Los campos extra que capturas en tu RegisterView
data class UserMetadata(
    val full_name: String,
    val dni: String,
    val phone: String,
    val username: String
)

// Lo que Supabase nos responde cuando el registro es exitoso
data class AuthResponse(
    val id: String,
    val email: String
)