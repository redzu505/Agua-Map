package com.aguamap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.R
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import com.aguamap.app.util.LocationService
import com.aguamap.app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWaterPointScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var selectedType by remember { mutableStateOf(WaterPointType.FUENTE) }
    var hours by remember { mutableStateOf("") }
    
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_point_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Ayuda a la comunidad registrando un nuevo punto de hidratación en SJL.",
                fontSize = 14.sp,
                color = primary.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del lugar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección aproximada") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            val loc = locationService.getCurrentLocation(context)
                            loc?.let {
                                latitude = it.latitude
                                longitude = it.longitude
                                if (address.isBlank()) {
                                    address = context.getString(R.string.location_gps_captured)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Usar mi ubicación", tint = secondary)
                    }
                }
            )

            if (latitude != null && longitude != null) {
                val latStr = String.format(Locale.getDefault(), "%.5f", latitude)
                val lngStr = String.format(Locale.getDefault(), "%.5f", longitude)
                Text(
                    "Coordenadas: $latStr, $lngStr",
                    fontSize = 12.sp,
                    color = secondary
                )
            }

            Text("Tipo de Punto", fontWeight = FontWeight.Bold, color = primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WaterPointType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.displayName, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Horario (ej: 24h o 08:00 - 18:00)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    val newPoint = WaterPoint(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        address = address,
                        rating = 5.0,
                        distance = "0m",
                        hours = if (hours.isBlank()) "24h" else hours,
                        status = WaterPointStatus.OPERATIVO,
                        type = selectedType,
                        latitude = latitude ?: -11.9763,
                        longitude = longitude ?: -77.0002
                    )
                    homeViewModel.addWaterPoint(newPoint)
                    onBack() 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondary),
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_register))
            }
        }
    }
}
