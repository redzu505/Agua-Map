package com.aguamap.app.data.repository

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.aguamap.app.data.local.FavoritosManager
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
import com.aguamap.app.util.SyncReportWorker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val sessionManager: SessionManager,
    private val favoritosManager: FavoritosManager,
    private val context: Context
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
        // 1. Iniciamos sesión (usuario + tokens)
        val resultado = remoteDataSource.iniciarSesion(email, contrasenia)
        val sesion = resultado.getOrNull() ?: return resultado

        // 2. Con el token, leemos el ROL del usuario (admin / usuario)
        val rol = sesion.accessToken
            ?.let { token -> remoteDataSource.getRolUsuario(token).getOrDefault("usuario") }
            ?: "usuario"

        // 3. Devolvemos la sesión con el rol incluido
        return Result.success(sesion.copy(usuario = sesion.usuario.copy(rol = rol)))
    }

    /**
     * Actualiza el perfil del usuario logueado en Supabase Auth.
     */
    suspend fun actualizarPerfil(nombre: String, telefono: String): Result<Unit> {
        val token = tokenValido()
            ?: return Result.failure(Exception("No hay sesión activa"))
        return remoteDataSource.actualizarPerfil(token, nombre, telefono)
    }

    /**
     * Cierra la sesión del lado del servidor (best-effort).
     */
    suspend fun cerrarSesionRemota() {
        val token = tokenValido()
        remoteDataSource.cerrarSesion(token)
    }

    // ==========================================
    // SECCIÓN: PUNTOS DE AGUA
    // ==========================================

    suspend fun getWaterPoints(): List<WaterPoint> {
        // 1. Intentamos traer de Supabase
        val token = tokenValido()
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
        val token = tokenValido()
        remoteDataSource.crearPunto(token, point)
    }

    // ==========================================
    // SECCIÓN: COMENTARIOS
    // ==========================================

    suspend fun getComments(pointId: String): List<Comment> {
        val token = tokenValido()
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
        val token = tokenValido()
        remoteDataSource.crearComentario(token, pointId, comment)
    }

    // ==========================================
    // SECCIÓN: VALORACIONES (puntaje 1-5)
    // ==========================================

    /**
     * Registra o modifica la valoración del usuario para un punto.
     * El promedio del punto lo recalcula un trigger en Supabase.
     */
    suspend fun valorarPunto(pointId: String, valor: Int): Result<Unit> {
        val token = tokenValido()
            ?: return Result.failure(Exception("Debes iniciar sesión para valorar"))
        return remoteDataSource.valorarPunto(token, pointId, valor)
    }

    /**
     * Devuelve la valoración que el usuario ya dio a un punto (o null si no votó
     * o si es invitado / no hay sesión).
     */
    suspend fun getMiValoracion(pointId: String): Int? {
        val token = tokenValido() ?: return null
        val userId = obtenerUserIdActual(token) ?: return null
        return remoteDataSource.getMiValoracion(token, userId, pointId).getOrNull()
    }

    // Serializa el refresco del token para que dos peticiones en paralelo no lo renueven a la vez
    private val refreshMutex = Mutex()

    /**
     * Devuelve un access_token VÁLIDO. Si el guardado está vencido, lo renueva con el
     * refresh_token y persiste el nuevo par. Si no se puede renovar, devuelve el que había
     * (que fallará con 401, pero sin romper la app). Es el reemplazo de obtenerAccessToken()
     * para todas las peticiones autenticadas.
     */
    private suspend fun tokenValido(): String? {
        val token = sessionManager.obtenerAccessToken() ?: return null
        if (!tokenExpirado(token)) return token

        return refreshMutex.withLock {
            // Re-chequeo: otro coroutine pudo renovarlo mientras esperábamos el lock
            val actual = sessionManager.obtenerAccessToken() ?: return@withLock null
            if (!tokenExpirado(actual)) return@withLock actual

            val refresh = sessionManager.obtenerRefreshToken() ?: return@withLock actual
            val nuevo = remoteDataSource.refrescarToken(refresh).getOrNull()
            val nuevoAccess = nuevo?.access_token
            if (nuevoAccess != null) {
                sessionManager.actualizarTokens(nuevoAccess, nuevo.refresh_token)
                nuevoAccess
            } else {
                actual // no se pudo renovar: devolvemos el viejo (dará 401, pero no crashea)
            }
        }
    }

    /** Indica si el JWT ya venció (con 60s de margen) leyendo el claim "exp". */
    private fun tokenExpirado(token: String): Boolean {
        return try {
            val partes = token.split(".")
            if (partes.size < 2) return false
            val payload = android.util.Base64.decode(
                partes[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            val json = org.json.JSONObject(String(payload, Charsets.UTF_8))
            val exp = json.optLong("exp", 0L) // epoch en segundos
            if (exp == 0L) return false
            val ahora = System.currentTimeMillis() / 1000
            ahora >= (exp - 60)
        } catch (e: Exception) {
            false // ante la duda, no forzamos refresco
        }
    }

    /**
     * UUID del usuario actual. Usa el guardado en sesión; si está vacío (por ejemplo,
     * una sesión creada antes de que se guardara el id), lo extrae del propio token
     * JWT (claim "sub"). Así el conteo/valoraciones funcionan sin obligar a re-loguear.
     */
    private suspend fun obtenerUserIdActual(token: String): String? {
        val guardado = sessionManager.obtenerUserId()
        if (!guardado.isNullOrBlank()) return guardado
        return extraerSubDelToken(token)
    }

    private fun extraerSubDelToken(token: String): String? {
        return try {
            val partes = token.split(".")
            if (partes.size < 2) return null
            val payload = android.util.Base64.decode(
                partes[1],
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            val json = org.json.JSONObject(String(payload, Charsets.UTF_8))
            json.optString("sub").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Estadísticas del usuario para el perfil: (reportes enviados, comentarios hechos).
     * Devuelve (0, 0) si es invitado, no hay sesión o falla la red.
     */
    suspend fun getEstadisticasUsuario(): Pair<Int, Int> {
        val token = tokenValido() ?: return 0 to 0
        val userId = obtenerUserIdActual(token) ?: return 0 to 0
        val reportes = remoteDataSource.contarReportesUsuario(token, userId)
        val comentarios = remoteDataSource.contarComentariosUsuario(token, userId)
        // DIAGNÓSTICO: en Logcat (filtro AGUAMAP_NET) veremos el UUID usado y los conteos.
        android.util.Log.d(
            "AGUAMAP_NET",
            "📊 Estadisticas → userId=[$userId] reportes=$reportes comentarios=$comentarios"
        )
        return reportes to comentarios
    }

    // ==========================================
    // SECCIÓN: REPORTES
    // ==========================================

    suspend fun getReports(pointId: String): List<WaterPointReport> {
        val token = tokenValido()
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
    suspend fun addReport(report: WaterPointReport, imageBytes: ByteArray? = null): Result<Unit> {
        val token = tokenValido()

        // 1. Si hay foto, la subimos a Storage y obtenemos su URL pública
        var reporteFinal = report
        if (imageBytes != null) {
            val nombreArchivo = "${report.id}.jpg"
            remoteDataSource.subirImagenReporte(token, nombreArchivo, imageBytes)
                .onSuccess { url -> reporteFinal = report.copy(imageUrl = url) }
        }

        // 2. Guardamos en local (offline-first) y replicamos en Supabase
        localDataSource.saveReport(reporteFinal)
        val result = remoteDataSource.crearReporte(token, reporteFinal)

        // 3. Programamos el Worker para asegurar la sincronización de reportes pendientes
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncReportWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncReportsWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        return result
    }

    /**
     * Devuelve los reportes recientes de TODA la comunidad (no de un punto concreto).
     * Se usa en la pantalla de Comunidad. Offline-first: remoto con fallback a local.
     */
    suspend fun getRecentReports(): List<WaterPointReport> {
        val token = tokenValido()
        remoteDataSource.getReportesRecientes(token).onSuccess { remotos ->
            if (remotos.isNotEmpty()) return remotos
        }
        return localDataSource.getAllReports()
    }

    // ==========================================
    // SECCIÓN: FAVORITOS (puntos guardados)
    // ==========================================

    /**
     * Devuelve los IDs de puntos guardados. Offline-first: si hay sesión, los trae
     * de Supabase y refresca la caché local; si no, usa la caché local.
     */
    suspend fun getFavoritos(): Set<String> {
        val token = tokenValido()
        if (token != null) {
            remoteDataSource.getFavoritos(token).onSuccess { remoto ->
                favoritosManager.guardar(remoto)
                return remoto
            }
        }
        return favoritosManager.favoritosActuales()
    }

    /**
     * Marca/desmarca un punto como guardado. Devuelve true si quedó GUARDADO,
     * false si se quitó. Actualiza local al instante y replica en Supabase.
     */
    suspend fun toggleFavorito(pointId: String): Boolean {
        val actuales = favoritosManager.favoritosActuales().toMutableSet()
        val token = tokenValido()

        return if (pointId in actuales) {
            actuales.remove(pointId)
            favoritosManager.guardar(actuales)
            if (token != null) remoteDataSource.quitarFavorito(token, pointId)
            false
        } else {
            actuales.add(pointId)
            favoritosManager.guardar(actuales)
            if (token != null) remoteDataSource.agregarFavorito(token, pointId)
            true
        }
    }

    /** Borra los favoritos locales (al cerrar sesión). */
    suspend fun limpiarFavoritos() {
        favoritosManager.limpiar()
    }

    // ==========================================
    // SECCIÓN: NOTICIAS
    // ==========================================

    suspend fun getNews(): List<CommunityNews> {
        val token = tokenValido()
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
