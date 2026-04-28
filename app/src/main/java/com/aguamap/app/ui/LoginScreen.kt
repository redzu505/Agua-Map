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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    START, LOGIN, REGISTER
}

@Composable
fun LoginScreen() {
    var currentState by remember { mutableStateOf(AuthState.START) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00747A), Color(0xFF0C141F)),
                    center = androidx.compose.ui.geometry.Offset(1000f, 0f),
                    radius = 2000f
                )
            )
    ) {
        AnimatedContent(
            targetState = currentState,
            label = "auth_transition"
        ) { state ->
            when (state) {
                AuthState.START -> StartView(
                    onLoginClick = { currentState = AuthState.LOGIN },
                    onGuestClick = { /* Navegar a Vista Principal */ }
                )
                AuthState.LOGIN -> LoginView(
                    onBack = { currentState = AuthState.START },
                    onRegisterClick = { currentState = AuthState.REGISTER }
                )
                AuthState.REGISTER -> RegisterView(
                    onBack = { currentState = AuthState.LOGIN }
                )
            }
        }
    }
}

@Composable
fun StartView(onLoginClick: () -> Unit, onGuestClick: () -> Unit) {
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
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFF3EDAE3)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "AguaMap",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3EDAE3)
            )
        }

        // Welcome Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Encuentra puntos de agua cercanos",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Únete a nuestra comunidad para localizar fuentes de agua potable en tiempo real.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2CF5))
            ) {
                Text("Iniciar Sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Continuar como invitado",
                color = Color(0xFF3EDAE3),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onGuestClick() },
                textDecoration = TextDecoration.Underline
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun LoginView(onBack: () -> Unit, onRegisterClick: () -> Unit) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Bienvenido de nuevo", onBack = onBack)
        
        Spacer(modifier = Modifier.height(40.dp))

        AuthTextField(value = userId, onValueChange = { userId = it }, label = "ID de Usuario", icon = Icons.Default.Person)
        AuthTextField(value = password, onValueChange = { password = it }, label = "Contraseña", icon = Icons.Default.Lock, isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Lógica de Login */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2CF5))
        ) {
            Text("Ingresar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("¿No tienes cuenta? ", color = Color.White.copy(alpha = 0.7f))
            Text(
                "Regístrate",
                color = Color(0xFF3EDAE3),
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

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(title = "Crear Cuenta", onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(nombre, { nombre = it }, "Nombre completo", Icons.Default.Badge)
        AuthTextField(dni, { dni = it }, "DNI", Icons.Default.Fingerprint)
        AuthTextField(correo, { correo = it }, "Correo electrónico", Icons.Default.Email)
        AuthTextField(telefono, { telefono = it }, "Teléfono", Icons.Default.Phone)
        AuthTextField(userId, { userId = it }, "ID de Usuario", Icons.Default.Person)
        AuthTextField(password, { password = it }, "Contraseña", Icons.Default.Lock, isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Lógica de Registro */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3EDAE3), contentColor = Color(0xFF0C141F))
        ) {
            Text("Registrarse", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderSection(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF3EDAE3)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF3EDAE3),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = Color(0xFF3EDAE3),
            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
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
