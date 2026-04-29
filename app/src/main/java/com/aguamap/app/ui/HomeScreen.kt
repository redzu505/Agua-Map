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
import androidx.compose.ui.graphics.Brush
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

    // Mock data based on the design
    val waterPoints = listOf(
        WaterPoint("1", "Fuente Central", "Plaza Mayor, Madrid", 4.8, "250m", "24h", WaterPointStatus.OPERATIVO, WaterPointType.FUENTE),
        WaterPoint("2", "Punto Eco-Filter", "Calle Fuencarral 42", 4.5, "800m", "08:00 - 22:00", WaterPointStatus.OPERATIVO, WaterPointType.FILTRADA),
        WaterPoint("3", "Pozo del Retiro", "Parque del Retiro", 4.2, "1.2km", "Cerrado", WaterPointStatus.MANTENIMIENTO, WaterPointType.POZO),
        WaterPoint("4", "Grifo Público Malasaña", "Plaza de la Luna", 4.9, "1.5km", "24h", WaterPointStatus.OPERATIVO, WaterPointType.GRIFO)
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
                                tint = Color(0xFF8B2CF5),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AguaMap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Filter action */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.LightGray)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0C141F).copy(alpha = 0.8f)
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C141F).copy(alpha = 0.9f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = selectedTab == "Map",
                    onClick = { selectedTab = "Map" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WaterDrop, contentDescription = "Points") },
                    label = { Text("Points") },
                    selected = selectedTab == "Points",
                    onClick = { selectedTab = "Points" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Community") },
                    label = { Text("Community") },
                    selected = selectedTab == "Community",
                    onClick = { selectedTab = "Community" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == "Profile",
                    onClick = { selectedTab = "Profile" }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == "Points" || selectedTab == "Map") {
                FloatingActionButton(
                    onClick = { /* Add point */ },
                    containerColor = Color(0xFFD8B9FF),
                    contentColor = Color(0xFF450086),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        containerColor = Color(0xFF0C141F)
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
                            placeholder = { Text("Buscar puntos de agua...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedBorderColor = Color(0xFF3EDAE3),
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        labelColor = Color.LightGray,
                                        selectedContainerColor = Color(0xFF8B2CF5),
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
                            item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
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
                        Text("Próximamente: $selectedTab", color = Color.White)
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
