package com.aguamap.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.viewmodel.AuthViewModel   // NUEVO IMPORT
import com.aguamap.app.viewmodel.RegisterState // UEVO IMPORT
import com.aguamap.app.viewmodel.LoginState // NUEVO IMPORT

enum class AuthState {
    START, LOGIN, REGISTER, HOME
}


@Composable
fun LoginScreen(
    authViewModel: AuthViewModel, // ◄ RECIBIMOS EL VIEWMODEL AQUÍ
    onLoginSuccess: () -> Unit
) {
    var currentState by remember { mutableStateOf(AuthState.START) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentState,
            // Transición limpia: solo desvanecido (cross-fade), SIN el efecto de escala
            // por defecto que hacía que el logo/contenido "se agrandara" al cambiar de pantalla.
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "auth_transition"
        ) { state ->
            when (state) {
                AuthState.START -> StartView(
                    onLoginClick = { currentState = AuthState.LOGIN },
                    onGuestClick = {
                        authViewModel.entrarComoInvitado() // Cambia el estado a true
                        onLoginSuccess()
                    }
                )
                AuthState.LOGIN -> LoginView(
                    authViewModel = authViewModel,
                    onBack = { currentState = AuthState.START },
                    onRegisterClick = { currentState = AuthState.REGISTER },
                    onLoginSuccess = { onLoginSuccess() }
                )
                AuthState.REGISTER -> RegisterView(
                    authViewModel = authViewModel, // ◄ SE LO PASAMOS A LA VISTA DE REGISTRO
                    onBack = {
                        authViewModel.resetRegisterState()
                        currentState = AuthState.LOGIN
                    },
                    onRegisterSuccess = { onLoginSuccess() } // Si se registra, entra directo
                )
                AuthState.HOME -> { /* Ya no navegamos internamente a Home */ }
            }
        }
    }
}

