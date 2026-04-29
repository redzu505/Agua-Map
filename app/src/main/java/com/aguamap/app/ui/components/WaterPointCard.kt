package com.aguamap.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType

@Composable
fun WaterPointCard(point: WaterPoint) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon based on type
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = getIconBackgroundColor(point.type).copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getIconForType(point.type),
                                contentDescription = null,
                                tint = getIconBackgroundColor(point.type),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = point.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = point.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (point.status == WaterPointStatus.OPERATIVO) 
                        Color(0xFF00E475).copy(alpha = 0.2f) 
                    else 
                        Color(0xFFFFB4AB).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = point.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (point.status == WaterPointStatus.OPERATIVO) Color(0xFF00E475) else Color(0xFFFFB4AB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoItem(icon = Icons.Default.Star, text = point.rating.toString(), color = Color(0xFF3EDAE3))
                Spacer(modifier = Modifier.width(16.dp))
                InfoItem(icon = Icons.Default.PinDrop, text = point.distance, color = Color.LightGray)
                Spacer(modifier = Modifier.width(16.dp))
                InfoItem(icon = Icons.Default.Schedule, text = point.hours, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getIconForType(type: WaterPointType) = when(type) {
    WaterPointType.FUENTE -> Icons.Default.WaterDrop
    WaterPointType.POZO -> Icons.Default.Opacity
    WaterPointType.FILTRADA -> Icons.Default.Waves
    WaterPointType.GRIFO -> Icons.Default.WaterDrop
}

fun getIconBackgroundColor(type: WaterPointType) = when(type) {
    WaterPointType.FUENTE -> Color(0xFF8B2CF5)
    WaterPointType.POZO -> Color(0xFF3EDAE3)
    WaterPointType.FILTRADA -> Color(0xFF00E475)
    WaterPointType.GRIFO -> Color(0xFFD8B9FF)
}
