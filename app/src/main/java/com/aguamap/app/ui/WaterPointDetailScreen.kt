package com.aguamap.app.ui

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aguamap.app.R
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.ReportType
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import com.aguamap.app.util.DateUtils
import com.aguamap.app.util.LocationService
import com.aguamap.app.util.LocationUtils
import com.aguamap.app.viewmodel.HomeViewModel
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterPointDetailScreen(
    pointId: String,
    homeViewModel: HomeViewModel,
    isGuest: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    
    val comments by homeViewModel.comments.collectAsState()
    val reports by homeViewModel.reports.collectAsState()
    val miValoracion by homeViewModel.miValoracion.collectAsState()
    val waterPointsState by homeViewModel.waterPoints.collectAsState()
    val favoritos by homeViewModel.favoritos.collectAsState()
    val esFavorito = pointId in favoritos
    
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pointId) {
        homeViewModel.loadDetails(pointId)
        if (LocationUtils.hasLocationPermissions(context)) {
            userLocation = locationService.getCurrentLocation(context)
        }
    }

    val point = remember(userLocation, waterPointsState) {
        waterPointsState.find { it.id == pointId }?.let { p ->
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

    val rawDistance = remember(userLocation, point) {
        userLocation?.let { loc ->
            point?.let { p ->
                LocationUtils.calculateDistance(loc.latitude, loc.longitude, p.latitude, p.longitude)
            }
        }
    }
    val isNearEnough = rawDistance != null && rawDistance <= 0.1 // 0.1 km = 100 metros

    if (showReportDialog) {
        ReportDialog(
            pointName = point.name,
            onDismiss = { showReportDialog = false },
            onSend = { type, desc, imageUri ->
                // 1. Leemos los bytes para el envío inmediato si hay red
                val imageBytes = imageUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                }

                // 2. --- NUEVO: Copiar imagen a caché interna para persistencia offline ---
                // El Photo Picker pierde permisos al cerrar la app; guardamos una copia física.
                val persistentPath = imageUri?.let { uri ->
                    try {
                        val fileName = "img_offline_${UUID.randomUUID()}.jpg"
                        val file = java.io.File(context.cacheDir, fileName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        file.absolutePath // Guardamos la ruta física real (/data/user/0/...)
                    } catch (e: Exception) {
                        imageUri.toString() // Fallback a la URI original si algo falla
                    }
                }

                homeViewModel.addReport(
                    WaterPointReport(
                        id = UUID.randomUUID().toString(),
                        pointId = pointId,
                        type = type,
                        description = desc,
                        date = DateUtils.fechaHoraActual(),
                        imageUrl = persistentPath // Usamos la ruta física persistente
                    ),
                    imageBytes = imageBytes
                )
                showReportDialog = false
            }
        )
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
                actions = {
                    // Botón de guardar (favorito). Solo para usuarios registrados.
                    if (!isGuest) {
                        IconButton(onClick = { homeViewModel.toggleFavorito(pointId) }) {
                            Icon(
                                imageVector = if (esFavorito) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (esFavorito) "Quitar de guardados" else "Guardar punto",
                                tint = secondary
                            )
                        }
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
                                modifier = Modifier.weight(1f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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

            Text(stringResource(R.string.label_exact_location), fontWeight = FontWeight.Bold, color = primary, fontSize = 18.sp)
            val latStr = String.format(Locale.getDefault(), "%.6f", point.latitude)
            val lngStr = String.format(Locale.getDefault(), "%.6f", point.longitude)
            Text("${stringResource(R.string.label_latitude)}: $latStr\n${stringResource(R.string.label_longitude)}: $lngStr", color = primary.copy(alpha = 0.6f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = primary.copy(alpha = 0.1f))

            // SECCIÓN DE VALORACIÓN (solo usuarios registrados)
            if (!isGuest) {
                RatingSelector(
                    miValoracion = miValoracion,
                    promedio = point.rating,
                    onRate = { estrellas -> homeViewModel.valorarPunto(pointId, estrellas) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = primary.copy(alpha = 0.1f))
            }

            // SECCIÓN DE REPORTES ACTIVOS
            if (reports.isNotEmpty()) {
                ReportsSection(reports)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = primary.copy(alpha = 0.1f))
            }

            // SECCIÓN DE COMENTARIOS
            CommentSection(
                comments = comments,
                isGuest = isGuest,
                onAddComment = { text ->
                    // Sin puntaje por ahora: se guarda 0 para no arrastrar valoraciones falsas
                    homeViewModel.addComment(pointId, text, 0)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // BOTÓN DE RUTA POR CALLES (OSRM)
            Button(
                onClick = {
                    homeViewModel.setRouteDestination(point)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver ruta por calles")
            }

            if (!isGuest) {
                // Solo usuarios registrados pueden reportar problemas si están cerca (100m)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showReportDialog = true },
                        enabled = isNearEnough,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondary,
                            disabledContainerColor = secondary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.Report, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_report_problem))
                    }
                    
                    if (!isNearEnough) {
                        Text(
                            "Debes estar a menos de 100 metros del punto para reportar.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            } else {
                // Aviso para invitados
                GuestActionHint(stringResource(R.string.guest_hint_report))
            }
        }
    }
}

/**
 * Mensaje que se muestra a los invitados cuando una acción está bloqueada.
 */
@Composable
fun GuestActionHint(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Selector de estrellas para valorar un punto (1-5). Muestra el voto actual del
 * usuario y el promedio del punto. Al tocar una estrella, envía/actualiza el voto.
 */
@Composable
fun RatingSelector(
    miValoracion: Int?,
    promedio: Double,
    onRate: (Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val estrella = Color(0xFFFFB300)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tu valoración", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            (1..5).forEach { i ->
                Icon(
                    imageVector = if (miValoracion != null && i <= miValoracion) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Valorar con $i estrella(s)",
                    tint = estrella,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onRate(i) }
                        .padding(4.dp)
                )
            }
        }
        Text(
            text = if (miValoracion == null) {
                "Toca una estrella para valorar. Promedio actual: $promedio ★"
            } else {
                "Tu voto: $miValoracion ★  ·  Promedio: $promedio ★"
            },
            fontSize = 12.sp,
            color = primary.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ReportsSection(reports: List<WaterPointReport>) {
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE67E22), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reportes de la comunidad", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primary)
        }
        reports.forEach { report ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEDEC)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.type.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFC0392B))
                            Text(report.description, fontSize = 13.sp, color = Color.DarkGray)
                        }
                        Text(report.date, fontSize = 11.sp, color = Color.Gray)
                    }
                    // Si el reporte trae foto adjunta (subida a Supabase Storage), la mostramos
                    if (!report.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = report.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentSection(
    comments: List<Comment>,
    isGuest: Boolean = false,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.label_comments), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primary)

        if (comments.isEmpty()) {
            Text(stringResource(R.string.label_no_comments), color = Color.Gray, fontSize = 14.sp)
        } else {
            comments.forEach { comment ->
                CommentItem(comment)
            }
        }

        if (!isGuest) {
            // Solo usuarios registrados pueden comentar
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.label_share_experience), fontSize = 14.sp) },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.btn_publish), tint = primary)
                    }
                }
            )
        } else {
            GuestActionHint(stringResource(R.string.guest_hint_comment))
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text(comment.date, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ReportDialog(
    pointName: String,
    onDismiss: () -> Unit,
    onSend: (ReportType, String, Uri?) -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf<ReportType?>(null) }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Uri temporal donde la app de cámara escribirá la foto tomada
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Selector de imagen de la galería del teléfono
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    // Lanzador de la cámara: si la captura fue exitosa, usamos la foto tomada
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) imageUri = cameraImageUri
    }

    // Lanzador del permiso de cámara: si se concede, abrimos la cámara al instante
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val uri = crearUriTemporalFoto(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                "Necesitas dar permiso de cámara para tomar la foto",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Abre la cámara pidiendo permiso solo si aún no está concedido
    fun abrirCamara() {
        val permisoConcedido = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permisoConcedido) {
            val uri = crearUriTemporalFoto(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { selectedType?.let { onSend(it, description, imageUri) } },
                enabled = selectedType != null && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.btn_send_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color.Gray)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE67E22))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.report_dialog_title), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.report_dialog_subtitle, pointName),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Text(stringResource(R.string.report_label_type), fontWeight = FontWeight.Bold)
                
                ReportType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                        Text(type.displayName, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
                    }
                }
                
                Text(stringResource(R.string.report_label_description), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.report_hint_description), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3
                )

                // --- Adjuntar foto del problema (opcional) ---
                Text(stringResource(R.string.report_label_photo), fontWeight = FontWeight.Bold)
                if (imageUri != null) {
                    // Vista previa de la foto seleccionada
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = { imageUri = null }) {
                        Text(stringResource(R.string.report_remove_photo), color = Color.Gray)
                    }
                } else {
                    // Dos opciones: tomar una foto con la cámara o elegir una de la galería
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { abrirCamara() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    )
}

/**
 * Crea un archivo temporal en la caché interna y devuelve una Uri segura (FileProvider)
 * que la app de cámara puede usar para escribir la foto tomada.
 */
private fun crearUriTemporalFoto(context: Context): Uri {
    val carpeta = File(context.cacheDir, "report_photos").apply { mkdirs() }
    val archivo = File(carpeta, "captura_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        archivo
    )
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
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
