package com.aguamap.app.domain

/**
 * CAPA DE DOMINIO - SESIÓN AUTENTICADA
 * Agrupa al usuario con los tokens que devuelve Supabase tras el login/registro.
 * Los tokens son los que permiten hacer peticiones autenticadas (RLS) más adelante.
 */
data class AuthSession(
    val usuario: UsuarioSesion,
    val accessToken: String? = null,
    val refreshToken: String? = null
)
