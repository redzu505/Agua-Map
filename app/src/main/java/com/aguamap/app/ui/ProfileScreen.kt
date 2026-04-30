package com.aguamap.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.ui.components.GlassCard

@Composable
fun ProfileScreen() {
    val bgLight = Color(0xFFF0F9FF)
    
    Box(modifier = Modifier.fillMaxSize().background(bgLight)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Profile Header Section
            item {
                ProfileHeader()
            }

            // Impact Section (Bento Style)
            item {
                ImpactSection()
            }

            // Saved Points Section
            item {
                SavedPointsSection()
            }

            // Settings Section
            item {
                SettingsSection()
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ProfileHeader() {
    val darkBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(celeste, darkBlue)
                        )
                    )
                    .padding(3.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = celeste.copy(alpha = 0.5f),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
            Surface(
                color = celeste,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Mateo Fernández",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = darkBlue
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                Icons.Default.MilitaryTech,
                contentDescription = null,
                tint = celeste,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Guardián del Agua",
                color = celeste,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ImpactSection() {
    val darkBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Tu Impacto",
            color = darkBlue.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImpactCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                value = "42L",
                label = "Agua consumida",
                accentColor = darkBlue
            )
            ImpactCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Recycling,
                value = "84",
                label = "Botellas evitadas",
                accentColor = celeste
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = celeste.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            tint = celeste,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("12", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = darkBlue)
                        Text("Puntos reportados", fontSize = 14.sp, color = Color.Gray)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun ImpactCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = accentColor)
            Column {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
                Text(label, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SavedPointsSection() {
    val darkBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mis Puntos Guardados",
                color = darkBlue.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Ver todos",
                color = celeste,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        SavedPointItem("Fuente Parque Central", "Potable • 200m", Color(0xFF4CAF50))
        SavedPointItem("Estación Eco-Sustentable", "Filtrada • 1.2km", Color(0xFF4CAF50))
    }
}

@Composable
fun SavedPointItem(title: String, subtitle: String, statusColor: Color) {
    val darkBlue = Color(0xFF01579B)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF03A9F4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Water, contentDescription = null, tint = Color(0xFF03A9F4))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = darkBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(subtitle, color = Color.Gray, fontSize = 14.sp)
                }
            }
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun SettingsSection() {
    val darkBlue = Color(0xFF01579B)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsItem(Icons.Default.Settings, "Ajustes", darkBlue)
        SettingsItem(Icons.Default.Shield, "Privacidad", darkBlue)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { /* Logout */ }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFD32F2F))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Cerrar Sesión", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, label: String, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}
