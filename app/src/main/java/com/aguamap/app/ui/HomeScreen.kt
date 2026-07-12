package com.aguamap.app.ui

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aguamap.app.R
import com.aguamap.app.data.local.UserPreferencesRepository
import com.aguamap.app.domain.UserPreferences
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import com.aguamap.app.ui.components.MapLibreView
import com.aguamap.app.ui.components.WaterPointCard
import com.aguamap.app.ui.theme.AguaMapTheme
import com.aguamap.app.util.LocationService
import com.aguamap.app.util.LocationUtils
import com.aguamap.app.viewmodel.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    isGuest: Boolean,
    isAdmin: Boolean = false,
    userName: String,
    userEmail: String,
    userPhone: String = "",
    onSaveProfile: (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddPoint: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            scope.launch {
                userLocation = locationService.getCurrentLocation(context)
            }
        }
    }

    val waterPointsState by homeViewModel.waterPoints.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val routeDestination by homeViewModel.routeDestination.collectAsState()

    // Preferencias del usuario (sector y radio de búsqueda) para filtrar los puntos
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val userPreferences by prefsRepo.userPreferencesFlow.collectAsState(initial = UserPreferences())

    // Puntos guardados (favoritos) del usuario
    val favoritos by homeViewModel.favoritos.collectAsState()

    // Estadísticas reales de actividad (reportes, comentarios) para la pestaña Perfil
    val estadisticas by homeViewModel.estadisticas.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadWaterPoints()
        if (LocationUtils.hasLocationPermissions(context)) {
            userLocation = locationService.getCurrentLocation(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("Todos", "Fuentes", "Pozos", "Filtrada", "Grifo")
    var selectedFilter by remember { mutableStateOf("Todos") }
    var selectedTab by remember { mutableStateOf("Points") }

    // Al entrar a la pestaña Perfil (usuario registrado), cargamos sus contadores reales
    LaunchedEffect(selectedTab, isGuest) {
        if (selectedTab == "Profile" && !isGuest) {
            homeViewModel.cargarEstadisticasUsuario()
        }
    }

    // Si se asigna un destino de ruta desde fuera, cambiamos a la pestaña de Mapa
    LaunchedEffect(routeDestination) {
        if (routeDestination != null) {
            selectedTab = "Map"
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val background = MaterialTheme.colorScheme.background

    val waterPoints = remember(userLocation, waterPointsState, searchQuery, selectedFilter, userPreferences) {
        waterPointsState
            .map { point ->
                val distKm = userLocation?.let { loc ->
                    LocationUtils.calculateDistance(
                        loc.latitude, loc.longitude,
                        point.latitude, point.longitude
                    )
                }
                point to distKm
            }
            .filter { (point, distKm) ->
                val matchesSearch = point.name.contains(searchQuery, ignoreCase = true) ||
                                   point.address.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "Todos" -> true
                    "Fuentes" -> point.type == WaterPointType.FUENTE
                    "Pozos" -> point.type == WaterPointType.POZO
                    "Filtrada" -> point.type == WaterPointType.FILTRADA
                    "Grifo" -> point.type == WaterPointType.GRIFO
                    else -> false
                }

                // PREFERENCIA: Sector (filtra por el sector elegido en el Perfil)
                val matchesSector = puntoEnSector(point, userPreferences.selectedSector)

                // PREFERENCIA: Radio de búsqueda (solo si ya tenemos ubicación GPS)
                val matchesRadius = userLocation == null || distKm == null ||
                        distKm <= userPreferences.searchRadius

                matchesSearch && matchesFilter && matchesSector && matchesRadius
            }
            .sortedBy { (_, distKm) -> distKm ?: Double.MAX_VALUE }
            .map { (point, distKm) ->
                point.copy(distance = distKm?.let { LocationUtils.formatDistance(it) } ?: "---")
            }
    }

    Scaffold(
        topBar = {
            if (selectedTab == "Points") {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (isGuest) "AguaMap SJL" else "¡Hola, $userName!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = primary
                            )
                            if (!isGuest) {
                                Text(
                                    "Guardián del Agua",
                                    fontSize = 12.sp,
                                    color = secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Filter action */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = background.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text(stringResource(R.string.nav_map)) },
                    selected = selectedTab == "Map",
                    onClick = { selectedTab = "Map" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = secondary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = "Points") },
                    label = { Text(stringResource(R.string.nav_points)) },
                    selected = selectedTab == "Points",
                    onClick = { selectedTab = "Points" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = secondary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Community") },
                    label = { Text(stringResource(R.string.nav_community)) },
                    selected = selectedTab == "Community",
                    onClick = { selectedTab = "Community" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = secondary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(R.string.nav_profile)) },
                    selected = selectedTab == "Profile",
                    onClick = { selectedTab = "Profile" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        indicatorColor = secondary.copy(alpha = 0.15f)
                    )
                )
            }
        },
        floatingActionButton = {
            // Solo el ADMINISTRADOR puede crear puntos de agua.
            // Invitados y usuarios normales no ven el botón "+".
            if (isAdmin && (selectedTab == "Points" || selectedTab == "Map")) {
                FloatingActionButton(
                    onClick = onNavigateToAddPoint,
                    containerColor = secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        containerColor = background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                "Map" -> {
                    MapLibreView(
                        modifier = Modifier.fillMaxSize(),
                        waterPoints = waterPoints,
                        userLocation = userLocation?.let { LatLng(it.latitude, it.longitude) },
                        routeDestination = routeDestination,
                        onMarkerClick = onNavigateToDetail
                    )
                }
                "Points" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = secondary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = primary,
                                unfocusedTextColor = primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filters
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filters) { filter ->
                                val isSelected = filter == selectedFilter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        selectedContainerColor = secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = secondary)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Water Points List
                        if (waterPoints.isEmpty() && !isLoading) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("No se encontraron puntos de agua", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(waterPoints) { point ->
                                    WaterPointCard(
                                        point = point,
                                        onClick = { onNavigateToDetail(point.id) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
                "Community" -> {
                    CommunityScreen(
                        homeViewModel = homeViewModel,
                        onBack = { selectedTab = "Points" },
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                "Profile" -> {
                    ProfileScreen(
                        isGuest = isGuest,
                        userName = userName,
                        userEmail = userEmail,
                        userPhone = userPhone,
                        savedPoints = waterPointsState.filter { it.id in favoritos },
                        puntosReportados = estadisticas.first,
                        comentariosRealizados = estadisticas.second,
                        onBack = { selectedTab = "Points" },
                        onLoginClick = onNavigateToLogin,
                        onLogoutClick = onLogoutClick,
                        onSaveProfile = onSaveProfile,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Próximamente", color = primary)
                    }
                }
            }
        }
    }
}

/**
 * ¿El punto pertenece al sector elegido? Si es "Todos", siempre true.
 * Como los puntos no tienen un campo "sector" propio, lo deducimos buscando el
 * nombre del sector dentro del nombre o la dirección del punto (sin tildes ni mayúsculas).
 */
private fun puntoEnSector(point: WaterPoint, sector: String): Boolean {
    if (sector == "Todos") return true
    val s = normalizarTexto(sector)
    return normalizarTexto(point.name).contains(s) || normalizarTexto(point.address).contains(s)
}

private fun normalizarTexto(texto: String): String =
    texto.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    // Para el preview, necesitaríamos un mock de ViewModel o Repository
    // Por simplicidad, dejamos el preview deshabilitado o mostramos un placeholder
    Text("Preview de HomeScreen")
}
