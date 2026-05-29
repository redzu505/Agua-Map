package com.aguamap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aguamap.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Representa los diferentes estados en los que puede estar la pantalla de registro
 */
sealed interface RegisterState {
    object Idle : RegisterState                               // Estado inicial, esperando que el usuario pulse el botón
    object Loading : RegisterState                            // Mostrando un spinner/barra de carga mientras viaja el internet
    data class Success(val message: String) : RegisterState   // Registro exitoso en Supabase
    data class Error(val error: String) : RegisterState       // Algo falló (ej. Correo ya registrado, sin internet)
}

data class UsuarioSesion(
    val nombre: String = "",
    val usuario: String = "",
    val email: String = "",
    val telefono: String = "",
    val dni: String = ""
)

/**
 * CAPA DE VIEWMODEL - AUTENTICACIÓN
 * Gestiona el estado del login y registro de usuarios interactuando con el repositorio.
 */
class AuthViewModel(private val repository: AppRepository) : ViewModel() {

    // Flujo interno mutable y flujo externo de solo lectura para la UI
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    // Controlar si el usuario actual entró como invitado o no
    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    //El estado que guardará al usuario logueado en la RAM
    private val _usuarioLogueado = MutableStateFlow<UsuarioSesion?>(null)
    val usuarioLogueado: StateFlow<UsuarioSesion?> = _usuarioLogueado.asStateFlow()

    /**
     * FUNCIÓN: Cambia el estado a 'true' si el usuario decide omitir el login
     */
    fun entrarComoInvitado() {
        _isGuest.value = true
        _usuarioLogueado.value = null // No hay usuario
    }

    /**
     * Envía los datos de registro recolectados de la UI hacia el repositorio en segundo plano
     */
    fun registrarUsuario(
        email: String,
        contrasenia: String,
        nombre: String,
        dni: String,
        telefono: String,
        usuario: String
    ) {
        // Ejecutamos la petición dentro de una corrutina en segundo plano para no congelar la app
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            val resultado = repository.registrarUsuario(
                email = email,
                contrasenia = contrasenia,
                nombre = nombre,
                dni = dni,
                telefono = telefono,
                usuario = usuario
            )

            // Evaluamos la respuesta que nos devolvió el repositorio
            resultado.onSuccess { authResponse ->
                //Al registrarse con éxito, guardamos los datos en nuestro estado local
                _usuarioLogueado.value = UsuarioSesion(
                    nombre = nombre,
                    usuario = usuario,
                    email = email,
                    telefono = telefono,
                    dni = dni
                )
                _isGuest.value = false //si se registra ya no es invitado
                _registerState.value = RegisterState.Success("¡Usuario creado con éxito!")
            }.onFailure { excepcion ->
                _registerState.value = RegisterState.Error(excepcion.message ?: "Ocurrió un error inesperado")
            }
        }
    }

    /**
     * Función de Ajustes
     * para actualizar los datos tanto en Supabase como en la pantalla de la app.
     * EN PRUEBA AÚN...
     */
    fun actualizarDatosUsuario(nuevoNombre: String, nuevoTelefono: String) {
        viewModelScope.launch {
            // 1. Aquí llamarías a tu repositorio para actualizar Supabase:
            // repository.actualizarPerfil(...)

            // 2. Si el backend responde OK, actualizamos nuestro StateFlow local:
            _usuarioLogueado.value = _usuarioLogueado.value?.copy(
                nombre = nuevoNombre,
                telefono = nuevoTelefono
            )
            // Al hacer este .copy(), cualquier pantalla que mire "usuarioLogueado" (como el Perfil)
            // se actualizará sola en tiempo real.
        }
    }

    /**
     * Limpia el estado cuando el usuario sale de la pantalla o cierra un mensaje de error
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}