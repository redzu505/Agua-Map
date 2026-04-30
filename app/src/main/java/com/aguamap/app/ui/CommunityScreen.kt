package com.aguamap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommunityScreen() {
    // Nueva Paleta Acuática (Clara)
    val bgLight = Color(0xFFF0F9FF)     // Azul muy claro (Agua)
    val primaryBlue = Color(0xFF03A9F4) // Celeste Brillante
    val darkBlue = Color(0xFF01579B)    // Azul Océano Profundo
    val cardBg = Color.White

    Box(modifier = Modifier.fillMaxSize().background(bgLight)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // Header con colores claros y frescos
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Comunidad SJL",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = darkBlue
                    )
                    Text(
                        "Información en tiempo real sobre el agua en tu zona.",
                        fontSize = 14.sp,
                        color = darkBlue.copy(alpha = 0.6f)
                    )
                }
            }

            // Reportes Recientes
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "REPORTES RECIENTES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue,
                            letterSpacing = 1.sp
                        )
                        Text("Ver todo", fontSize = 12.sp, color = darkBlue, fontWeight = FontWeight.Bold)
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            LightReportCard(
                                title = "Fuente Los Postes",
                                status = "Operativo",
                                time = "Hace 5 min",
                                location = "Estación Metro",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                textColor = darkBlue
                            )
                        }
                        item {
                            LightReportCard(
                                title = "Parque Huiracocha",
                                status = "Baja Presión",
                                time = "Hace 1h",
                                location = "Zona 3",
                                icon = Icons.Default.Warning,
                                iconColor = Color(0xFFFFA000),
                                textColor = darkBlue
                            )
                        }
                    }
                }
            }

            // Tip del Día (Banner con degradado suave)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.background(
                        Brush.horizontalGradient(
                            listOf(primaryBlue.copy(alpha = 0.2f), Color.White)
                        )
                    ).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TIP DEL DÍA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                                }
                                Text("Duchas de 5 minutos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = darkBlue)
                                Text(
                                    "Ahorra hasta 40 litros de agua por sesión. ¡Cuidemos SJL!",
                                    fontSize = 12.sp,
                                    color = darkBlue.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = primaryBlue.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }
            }

            // Noticias de Infraestructura (Estilo Blanco Limpio)
            item {
                LightNewsCard(
                    title = "Nueva red de fuentes en Zárate",
                    description = "Se han instalado 15 nuevas fuentes de agua potable cerca a la Av. Gran Chimú.",
                    date = "2026-03-28",
                    accentColor = primaryBlue,
                    textColor = darkBlue
                )
            }

            // Actividad de Usuarios (Feed Claro)
            item {
                LightActivityCard(
                    user = "Lucía Rodríguez",
                    time = "Hace 3h",
                    content = "La fuente en la Estación Caja de Agua tiene una pequeña fuga. ¡Tomen precauciones!",
                    likes = 24,
                    comments = 5,
                    accentColor = primaryBlue,
                    textColor = darkBlue
                )
            }

            item {
                LightNewsCard(
                    title = "Mantenimiento Programado",
                    description = "Sedapal realizará limpieza de reservorios en Campoy este fin de semana.",
                    date = "2026-04-02",
                    accentColor = primaryBlue,
                    textColor = darkBlue
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun LightReportCard(title: String, status: String, time: String, location: String, icon: ImageVector, iconColor: Color, textColor: Color) {
    Card(
        modifier = Modifier.width(170.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
            Text(location, color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status, color = Color.Gray, fontSize = 10.sp)
                Text(time, color = Color.LightGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun LightNewsCard(title: String, description: String, date: String, accentColor: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = accentColor.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(description, color = Color.DarkGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(date, color = Color.Gray, fontSize = 11.sp)
                    Text("Leer más >", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LightActivityCard(user: String, time: String, content: String, likes: Int, comments: Int, accentColor: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(user, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Reportó • $time", color = Color.Gray, fontSize = 11.sp)
                }
            }
            Text(content, color = Color.DarkGray, fontSize = 14.sp, lineHeight = 20.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" $likes", color = Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" $comments", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}
