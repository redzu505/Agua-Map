package com.aguamap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.util.TipsProvider
import com.aguamap.app.viewmodel.HomeViewModel

@Composable
fun CommunityScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val recentReports by homeViewModel.recentReports.collectAsState()
    val waterPoints by homeViewModel.waterPoints.collectAsState()

    // Tip del día: se elige según el día del año desde assets/tips.json
    val context = LocalContext.current
    val tipDelDia = remember { TipsProvider.tipDelDia(context) }

    LaunchedEffect(Unit) {
        homeViewModel.loadCommunityData()
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primary)
                        }
                        Text(
                            "Comunidad SJL",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primary
                        )
                    }
                    Text(
                        "Información en tiempo real sobre el agua en tu zona.",
                        modifier = Modifier.padding(start = 48.dp),
                        fontSize = 14.sp,
                        color = primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Reportes Recientes (REALES: salen de los reportes que crea la gente)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "REPORTES RECIENTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondary,
                        letterSpacing = 1.sp
                    )

                    if (recentReports.isEmpty()) {
                        Text(
                            "Aún no hay reportes de la comunidad. ¡Sé el primero en reportar un punto de agua!",
                            fontSize = 13.sp,
                            color = primary.copy(alpha = 0.6f)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentReports) { report ->
                                val pointName = waterPoints.find { it.id == report.pointId }?.name
                                    ?: "Punto reportado"
                                RealReportCard(
                                    report = report,
                                    pointName = pointName,
                                    textColor = primary,
                                    onClick = { onNavigateToDetail(report.pointId) }
                                )
                            }
                        }
                    }
                }
            }

            // Tip del Día (Banner con degradado suave)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.background(
                        Brush.horizontalGradient(
                            listOf(secondary.copy(alpha = 0.2f), surface)
                        )
                    ).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TIP DEL DÍA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondary)
                                }
                                Text(tipDelDia.titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)
                                Text(
                                    tipDelDia.descripcion,
                                    fontSize = 12.sp,
                                    color = primary.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = secondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }
            }

            // NUEVOS PUNTOS DE AGUA (se generan automáticamente al crear un punto)
            item {
                Text(
                    "NUEVOS PUNTOS DE AGUA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondary,
                    letterSpacing = 1.sp
                )
            }

            if (waterPoints.isEmpty()) {
                item {
                    Text(
                        "Aún no hay puntos de agua registrados.",
                        fontSize = 13.sp,
                        color = primary.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(waterPoints) { point ->
                    NewPointCard(
                        point = point,
                        textColor = primary,
                        accentColor = secondary,
                        onClick = { onNavigateToDetail(point.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RealReportCard(report: WaterPointReport, pointName: String, textColor: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.width(210.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Nombre del punto reportado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    pointName,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Tipo de problema reportado
            Text(
                report.type.displayName,
                color = Color(0xFFC0392B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Descripción (si la hay)
            if (report.description.isNotBlank()) {
                Text(
                    report.description,
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Fecha
            Text(report.date, color = Color.LightGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun NewPointCard(point: WaterPoint, textColor: Color, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("NUEVO PUNTO DE AGUA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Text(
                    point.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    point.address,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(point.type.displayName, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Medium)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.3f)
            )
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