@Composable
fun StartView(onLoginClick: () -> Unit, onGuestClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "AguaMap",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primary
            )
            Text(
                "San Juan de Lurigancho",
                fontSize = 16.sp,
                color = primary.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "¡Bienvenido a AguaMap!",
                style = MaterialTheme.typography.headlineMedium,
                color = primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Tu guía para encontrar fuentes de agua confiables en SJL. Únete a la comunidad de Guardianes del Agua.",
                style = MaterialTheme.typography.bodyLarge,
                color = primary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondary)
            ) {
                Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Continuar como invitado",
                color = primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onGuestClick() },
                textDecoration = TextDecoration.Underline
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun LoginView(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,           //
    onRegisterClick: () -> Unit,   //
    onLoginSuccess: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Mensajes de error por campo (null = válido / aún sin validar)
    var correoError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val context = LocalContext.current

    // 1. Escuchamos de forma reactiva el estado del Login desde el ViewModel
    val loginState by authViewModel.loginState.collectAsState()

    // 2. Manejo de navegación o alertas según lo que responda el repositorio
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                authViewModel.resetLoginState()
                onLoginSuccess() // Entra a la app con datos cargados
            }
            is LoginState.Error -> {
                Toast.makeText(context, (loginState as LoginState.Error).error, Toast.LENGTH_LONG).show()
                authViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Iniciar Sesión", onBack = onBack)

        Spacer(modifier = Modifier.height(40.dp))

        AuthTextField(
            value = correo,
            onValueChange = { correo = it; correoError = null },
            label = "Correo",
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            isError = correoError != null,
            errorMessage = correoError
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            isError = passwordError != null,
            errorMessage = passwordError
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Control de UI: Si está cargando muestra el Spinner, si no, el Botón
        if (loginState is LoginState.Loading) {
            CircularProgressIndicator(color = primary)
        } else {
            Button(
                onClick = {
                    // Validación de correo y contraseña antes de enviar
                    correoError = when {
                        correo.isBlank() -> "Ingresa tu correo"
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() ->
                            "Correo no válido"
                        else -> null
                    }
                    passwordError = if (password.isBlank()) "Ingresa tu contraseña" else null

                    if (correoError == null && passwordError == null) {
                        // Enviamos los datos para validar en el ViewModel
                        authViewModel.iniciarSesion(
                            email = correo.trim(),
                            contrasenia = password.trim()
                        )
                    } else {
                        Toast.makeText(context, "Revisa los campos marcados", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text("Ingresar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("¿Nuevo en SJL? ", color = primary.copy(alpha = 0.7f))
            Text(
                "Crea una cuenta",
                color = secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}

/*@Composable
fun LoginView(onBack: () -> Unit, onRegisterClick: () -> Unit, onLoginSuccess: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Iniciar Sesión", onBack = onBack)

        Spacer(modifier = Modifier.height(40.dp))

        AuthTextField(value = userId, onValueChange = { userId = it }, label = "Usuario", icon = Icons.Default.Person)
        AuthTextField(value = password, onValueChange = { password = it }, label = "Contraseña", icon = Icons.Default.Lock, isPassword = true)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "¿Olvidaste tu contraseña?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            color = secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLoginSuccess() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary)
        ) {
            Text("Ingresar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("¿Nuevo en SJL? ", color = primary.copy(alpha = 0.7f))
            Text(
                "Crea una cuenta",
                color = secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}*/



@Composable
fun RegisterView(
    authViewModel: AuthViewModel, // ◄ CAMBIO
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit // ◄ CAMBIO
) {
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Mensajes de error por campo (null = campo válido / aún sin validar)
    var nombreError by remember { mutableStateOf<String?>(null) }
    var dniError by remember { mutableStateOf<String?>(null) }
    var correoError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }
    var usuarioError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Aceptación de términos y condiciones (obligatorio para registrarse)
    var aceptaTerminos by remember { mutableStateOf(false) }
    // Controla la visibilidad del modal de términos y condiciones
    var mostrarDialogoTerminos by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val context = LocalContext.current

    // Escuchamos de forma reactiva el estado de la petición desde el ViewModel
    val registerState by authViewModel.registerState.collectAsState()

    // Manejo de eventos según el estado del servidor
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterState.Success -> {
                Toast.makeText(context, (registerState as RegisterState.Success).message, Toast.LENGTH_LONG).show()
                authViewModel.resetRegisterState()
                onRegisterSuccess() // Mandamos al usuario dentro de la app
            }
            is RegisterState.Error -> {
                Toast.makeText(context, (registerState as RegisterState.Error).error, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Registro", onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = nombre,
            onValueChange = { nombre = it; nombreError = null },
            label = "Nombre completo",
            icon = Icons.Default.Badge,
            isError = nombreError != null,
            errorMessage = nombreError
        )
        AuthTextField(
            value = dni,
            // Solo dígitos, máximo 8 (DNI peruano)
            onValueChange = { dni = it.filter { c -> c.isDigit() }.take(8); dniError = null },
            label = "DNI",
            icon = Icons.Default.Fingerprint,
            keyboardType = KeyboardType.Number,
            isError = dniError != null,
            errorMessage = dniError
        )
        AuthTextField(
            value = correo,
            onValueChange = { correo = it; correoError = null },
            label = "Correo electrónico",
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            isError = correoError != null,
            errorMessage = correoError
        )
        AuthTextField(
            value = telefono,
            // Solo dígitos, máximo 9 (móvil peruano)
            onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(9); telefonoError = null },
            label = "Teléfono",
            icon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            isError = telefonoError != null,
            errorMessage = telefonoError
        )
        AuthTextField(
            value = userId,
            // Usuario sin espacios
            onValueChange = { userId = it.filter { c -> !c.isWhitespace() }; usuarioError = null },
            label = "Nombre de usuario",
            icon = Icons.Default.Person,
            isError = usuarioError != null,
            errorMessage = usuarioError
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            isError = passwordError != null,
            errorMessage = passwordError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Términos y condiciones: sin marcar esta casilla no se puede registrar.
        // Para marcarla hay que abrir el modal y pulsar "Acepto".
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = aceptaTerminos,
                onCheckedChange = { marcado ->
                    if (marcado) {
                        // Para aceptar primero debe leer el modal
                        mostrarDialogoTerminos = true
                    } else {
                        // Permitimos desmarcar directamente
                        aceptaTerminos = false
                    }
                },
                colors = CheckboxDefaults.colors(checkedColor = secondary)
            )
            Text(
                buildAnnotatedString {
                    append("Acepto los ")
                    withStyle(SpanStyle(color = secondary, fontWeight = FontWeight.Bold)) {
                        append("Términos y Condiciones")
                    }
                },
                color = primary.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.clickable { mostrarDialogoTerminos = true }
            )
        }

        // Modal con el contenido completo de los términos
        if (mostrarDialogoTerminos) {
            TerminosCondicionesDialog(
                onAceptar = {
                    aceptaTerminos = true
                    mostrarDialogoTerminos = false
                },
                onCerrar = { mostrarDialogoTerminos = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Si está cargando, cambiamos el botón por un indicador de carga circular
        if (registerState is RegisterState.Loading) {
            CircularProgressIndicator(color = secondary)
        } else {
            Button(
                enabled = aceptaTerminos,
                onClick = {
                    // Validación por campo. Devuelve el mensaje de error o null si es válido.
                    nombreError = if (nombre.isBlank()) "Ingresa tu nombre completo" else null
                    correoError = when {
                        correo.isBlank() -> "Ingresa tu correo"
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() ->
                            "Correo no válido"
                        else -> null
                    }
                    dniError = when {
                        dni.isBlank() -> "Ingresa tu DNI"
                        dni.length != 8 -> "El DNI debe tener 8 dígitos"
                        else -> null
                    }
                    telefonoError = when {
                        telefono.isBlank() -> "Ingresa tu teléfono"
                        telefono.length != 9 -> "El teléfono debe tener 9 dígitos"
                        else -> null
                    }
                    usuarioError = if (userId.isBlank()) "Ingresa un nombre de usuario" else null
                    passwordError = when {
                        password.isBlank() -> "Ingresa una contraseña"
                        password.length < 6 -> "Mínimo 6 caracteres"
                        else -> null
                    }

                    val todoValido = nombreError == null && correoError == null &&
                        dniError == null && telefonoError == null &&
                        usuarioError == null && passwordError == null

                    if (todoValido) {
                        authViewModel.registrarUsuario(
                            email = correo.trim(),
                            contrasenia = password.trim(),
                            nombre = nombre.trim(),
                            dni = dni.trim(),
                            telefono = telefono.trim(),
                            usuario = userId.trim()
                        )
                    } else {
                        Toast.makeText(context, "Revisa los campos marcados", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondary)
            ) {
                Text("Registrarse", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Modal con los Términos y Condiciones de AguaMap.
 * El usuario debe pulsar "Acepto" para poder continuar con el registro.
 */
@Composable
fun TerminosCondicionesDialog(
    onAceptar: () -> Unit,
    onCerrar: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val introduccion =
        "Los presentes Términos y Condiciones regulan de forma vinculante el acceso y uso " +
        "de la aplicación móvil AguaMap (en adelante, \"la Aplicación\"), desarrollada como un " +
        "proyecto de ingeniería de software con fines estrictamente académicos e informativos. " +
        "Al acceder, registrarse o interactuar con la Aplicación, el usuario acepta de manera " +
        "expresa y sin reservas todas las estipulaciones descritas en este documento."

    // Cada sección es (título, cuerpo)
    val secciones = listOf(
        "1. Naturaleza del Servicio y Limitación de Responsabilidad Académica" to
            "Ámbito del Proyecto: El usuario reconoce que AguaMap es un prototipo funcional en fase " +
            "de desarrollo. La información cartográfica sobre puntos de abastecimiento, horarios o " +
            "estados operativos se genera de manera colaborativa (crowdsourcing) y mediante " +
            "simulaciones controladas.\n\n" +
            "Exclusión de SEDAPAL o Entidades Oficiales: La Aplicación no guarda relación jurídica, " +
            "comercial ni oficial con SEDAPAL ni con el Ministerio de Vivienda, Construcción y " +
            "Saneamiento. Los desarrolladores no se hacen responsables por decisiones vecinales " +
            "tomadas con base en la disponibilidad o calidad del agua mostrada en la interfaz.",

        "2. Registro de Cuenta y Jerarquía de Roles" to
            "Veracidad de las Credenciales: Para adquirir el rol de \"Ciudadano Registrado\" y emitir " +
            "reportes de fallas, el usuario debe proveer datos mínimos obligatorios (nombre, correo " +
            "electrónico y DNI). El usuario es el único responsable de la confidencialidad de sus " +
            "credenciales de acceso autenticadas mediante Supabase Auth.\n\n" +
            "Uso del Rol de Invitado: Los usuarios que opten por el acceso en rol de \"Invitado\" " +
            "operarán bajo una modalidad de solo lectura, no estando facultados para emitir reportes, " +
            "comentarios ni valoraciones hídricas.",

        "3. Reglas de Negocio y Georreferenciación Obligatoria" to
            "Validación de Proximidad por GPS: Al enviar una alerta o reporte de avería física hídrica, " +
            "el usuario acepta que la Aplicación acceda temporalmente al sensor GPS de su terminal. El " +
            "envío del formulario queda condicionado por un algoritmo perimetral que exige una " +
            "proximidad física inferior a 100 metros respecto al pozo o contenedor georreferenciado.\n\n" +
            "Captura de Evidencia Fotográfica: El registro de incidencias hídricas críticas exige " +
            "capturar una fotografía en tiempo real desde la cámara del terminal. El usuario asume toda " +
            "la responsabilidad civil y penal por las imágenes capturadas, prohibiéndose la carga de " +
            "archivos ajenos a la avería o que vulneren derechos de terceros.",

        "4. Política de Moderación Léxica y Uso Comunitario" to
            "Moderación Automatizada: El software cuenta con filtros lógicos integrados que examinan " +
            "los campos de comentarios y descripciones de fallas. Queda prohibida la inserción de " +
            "lenguaje ofensivo, insultos, difamaciones o expresiones que atenten contra las normas " +
            "comunitarias vecinales.\n\n" +
            "Sanciones por Falsificación: El equipo administrador se reserva el derecho de suspender " +
            "temporal o definitivamente las cuentas de aquellos ciudadanos que simulen repetitivamente " +
            "coordenadas falsas o emitan reportes deliberadamente erróneos.",

        "5. Privacidad y Tratamiento de Datos Personales (Privacy by Design)" to
            "Cumplimiento de la Ley N° 29733: Toda la información recolectada es tratada bajo principios " +
            "estrictos de confidencialidad y minimización de identidades. Las contraseñas se almacenan " +
            "mediante hashes encriptados inaccesibles en el servidor central de Supabase.\n\n" +
            "Gestión Efímera del GPS: La geolocalización se lee de forma efímera únicamente en el " +
            "instante preciso de la transacción del reporte vecinal. La Aplicación bajo ninguna " +
            "circunstancia rastrea movimientos en segundo plano ni almacena un histórico geográfico de " +
            "las rutas de los usuarios.",

        "6. Modificaciones de los Términos" to
            "Al tratarse de una iteración ágil bajo el marco Scrum, los desarrolladores se reservan la " +
            "facultad de modificar o escalar las reglas técnicas, funcionales y lógicas de este " +
            "documento con el fin de optimizar el rendimiento y la interoperabilidad de la base de " +
            "datos distribuida."
    )

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                "Términos y Condiciones de Uso",
                fontWeight = FontWeight.Bold,
                color = primary,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    introduccion,
                    fontSize = 13.sp,
                    color = primary.copy(alpha = 0.85f)
                )
                secciones.forEach { (titulo, cuerpo) ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        cuerpo,
                        fontSize = 13.sp,
                        color = primary.copy(alpha = 0.85f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondary)
            ) {
                Text("Acepto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar", color = primary)
            }
        }
    )
}

@Composable
fun HeaderSection(title: String, onBack: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = primary)
        }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = primary)
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = secondary) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = primary,
            unfocusedTextColor = primary,
            focusedBorderColor = secondary,
            unfocusedBorderColor = primary.copy(alpha = 0.1f),
            focusedLabelColor = secondary,
            unfocusedLabelColor = primary.copy(alpha = 0.5f)
        )
    )
}