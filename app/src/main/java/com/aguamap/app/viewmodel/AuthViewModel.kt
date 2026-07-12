package com.aguamap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aguamap.app.domain.UsuarioSesion
import com.aguamap.app.data.repository.AppRepository
import com.aguamap.app.data.local.SessionManager
import com.aguamap.app.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 * Estado de la verificación de sesión al abrir la app (auto-login).
 * Mientras es CHECKING mostramos un splash; luego decidimos a qué pantalla ir.
 */
enum class AuthCheckState {
    CHECKING,        // Revisando si hay una sesión guardada
    AUTHENTICATED,   // Hay sesión válida -> ir directo al Home
    UNAUTHENTICATED  // No hay sesión -> ir al Login
}


/**
 * CAPA DE VIEWMODEL - AUTENTICACIÓN
 * Gestiona el estado del login y registro de usuarios interactuando con el repositorio.
 */
class AuthViewModel(
    private val repository: AppRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionManager: SessionManager
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

    // Estado de la verificación inicial de sesión (auto-login)
    private val _authCheckState = MutableStateFlow(AuthCheckState.CHECKING)
    val authCheckState: StateFlow<AuthCheckState> = _authCheckState.asStateFlow()

    init {
        // AUTO-LOGIN: al crear el ViewModel revisamos si hay una sesión guardada
        viewModelScope.launch {
            val sesionGuardada = sessionManager.sessionFlow.first()
            if (sesionGuardada != null) {
                _usuarioLogueado.value = sesionGuardada.usuario
                _isGuest.value = false
                _authCheckState.value = AuthCheckState.AUTHENTICATED
            } else {
                _authCheckState.value = AuthCheckState.UNAUTHENTICATED
            }
        }
    }

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
                val nuevoUsuario = UsuarioSesion(
                    id = authResponse.user?.id ?: "",
                    nombre = nombre,
                    usuario = usuario,
                    email = email,
                    telefono = telefono,
                    dni = dni
                )
                _usuarioLogueado.value = nuevoUsuario
                _isGuest.value = false //si se registra ya no es invitado

                // Persistimos la sesión (el token puede ser null si Supabase exige
                // confirmar el correo; igual guardamos los datos del usuario)
                sessionManager.guardarSesion(
                    usuario = nuevoUsuario,
                    accessToken = authResponse.access_token,
                    refreshToken = authResponse.refresh_token
                )

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

            // El repositorio nos devuelve el usuario junto con los tokens (AuthSession)
            val resultado = repository.iniciarSesion(email, contrasenia)

            resultado.onSuccess { sesion ->
                // Guardamos el usuario en la RAM
                _usuarioLogueado.value = sesion.usuario
                _isGuest.value = false

                // Persistimos la sesión completa para el auto-login
                sessionManager.guardarSesion(
                    usuario = sesion.usuario,
                    accessToken = sesion.accessToken,
                    refreshToken = sesion.refreshToken
                )

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
     * Función de Ajustes: actualiza el nombre y teléfono tanto en Supabase como en la app.
     */
    fun actualizarDatosUsuario(nuevoNombre: String, nuevoTelefono: String) {
        viewModelScope.launch {
            // 1. Intentamos actualizar en Supabase (PUT /auth/v1/user)
            val resultado = repository.actualizarPerfil(nuevoNombre, nuevoTelefono)

            // 2. Pase lo que pase con la red, actualizamos el estado local para que la UI reaccione
            val actualizado = _usuarioLogueado.value?.copy(
                nombre = nuevoNombre,
                telefono = nuevoTelefono
            )
            _usuarioLogueado.value = actualizado

            // 3. Si tenemos usuario, persistimos los nuevos datos localmente
            actualizado?.let { sessionManager.actualizarUsuario(it) }

            // Si falló el backend no rompemos nada: la UI ya muestra el cambio local
            resultado.onFailure { /* opcional: exponer un estado de error de perfil */ }
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
        viewModelScope.launch {
            // 1. Cerramos sesión en el servidor (best-effort) antes de borrar el token
            repository.cerrarSesionRemota()
            // 2. Borramos la sesión local persistida
            sessionManager.limpiarSesion()
            // 3. También limpiamos las preferencias locales
            userPreferencesRepository.clearAll()
            // 4. Limpiamos los puntos guardados (son del usuario que salió)
            repository.limpiarFavoritos()
        }
        // 4. Limpiamos el estado en memoria (inmediato para que la UI reaccione)
        _usuarioLogueado.value = null
        _isGuest.value = false
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }
}
