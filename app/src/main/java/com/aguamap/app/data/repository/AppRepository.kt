package com.aguamap.app.data.repository

import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.domain.UsuarioSesion
import com.aguamap.app.data.remote.AuthResponse     // import para autenticación
import com.aguamap.app.data.remote.RemoteDataSource // import para autenticación
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
class AppRepository(private val localDataSource: LocalDataSource, private val remoteDataSource: RemoteDataSource) {

    // Simulación de datos remotos para la Fase 1
    private val mockRemotePoints = listOf(
        WaterPoint("1", "Fuente Los Postes", "Paradero Los Postes, SJL", 4.8, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.FUENTE, -11.9904, -77.0006),
        WaterPoint("2", "Punto Eco-Filter Zárate", "Av. Gran Chimú 452", 4.5, "---", "08:00 - 22:00", WaterPointStatus.OPERATIVO, WaterPointType.FILTRADA, -12.0225, -77.0012),
        WaterPoint("3", "Pozo Huiracocha", "Parque Zonal Huiracocha", 4.2, "---", "Cerrado", WaterPointStatus.MANTENIMIENTO, WaterPointType.POZO, -11.9961, -76.9958),
        WaterPoint("4", "Grifo Caja de Agua", "Estación Caja de Agua", 4.9, "---", "24h", WaterPointStatus.OPERATIVO, WaterPointType.GRIFO, -12.0272, -77.0142)
    )

    // ==========================================
    // SECCIÓN: AUTENTICACIÓN
    // ==========================================

    suspend fun registrarUsuario(
        email: String,
        contrasenia: String,
        nombre: String,
        dni: String,
        telefono: String,
        usuario: String
    ): Result<AuthResponse> {
        // Conecta directamente con Supabase mediante el RemoteDataSource
        return remoteDataSource.registrarUsuario(
            email = email,
            contrasenia = contrasenia,
            nombre = nombre,
            dni = dni,
            telefono = telefono,
            usuario = usuario
        )
    }


    /*suspend fun iniciarSesion(email: String, contrasenia: String): Result<UsuarioSesion> {
        return try {
            // TODO: Aquí conectarás con Supabase real más adelante.
            // Por ahora, devolvemos un usuario de prueba para que la app funcione:
            val usuarioMock = UsuarioSesion(
                nombre = "Juan Pérez",
                usuario = "juanito123",
                email = email,
                telefono = "987654321",
                dni = "77777777"
            )
            Result.success(usuarioMock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
    suspend fun iniciarSesion(email: String, contrasenia: String): Result<UsuarioSesion> {
        return remoteDataSource.iniciarSesion(email, contrasenia).map { authResponse ->
            // Supabase devuelve la metadata (nombre, dni, etc.) dentro del objeto 'user'
            // NOTA: Ajusta las llamadas (.user o .user_metadata) según cómo esté mapeado tu 'AuthResponse'
            val metadata = authResponse.user?.user_metadata

            UsuarioSesion(
                nombre = metadata?.full_name ?: "",
                usuario = metadata?.username ?: "",
                email = authResponse.user?.email ?: email,
                telefono = metadata?.phone ?: "",
                dni = metadata?.dni ?: ""
            )
        }
    }


    // ==========================================
    // SECCIÓN: PUNTOS DE AGUA Y COMENTARIOS
    // ==========================================

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

    suspend fun addWaterPoint(point: WaterPoint) {
        localDataSource.saveWaterPoint(point)
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
