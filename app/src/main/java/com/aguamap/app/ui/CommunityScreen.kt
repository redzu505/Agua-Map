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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.ui.components.GlassCard

@Composable
fun CommunityScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Header de Bienvenida
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Comunidad SJL",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD8B9FF)
                )
                Text(
                    "Información en tiempo real sobre el agua en tu zona.",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
        }

        // Fila de Reportes Recientes
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
                        color = Color(0xFF7DFFA2),
                        letterSpacing = 1.sp
                    )
                    Text("Ver todo", fontSize = 12.sp, color = Color(0xFF3EDAE3))
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ReportSmallCard(
                            title = "Fuente Los Postes",
                            status = "Operativo",
                            time = "Hace 5 min",
                            location = "Estación Metro",
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF7DFFA2)
                        )
                    }
                    item {
                        ReportSmallCard(
                            title = "Parque Huiracocha",
                            status = "Baja Presión",
                            time = "Hace 1h",
                            location = "Zona 3",
                            icon = Icons.Default.Warning,
                            iconColor = Color(0xFFFFB4AB)
                        )
                    }
                }
            }
        }

        // Tip del Día (Banner Vibrante)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8B2CF5).copy(alpha = 0.15f), Color(0xFF3EDAE3).copy(alpha = 0.05f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = Color(0xFFD8B9FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TIP DEL DÍA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD8B9FF))
                        }
                        Text("Duchas de 5 minutos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Ahorra hasta 40 litros de agua por sesión. ¡Cuidemos SJL!",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }

        // Noticias de Infraestructura (Estilo Imagen)
        item {
            NewsImageCard(
                title = "Nueva red de fuentes en Zárate",
                description = "Se han instalado 15 nuevas fuentes de agua potable cerca a la Av. Gran Chimú.",
                date = "2026-03-28",
                imageContent = Color(0xFF1E3A5F) // Azul oscuro simulando foto
            )
        }

        // Actividad de Usuarios (Feed Social)
        item {
            ActivityFeedCard(
                user = "Lucía Rodríguez",
                time = "Hace 3h",
                content = "La fuente en la Estación Caja de Agua tiene una pequeña fuga. ¡Tomen precauciones!",
                likes = 24,
                comments = 5,
                tag = "Falla mecánica"
            )
        }

        item {
            NewsImageCard(
                title = "Mantenimiento Programado",
                description = "Sedapal realizará limpieza de reservorios en Campoy este fin de semana.",
                date = "2026-04-02",
                imageContent = Color(0xFF3D2B1F) // Marrón simulando foto de obras
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ReportSmallCard(title: String, status: String, time: String, location: String, icon: ImageVector, iconColor: Color) {
    GlassCard(modifier = Modifier.width(180.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
            Text(status, color = Color.LightGray, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                Text(" $location • $time", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun NewsImageCard(title: String, description: String, date: String, imageContent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(imageContent)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.Center).size(40.dp))
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color(0xFF0C141F), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(description, color = Color.DarkGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(date, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("Leer más >", color = Color(0xFF8B2CF5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActivityFeedCard(user: String, time: String, content: String, likes: Int, comments: Int, tag: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF8B2CF5).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Reportó un problema • $time", color = Color.Gray, fontSize = 11.sp)
                }
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(18.dp))
            }
            
            // Imagen del reporte (simulada)
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                Text(tag, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp)
            }

            Text(content, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Text(" $likes", color = Color.LightGray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Text(" $comments", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}
