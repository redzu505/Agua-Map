package com.aguamap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aguamap.app.domain.UsuarioSesion
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.data.local.UserPreferencesRepository
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
/**
 * Representa los diferentes estados en los que puede estar la pantalla de login
 */
sealed interface LoginState {
    object Idle : LoginState                                // Esperando acción del usuario
    object Loading : LoginState                             // Procesando credenciales (Cargando...)
    object Success : LoginState                             // Login correcto
    data class Error(val error: String) : LoginState        // Credenciales inválidas o sin red
}



/**
 * CAPA DE VIEWMODEL - AUTENTICACIÓN
 * Gestiona el estado del login y registro de usuarios interactuando con el repositorio.
 */
class AuthViewModel(
    private val repository: AppRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Flujo interno mutable y flujo externo de solo lectura para la UI
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    // Controlar si el usuario actual entró como invitado o no
    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    //El estado que guardará al usuario logueado en la RAM
    private val _usuarioLogueado = MutableStateFlow<UsuarioSesion?>(null)
    val usuarioLogueado: StateFlow<UsuarioSesion?> = _usuarioLogueado.asStateFlow()

    // Flujo interno mutable y externo de solo lectura para el estado del Login
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

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
     * FUNCIÓN: Valida las credenciales contra el repositorio
     */
    fun iniciarSesion(email: String, contrasenia: String) {
        if (email.isBlank() || contrasenia.isBlank()) {
            _loginState.value = LoginState.Error("Por favor, llena todos los campos.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            // Al llamar al repositorio, 'resultado' ya sabe que es un Result<UsuarioSesion>
            val resultado = repository.iniciarSesion(email, contrasenia)

            resultado.onSuccess { usuarioDeBackend ->
                // Como el repositorio ya nos da el objeto estructurado, lo guardamos directo en la RAM
                _usuarioLogueado.value = usuarioDeBackend
                _isGuest.value = false
                _loginState.value = LoginState.Success
            }.onFailure { excepcion ->
                // Mapeo de errores para mostrar mensajes limpios en español
                val errorMsg = when {
                    // Errores de red
                    excepcion is java.net.UnknownHostException || 
                    excepcion is java.net.ConnectException ||
                    excepcion.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Error de conexión. Inténtalo de nuevo."

                    // Errores de credenciales (Supabase / GoTrue)
                    excepcion.message?.contains("credentials", ignoreCase = true) == true ||
                    excepcion.message?.contains("invalid", ignoreCase = true) == true ||
                    excepcion.message?.contains("auth", ignoreCase = true) == true -> 
                        "Correo o contraseña erróneos."

                    // Fallback para cualquier otro error de autenticación
                    else -> "correo o contraseña erróneos."
                }
                _loginState.value = LoginState.Error(errorMsg)
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
     * Limpia el estado de error del login al escribir o cerrar alertas
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * Limpia el estado cuando el usuario sale de la pantalla o cierra un mensaje de error
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    fun cerrarSesion() {
        _usuarioLogueado.value = null
        _isGuest.value = false
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
        
        // También limpiamos las preferencias locales
        viewModelScope.launch {
            userPreferencesRepository.clearAll()
        }
    }
}