package com.aguamap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import com.aguamap.app.util.LocationUtils
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.location.Location
import com.aguamap.app.util.LocationService
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.aguamap.app.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterPointDetailScreen(
    pointId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        if (LocationUtils.hasLocationPermissions(context)) {
            userLocation = locationService.getCurrentLocation(context)
        }
    }

    // Mock data lookup - en una app real esto vendría de un ViewModel/Repository
    val rawWaterPoints = listOf(
        WaterPoint("1", "Fuente Los Postes", "Paradero Los Postes, SJL", 4.8, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.FUENTE, -11.9904, -77.0006),
        WaterPoint("2", "Punto Eco-Filter Zárate", "Av. Gran Chimú 452", 4.5, "---", "08:00 - 22:00", WaterPointStatus.OPERATIVO, WaterPointType.FILTRADA, -12.0225, -77.0012),
        WaterPoint("3", "Pozo Huiracocha", "Parque Zonal Huiracocha", 4.2, "---", "Cerrado", WaterPointStatus.MANTENIMIENTO, WaterPointType.POZO, -11.9961, -76.9958),
        WaterPoint("4", "Grifo Caja de Agua", "Estación Caja de Agua", 4.9, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.GRIFO, -12.0272, -77.0142)
    )
    
    val point = remember(userLocation) {
        rawWaterPoints.find { it.id == pointId }?.let { p ->
            val dist = userLocation?.let { loc ->
                LocationUtils.calculateDistance(loc.latitude, loc.longitude, p.latitude, p.longitude)
            }
            p.copy(distance = dist?.let { LocationUtils.formatDistance(it) } ?: "---")
        }
    }

    if (point == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.error_point_not_found))
        }
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    if (point.imageUrl != null) {
                        AsyncImage(
                            model = point.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                point.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primary
                            )
                            StatusBadge(point.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(point.address, color = primary.copy(alpha = 0.7f))
                    }
                }
            }

            // Info Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem(Modifier.weight(1f), Icons.Default.Star, stringResource(R.string.label_rating), point.rating.toString(), secondary)
                InfoItem(Modifier.weight(1f), Icons.Default.Schedule, stringResource(R.string.label_hours), point.hours, secondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem(Modifier.weight(1f), Icons.Default.Place, stringResource(R.string.label_distance), point.distance, secondary)
                InfoItem(Modifier.weight(1f), Icons.Default.Category, stringResource(R.string.label_type), point.type.displayName, secondary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.label_exact_location), fontWeight = FontWeight.Bold, color = primary, fontSize = 18.sp)
            val latStr = String.format(Locale.getDefault(), "%.6f", point.latitude)
            val lngStr = String.format(Locale.getDefault(), "%.6f", point.longitude)
            Text("${stringResource(R.string.label_latitude)}: $latStr\n${stringResource(R.string.label_longitude)}: $lngStr", color = primary.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* Reportar problema */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondary)
            ) {
                Icon(Icons.Default.Report, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_report_problem))
            }
        }
    }
}

@Composable
fun InfoItem(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatusBadge(status: WaterPointStatus) {
    val color = if (status == WaterPointStatus.OPERATIVO) Color(0xFF4CAF50) else Color(0xFFF44336)
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
