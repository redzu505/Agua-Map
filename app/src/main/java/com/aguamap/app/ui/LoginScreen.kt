package com.aguamap.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.ui.theme.AguaMapTheme

enum class AuthState {
    START, LOGIN, REGISTER, HOME
}

@Composable
fun LoginScreen() {
    var currentState by remember { mutableStateOf(AuthState.START) }
    
    // Paleta Acuática (Clara)
    val bgLight = Color(0xFFF0F9FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgLight)
    ) {
        AnimatedContent(
            targetState = currentState,
            label = "auth_transition"
        ) { state ->
            when (state) {
                AuthState.START -> StartView(
                    onLoginClick = { currentState = AuthState.LOGIN },
                    onGuestClick = { currentState = AuthState.HOME }
                )
                AuthState.LOGIN -> LoginView(
                    onBack = { currentState = AuthState.START },
                    onRegisterClick = { currentState = AuthState.REGISTER },
                    onLoginSuccess = { currentState = AuthState.HOME }
                )
                AuthState.REGISTER -> RegisterView(
                    onBack = { currentState = AuthState.LOGIN }
                )
                AuthState.HOME -> HomeScreen()
            }
        }
    }
}

@Composable
fun StartView(onLoginClick: () -> Unit, onGuestClick: () -> Unit) {
    val oceanBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = celeste
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "AguaMap",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = oceanBlue
            )
            Text(
                "San Juan de Lurigancho",
                fontSize = 16.sp,
                color = oceanBlue.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Welcome Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "¡Bienvenido a AguaMap!",
                style = MaterialTheme.typography.headlineMedium,
                color = oceanBlue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Tu guía para encontrar fuentes de agua confiables en SJL. Únete a la comunidad de Guardianes del Agua.",
                style = MaterialTheme.typography.bodyLarge,
                color = oceanBlue.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = celeste)
            ) {
                Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Continuar como invitado",
                color = oceanBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onGuestClick() },
                textDecoration = TextDecoration.Underline
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun LoginView(onBack: () -> Unit, onRegisterClick: () -> Unit, onLoginSuccess: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val oceanBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

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
            color = celeste,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLoginSuccess() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = oceanBlue)
        ) {
            Text("Ingresar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("¿Nuevo en SJL? ", color = oceanBlue.copy(alpha = 0.7f))
            Text(
                "Crea una cuenta",
                color = celeste,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}

@Composable
fun RegisterView(onBack: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val oceanBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Registro de Guardián", onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(nombre, { nombre = it }, "Nombre completo", Icons.Default.Badge)
        AuthTextField(dni, { dni = it }, "DNI", Icons.Default.Fingerprint)
        AuthTextField(correo, { correo = it }, "Correo electrónico", Icons.Default.Email)
        AuthTextField(telefono, { telefono = it }, "Teléfono", Icons.Default.Phone)
        AuthTextField(userId, { userId = it }, "Nombre de usuario", Icons.Default.Person)
        AuthTextField(password, { password = it }, "Contraseña", Icons.Default.Lock, isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Lógica de Registro */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = celeste)
        ) {
            Text("Registrarse", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderSection(title: String, onBack: () -> Unit) {
    val oceanBlue = Color(0xFF01579B)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = oceanBlue)
        }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = oceanBlue)
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    val oceanBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = celeste) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = oceanBlue,
            unfocusedTextColor = oceanBlue,
            focusedBorderColor = celeste,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = celeste,
            unfocusedLabelColor = oceanBlue.copy(alpha = 0.5f)
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    AguaMapTheme {
        LoginScreen()
    }
}
