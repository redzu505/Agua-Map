package com.aguamap.app.data.repository

import com.aguamap.app.data.local.LocalDataSource
import com.aguamap.app.data.local.SessionManager
import com.aguamap.app.data.remote.AuthResponse     // import para autenticación
import com.aguamap.app.data.remote.RemoteDataSource // import para autenticación
import com.aguamap.app.domain.AuthSession
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.UsuarioSesion
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType

/**
 * CAPA DE DATOS - REPOSITORIO
 * El repositorio es la única fuente de verdad. Decide si los datos
 * vienen de la base de datos local o de la API remota (Supabase).
 *
 * Estrategia OFFLINE-FIRST: siempre se intenta traer/guardar en Supabase, pero si
 * falla (sin internet, tablas aún no creadas, etc.) se usa la caché local / mock,
 * de modo que la app NUNCA se rompe aunque el backend no esté listo.
 */
class AppRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val sessionManager: SessionManager
) {

    // Datos de respaldo (fallback) cuando no hay conexión ni datos remotos todavía
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

    suspend fun iniciarSesion(email: String, contrasenia: String): Result<AuthSession> {
        // Devuelve el usuario junto con los tokens (para persistir la sesión)
        return remoteDataSource.iniciarSesion(email, contrasenia)
    }

    /**
     * Actualiza el perfil del usuario logueado en Supabase Auth.
     */
    suspend fun actualizarPerfil(nombre: String, telefono: String): Result<Unit> {
        val token = sessionManager.obtenerAccessToken()
            ?: return Result.failure(Exception("No hay sesión activa"))
        return remoteDataSource.actualizarPerfil(token, nombre, telefono)
    }

    /**
     * Cierra la sesión del lado del servidor (best-effort).
     */
    suspend fun cerrarSesionRemota() {
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.cerrarSesion(token)
    }

    // ==========================================
    // SECCIÓN: PUNTOS DE AGUA
    // ==========================================

    suspend fun getWaterPoints(): List<WaterPoint> {
        // 1. Intentamos traer de Supabase
        val token = sessionManager.obtenerAccessToken()
        val remoto = remoteDataSource.getPuntos(token)

        remoto.onSuccess { puntosRemotos ->
            if (puntosRemotos.isNotEmpty()) {
                // Refrescamos la caché local con lo último del servidor
                puntosRemotos.forEach { localDataSource.saveWaterPoint(it) }
                return puntosRemotos
            }
        }

        // 2. Fallback offline: caché local; si está vacía, sembramos los mock
        val localPoints = localDataSource.getWaterPoints()
        return if (localPoints.isNotEmpty()) {
            localPoints
        } else {
            mockRemotePoints.forEach { localDataSource.saveWaterPoint(it) }
            mockRemotePoints
        }
    }

    suspend fun addWaterPoint(point: WaterPoint) {
        // Guardamos siempre en local (offline-first)
        localDataSource.saveWaterPoint(point)
        // Y replicamos en Supabase (si falla, queda solo local)
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.crearPunto(token, point)
    }

    // ==========================================
    // SECCIÓN: COMENTARIOS
    // ==========================================

    suspend fun getComments(pointId: String): List<Comment> {
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.getComentarios(token, pointId).onSuccess { remotos ->
            if (remotos.isNotEmpty()) {
                remotos.forEach { localDataSource.saveComment(pointId, it) }
                return remotos
            }
        }
        return localDataSource.getComments(pointId)
    }

    suspend fun addComment(pointId: String, comment: Comment) {
        localDataSource.saveComment(pointId, comment)
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.crearComentario(token, pointId, comment)
    }

    // ==========================================
    // SECCIÓN: REPORTES
    // ==========================================

    suspend fun getReports(pointId: String): List<WaterPointReport> {
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.getReportes(token, pointId).onSuccess { remotos ->
            if (remotos.isNotEmpty()) {
                remotos.forEach { localDataSource.saveReport(it) }
                return remotos
            }
        }
        return localDataSource.getReports(pointId)
    }

    /**
     * Agrega un reporte. Si se adjuntó una foto (imageBytes), primero la sube a
     * Supabase Storage y guarda la URL pública en el reporte.
     */
    suspend fun addReport(report: WaterPointReport, imageBytes: ByteArray? = null) {
        val token = sessionManager.obtenerAccessToken()

        // 1. Si hay foto, la subimos a Storage y obtenemos su URL pública
        var reporteFinal = report
        if (imageBytes != null) {
            val nombreArchivo = "${report.id}.jpg"
            remoteDataSource.subirImagenReporte(token, nombreArchivo, imageBytes)
                .onSuccess { url -> reporteFinal = report.copy(imageUrl = url) }
        }

        // 2. Guardamos en local (offline-first) y replicamos en Supabase
        localDataSource.saveReport(reporteFinal)
        remoteDataSource.crearReporte(token, reporteFinal)
    }

    // ==========================================
    // SECCIÓN: NOTICIAS
    // ==========================================

    suspend fun getNews(): List<CommunityNews> {
        val token = sessionManager.obtenerAccessToken()
        remoteDataSource.getNoticias(token).onSuccess { remotas ->
            if (remotas.isNotEmpty()) return remotas
        }

        // Fallback offline: caché local; si está vacía, sembramos los mock
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
