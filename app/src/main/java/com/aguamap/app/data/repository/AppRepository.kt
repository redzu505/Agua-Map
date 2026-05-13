package com.aguamap.app.data.repository

import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType

/**
 * CAPA DE DATOS - REPOSITORIO
 * El repositorio es la única fuente de verdad. Decide si los datos
 * vienen de la base de datos local o de la API remota.
 */
class AppRepository(private val localDataSource: LocalDataSource) {

    // Simulación de datos remotos para la Fase 1
    private val mockRemotePoints = listOf(
        WaterPoint("1", "Fuente Los Postes", "Paradero Los Postes, SJL", 4.8, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.FUENTE, -11.9904, -77.0006),
        WaterPoint("2", "Punto Eco-Filter Zárate", "Av. Gran Chimú 452", 4.5, "---", "08:00 - 22:00", WaterPointStatus.OPERATIVO, WaterPointType.FILTRADA, -12.0225, -77.0012),
        WaterPoint("3", "Pozo Huiracocha", "Parque Zonal Huiracocha", 4.2, "---", "Cerrado", WaterPointStatus.MANTENIMIENTO, WaterPointType.POZO, -11.9961, -76.9958),
        WaterPoint("4", "Grifo Caja de Agua", "Estación Caja de Agua", 4.9, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.GRIFO, -12.0272, -77.0142)
    )

    suspend fun getWaterPoints(): List<WaterPoint> {
        // Estrategia Offline-First: Primero intentamos obtener caché local
        val localPoints = localDataSource.getWaterPoints()
        
        return if (localPoints.isNotEmpty()) {
            localPoints
        } else {
            // Si no hay local, guardamos los "remotos" en local para la próxima vez
            mockRemotePoints.forEach { localDataSource.saveWaterPoint(it) }
            mockRemotePoints
        }
    }

    suspend fun getComments(pointId: String): List<Comment> {
        return localDataSource.getComments(pointId)
    }

    suspend fun addComment(pointId: String, comment: Comment) {
        localDataSource.saveComment(pointId, comment)
    }

    suspend fun getReports(pointId: String): List<WaterPointReport> {
        return localDataSource.getReports(pointId)
    }

    suspend fun addReport(report: WaterPointReport) {
        localDataSource.saveReport(report)
    }

    suspend fun getNews(): List<CommunityNews> {
        val localNews = localDataSource.getNews()
        return if (localNews.isNotEmpty()) {
            localNews
        } else {
            val mockNews = listOf(
                CommunityNews(title = "Nueva red de fuentes en Zárate", content = "Se han instalado 15 nuevas fuentes de agua potable cerca a la Av. Gran Chimú.", date = "2026-03-28"),
                CommunityNews(title = "Mantenimiento Programado", content = "Sedapal realizará limpieza de reservorios en Campoy este fin de semana.", date = "2026-04-02")
            )
            mockNews.forEach { localDataSource.saveNews(it) }
            mockNews
        }
    }
}
