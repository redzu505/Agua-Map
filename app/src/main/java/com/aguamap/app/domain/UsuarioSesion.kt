package com.aguamap.app.domain

data class UsuarioSesion(
    val nombre: String = "",
    val usuario: String = "",
    val email: String = "",
    val telefono: String = "",
    val dni: String = "",
    val rol: String = "usuario"   // 'usuario' | 'admin' (se lee de la tabla perfiles)
)