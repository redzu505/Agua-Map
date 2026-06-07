package com.aguamap.app.ui

import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val waterPointsState by homeViewModel.waterPoints.collectAsState()
    
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

    if (showReportDialog) {
        ReportDialog(
            pointName = point.name,
            onDismiss = { showReportDialog = false },
            onSend = { type, desc, imageUri ->
                // Si el usuario adjuntó una foto, leemos sus bytes para subirlos a Storage
                val imageBytes = imageUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                }
                homeViewModel.addReport(
                    WaterPointReport(
                        id = UUID.randomUUID().toString(),
                        pointId = pointId,
                        type = type,
                        description = desc,
                        date = "Hoy"
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

            Text(stringResource(R.string.label_exact_location), fontWeight = FontWeight.Bold, color = primary, fontSize = 18.sp)
            val latStr = String.format(Locale.getDefault(), "%.6f", point.latitude)
            val lngStr = String.format(Locale.getDefault(), "%.6f", point.longitude)
            Text("${stringResource(R.string.label_latitude)}: $latStr\n${stringResource(R.string.label_longitude)}: $lngStr", color = primary.copy(alpha = 0.6f))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = primary.copy(alpha = 0.1f))

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
                    homeViewModel.addComment(pointId, text, 5)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGuest) {
                // Solo usuarios registrados pueden reportar problemas
                Button(
                    onClick = { showReportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = secondary)
                ) {
                    Icon(Icons.Default.Report, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_report_problem))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (index < comment.rating) Color(0xFFFFB300) else Color.LightGray
                    )
                }
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
    var selectedType by remember { mutableStateOf<ReportType?>(null) }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Selector de imagen de la galería del teléfono
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

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
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.report_attach_photo))
                    }
                }
            }
        }
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
            fontWeight = FontWeight.Bold
        )
    }
}
