package com.aguamap.app.ui

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
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import com.aguamap.app.ui.components.MapLibreView
import com.aguamap.app.ui.components.WaterPointCard
import com.aguamap.app.ui.theme.AguaMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("Todos", "Fuentes", "Pozos", "Filtrada", "Grifo")
    var selectedFilter by remember { mutableStateOf("Todos") }
    var selectedTab by remember { mutableStateOf("Points") }

    // Paleta Acuática
    val bgLight = Color(0xFFF0F9FF)
    val oceanBlue = Color(0xFF01579B)
    val celeste = Color(0xFF03A9F4)

    // Mock data adaptada a SJL
    val waterPoints = listOf(
        WaterPoint("1", "Fuente Los Postes", "Paradero Los Postes, SJL", 4.8, "50m", "24h", WaterPointStatus.OPERATIVO, WaterPointType.FUENTE),
        WaterPoint("2", "Punto Eco-Filter Zárate", "Av. Gran Chimú 452", 4.5, "800m", "08:00 - 22:00", WaterPointStatus.OPERATIVO, WaterPointType.FILTRADA),
        WaterPoint("3", "Pozo Huiracocha", "Parque Zonal Huiracocha", 4.2, "1.2km", "Cerrado", WaterPointStatus.MANTENIMIENTO, WaterPointType.POZO),
        WaterPoint("4", "Grifo Caja de Agua", "Estación Caja de Agua", 4.9, "1.5km", "24h", WaterPointStatus.OPERATIVO, WaterPointType.GRIFO)
    )

    Scaffold(
        topBar = {
            if (selectedTab == "Points") {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = celeste,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AguaMap",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = oceanBlue
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Filter action */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = oceanBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgLight.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Mapa") },
                    selected = selectedTab == "Map",
                    onClick = { selectedTab = "Map" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = oceanBlue,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = celeste.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = "Points") },
                    label = { Text("Puntos") },
                    selected = selectedTab == "Points",
                    onClick = { selectedTab = "Points" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = oceanBlue,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = celeste.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Community") },
                    label = { Text("Comunidad") },
                    selected = selectedTab == "Community",
                    onClick = { selectedTab = "Community" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = oceanBlue,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = celeste.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Perfil") },
                    selected = selectedTab == "Profile",
                    onClick = { selectedTab = "Profile" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = oceanBlue,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = celeste.copy(alpha = 0.2f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == "Points" || selectedTab == "Map") {
                FloatingActionButton(
                    onClick = { /* Add point */ },
                    containerColor = celeste,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        containerColor = bgLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                "Map" -> {
                    MapLibreView(modifier = Modifier.fillMaxSize())
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
                            placeholder = { Text("Buscar puntos en SJL...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = oceanBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = celeste,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = oceanBlue,
                                unfocusedTextColor = oceanBlue
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
                                        containerColor = Color.White,
                                        labelColor = Color.Gray,
                                        selectedContainerColor = celeste,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Water Points List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(waterPoints) { point ->
                                WaterPointCard(point)
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
                "Community" -> {
                    CommunityScreen()
                }
                "Profile" -> {
                    ProfileScreen()
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Próximamente", color = oceanBlue)
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    AguaMapTheme {
        HomeScreen()
    }
}
