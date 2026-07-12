package com.aguamap.app.data.remote

import android.util.Log
import com.aguamap.app.BuildConfig
import com.aguamap.app.domain.AuthSession
import com.aguamap.app.domain.Comment
import com.aguamap.app.domain.CommunityNews
import com.aguamap.app.domain.ReportType
import com.aguamap.app.domain.UsuarioSesion
import com.aguamap.app.domain.WaterPoint
import com.aguamap.app.domain.WaterPointReport
import com.aguamap.app.domain.WaterPointStatus
import com.aguamap.app.domain.WaterPointType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.Exception

/**
 * CAPA DE DATOS - REMOTA
 * Gestiona toda la comunicación con Supabase (Auth, REST/PostgREST y Storage).
 *
 * DIAGNÓSTICO: cada método registra en Logcat (tag "AGUAMAP_NET") si la llamada
 * salió bien (✅) o falló (❌), indicando el endpoint, el código HTTP y el mensaje
 * EXACTO que devolvió Supabase. Así sabes siempre qué endpoint está fallando y por qué.
 *
 *   👉 En Android Studio: pestaña Logcat, filtra por  AGUAMAP_NET
 */
class RemoteDataSource(private val apiService: SupabaseApiService) {

    companion object {
        private const val TAG = "AGUAMAP_NET"
    }

    // La apikey pública (anon) se inyecta desde BuildConfig / local.properties
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY
    private val anonBearer = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

    // Bucket de Storage donde se guardan las fotos de los reportes
    private val bucketReportes = "reportes"

    /**
     * Construye el header Authorization. Si hay token de usuario lo usa (peticiones
     * autenticadas para RLS); si no, cae al token anónimo (lecturas públicas).
     */
    private fun bearer(token: String?): String =
        if (!token.isNullOrBlank()) "Bearer $token" else anonBearer

    // --- Helpers de logging ---
    private fun logFallo(endpoint: String, code: Int, error: String) {
        Log.e(TAG, "❌ [$endpoint] HTTP $code → $error")
    }

    private fun logExcepcion(endpoint: String, e: Exception) {
        Log.e(TAG, "❌ [$endpoint] Sin respuesta (¿sin internet?): ${e.message}", e)
    }

    private fun logOk(endpoint: String, detalle: String = "") {
        Log.d(TAG, "✅ [$endpoint] OK $detalle")
    }

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
        val endpoint = "POST auth/v1/signup"
        return try {
            // 1. Empaquetamos los datos del formulario en el formato que Supabase entiende
            val metadata = UserMetadata(full_name = nombre, dni = dni, phone = telefono, username = usuario)
            val request = SignUpRequest(email = email, password = contrasenia, data = metadata)

            // 2. Hacemos la petición web a través de Retrofit
            val response = apiService.signUp(apiKey, anonBearer, request)

            // 3. Evaluamos si el servidor nos aceptó
            if (response.isSuccessful && response.body() != null) {
                logOk(endpoint, "usuario=$email")
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception("Error en Supabase: $errorMsg"))
            }
        } catch (e: Exception) {
            // Captura errores como: "El celular no tiene internet" o "Servidor caído"
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, contrasenia: String): Result<AuthSession> {
        val endpoint = "POST auth/v1/token"
        return try {
            // 1. Armamos la petición de login
            val request = LoginRequest(email = email, password = contrasenia)

            // 2. Ejecutamos el POST contra Supabase
            val response = apiService.signIn(apiKey, anonBearer, request)

            // 3. Evaluamos el resultado del servidor
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val metadata = body.user?.user_metadata
                val sesion = AuthSession(
                    usuario = UsuarioSesion(
                        id = body.user?.id ?: "",
                        nombre = metadata?.full_name ?: "",
                        usuario = metadata?.username ?: "",
                        email = body.user?.email ?: email,
                        telefono = metadata?.phone ?: "",
                        dni = metadata?.dni ?: ""
                    ),
                    accessToken = body.access_token,
                    refreshToken = body.refresh_token
                )
                logOk(endpoint, "usuario=$email")
                Result.success(sesion)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception("Error en Supabase: $errorMsg"))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e) // Captura fallas de red/conectividad
        }
    }

    /**
     * Actualiza la metadata del usuario logueado (nombre y teléfono) en Supabase Auth.
     */
    suspend fun actualizarPerfil(
        token: String,
        nombre: String,
        telefono: String
    ): Result<Unit> {
        val endpoint = "PUT auth/v1/user"
        return try {
            val request = UpdateUserRequest(
                data = UserMetadata(full_name = nombre, phone = telefono)
            )
            val response = apiService.updateUser(apiKey, "Bearer $token", request)
            if (response.isSuccessful) {
                logOk(endpoint)
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception("Error en Supabase: $errorMsg"))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    /**
     * Lee el rol del usuario logueado desde la tabla `perfiles`. Si la tabla no existe
     * todavía o falla, devuelve "usuario" (fail-closed: nunca da admin por error).
     */
    suspend fun getRolUsuario(token: String): Result<String> {
        val endpoint = "GET rest/v1/perfiles (rol)"
        return try {
            val response = apiService.getMiPerfil(apiKey, "Bearer $token")
            if (response.isSuccessful) {
                val rol = response.body()?.firstOrNull()?.rol ?: "usuario"
                logOk(endpoint, "rol=$rol")
                Result.success(rol)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.success("usuario") // ante error, tratamos como usuario normal
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.success("usuario")
        }
    }

    /**
     * Cierra la sesión del lado del servidor. No es crítico si falla
     * (igual borraremos la sesión local), por eso nunca lanza.
     */
    suspend fun cerrarSesion(token: String?) {
        if (token.isNullOrBlank()) return
        val endpoint = "POST auth/v1/logout"
        try {
            apiService.signOut(apiKey, "Bearer $token")
            logOk(endpoint)
        } catch (e: Exception) {
            // Silenciamos: el logout local es lo que realmente importa
            logExcepcion(endpoint, e)
        }
    }

    // ==========================================
    // SECCIÓN: PUNTOS DE AGUA
    // ==========================================

    suspend fun getPuntos(token: String?): Result<List<WaterPoint>> {
        val endpoint = "GET rest/v1/puntos_agua"
        return try {
            val response = apiService.getPuntos(apiKey, bearer(token))
            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!.map { it.toWaterPoint() }
                logOk(endpoint, "${lista.size} puntos")
                Result.success(lista)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun crearPunto(token: String?, point: WaterPoint): Result<Unit> {
        val endpoint = "POST rest/v1/puntos_agua"
        return try {
            val response = apiService.crearPunto(apiKey, bearer(token), punto = point.toDto())
            if (response.isSuccessful) {
                logOk(endpoint, "id=${point.id}")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: COMENTARIOS
    // ==========================================

    suspend fun getComentarios(token: String?, pointId: String): Result<List<Comment>> {
        val endpoint = "GET rest/v1/comentarios"
        return try {
            val response = apiService.getComentarios(apiKey, bearer(token), puntoId = "eq.$pointId")
            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!.map { it.toComment() }
                logOk(endpoint, "${lista.size} comentarios")
                Result.success(lista)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun crearComentario(token: String?, pointId: String, comment: Comment): Result<Unit> {
        val endpoint = "POST rest/v1/comentarios"
        return try {
            val dto = ComentarioDto(
                id = comment.id,
                puntoId = pointId,
                autor = comment.author,
                contenido = comment.content,
                calificacion = comment.rating,
                fecha = comment.date
            )
            val response = apiService.crearComentario(apiKey, bearer(token), comentario = dto)
            if (response.isSuccessful) {
                logOk(endpoint, "punto=$pointId")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: VALORACIONES (puntaje 1-5)
    // ==========================================

    /**
     * Crea o modifica MI valoración de un punto (upsert). El promedio del punto
     * lo recalcula automáticamente un trigger en la base de datos.
     */
    suspend fun valorarPunto(token: String, pointId: String, valor: Int): Result<Unit> {
        val endpoint = "POST rest/v1/valoraciones"
        return try {
            val dto = ValoracionDto(puntoId = pointId, valor = valor)
            val response = apiService.valorarPunto(apiKey, "Bearer $token", valoracion = dto)
            if (response.isSuccessful) {
                logOk(endpoint, "punto=$pointId valor=$valor")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    /**
     * Lee MI valoración actual de un punto (o null si aún no he votado / falla).
     */
    suspend fun getMiValoracion(token: String, userId: String, pointId: String): Result<Int?> {
        val endpoint = "GET rest/v1/valoraciones (mi voto)"
        return try {
            val response = apiService.getMiValoracion(
                apiKey, "Bearer $token",
                puntoId = "eq.$pointId",
                userId = "eq.$userId"
            )
            if (response.isSuccessful) {
                val valor = response.body()?.firstOrNull()?.valor
                logOk(endpoint, "punto=$pointId valor=$valor")
                Result.success(valor)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.success(null)
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.success(null)
        }
    }

    // ==========================================
    // SECCIÓN: ESTADÍSTICAS DEL USUARIO (perfil)
    // ==========================================

    /** Cuenta cuántos reportes ha enviado el usuario. Devuelve 0 si falla. */
    suspend fun contarReportesUsuario(token: String, userId: String): Int {
        val endpoint = "GET rest/v1/reportes (conteo usuario)"
        return try {
            val response = apiService.getReportesDeUsuario(apiKey, "Bearer $token", userId = "eq.$userId")
            if (response.isSuccessful) {
                val total = response.body()?.size ?: 0
                logOk(endpoint, "total=$total")
                total
            } else {
                logFallo(endpoint, response.code(), response.errorBody()?.string() ?: "")
                0
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            0
        }
    }

    /** Cuenta cuántos comentarios ha hecho el usuario. Devuelve 0 si falla. */
    suspend fun contarComentariosUsuario(token: String, userId: String): Int {
        val endpoint = "GET rest/v1/comentarios (conteo usuario)"
        return try {
            val response = apiService.getComentariosDeUsuario(apiKey, "Bearer $token", userId = "eq.$userId")
            if (response.isSuccessful) {
                val total = response.body()?.size ?: 0
                logOk(endpoint, "total=$total")
                total
            } else {
                logFallo(endpoint, response.code(), response.errorBody()?.string() ?: "")
                0
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            0
        }
    }

    // ==========================================
    // SECCIÓN: REPORTES
    // ==========================================

    suspend fun getReportes(token: String?, pointId: String): Result<List<WaterPointReport>> {
        val endpoint = "GET rest/v1/reportes"
        return try {
            val response = apiService.getReportes(apiKey, bearer(token), puntoId = "eq.$pointId")
            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!.map { it.toReport() }
                logOk(endpoint, "${lista.size} reportes")
                Result.success(lista)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun getReportesRecientes(token: String?): Result<List<WaterPointReport>> {
        val endpoint = "GET rest/v1/reportes (recientes)"
        return try {
            val response = apiService.getReportesRecientes(apiKey, bearer(token))
            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!.map { it.toReport() }
                logOk(endpoint, "${lista.size} reportes")
                Result.success(lista)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun crearReporte(token: String?, report: WaterPointReport): Result<Unit> {
        val endpoint = "POST rest/v1/reportes"
        return try {
            val dto = ReporteDto(
                id = report.id,
                puntoId = report.pointId,
                tipo = report.type.name,
                descripcion = report.description,
                imagenUrl = report.imageUrl,
                fecha = report.date
            )
            val response = apiService.crearReporte(apiKey, bearer(token), reporte = dto)
            if (response.isSuccessful) {
                logOk(endpoint, "punto=${report.pointId}")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: NOTICIAS
    // ==========================================

    suspend fun getNoticias(token: String?): Result<List<CommunityNews>> {
        val endpoint = "GET rest/v1/noticias_comunidad"
        return try {
            val response = apiService.getNoticias(apiKey, bearer(token))
            if (response.isSuccessful && response.body() != null) {
                val lista = response.body()!!.map { it.toNews() }
                logOk(endpoint, "${lista.size} noticias")
                Result.success(lista)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: FAVORITOS (puntos guardados)
    // ==========================================

    suspend fun getFavoritos(token: String): Result<Set<String>> {
        val endpoint = "GET rest/v1/favoritos"
        return try {
            val response = apiService.getFavoritos(apiKey, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val ids = response.body()!!.mapNotNull { it.puntoId }.toSet()
                logOk(endpoint, "${ids.size} guardados")
                Result.success(ids)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun agregarFavorito(token: String, pointId: String): Result<Unit> {
        val endpoint = "POST rest/v1/favoritos"
        return try {
            val response = apiService.agregarFavorito(apiKey, "Bearer $token", favorito = FavoritoDto(puntoId = pointId))
            if (response.isSuccessful) {
                logOk(endpoint, "punto=$pointId")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    suspend fun quitarFavorito(token: String, pointId: String): Result<Unit> {
        val endpoint = "DELETE rest/v1/favoritos"
        return try {
            val response = apiService.quitarFavorito(apiKey, "Bearer $token", puntoId = "eq.$pointId")
            if (response.isSuccessful) {
                logOk(endpoint, "punto=$pointId")
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // SECCIÓN: STORAGE (fotos de reportes)
    // ==========================================

    /**
     * Sube una imagen al bucket "reportes" y devuelve su URL pública.
     * Requiere token de usuario autenticado (las escrituras a Storage usan RLS).
     */
    suspend fun subirImagenReporte(
        token: String?,
        fileName: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> {
        val endpoint = "POST storage/v1/object/$bucketReportes"
        return try {
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val response = apiService.subirArchivo(
                apiKey = apiKey,
                bearerToken = bearer(token),
                contentType = mimeType,
                bucket = bucketReportes,
                path = fileName,
                file = body
            )
            if (response.isSuccessful) {
                // URL pública estándar de Supabase Storage
                val publicUrl = "${BuildConfig.SUPABASE_URL}storage/v1/object/public/$bucketReportes/$fileName"
                logOk(endpoint, "archivo=$fileName")
                Result.success(publicUrl)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error subiendo imagen"
                logFallo(endpoint, response.code(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logExcepcion(endpoint, e)
            Result.failure(e)
        }
    }

    // ==========================================
    // MAPEADORES DTO <-> DOMINIO
    // ==========================================

    private fun PuntoDto.toWaterPoint(): WaterPoint = WaterPoint(
        id = id ?: "",
        name = nombre ?: "",
        address = direccion ?: "",
        rating = calificacion ?: 0.0,
        distance = "---", // la distancia real se calcula en la UI con el GPS
        hours = horario ?: "",
        status = parseStatus(estado),
        type = parseType(tipo),
        latitude = latitud ?: 0.0,
        longitude = longitud ?: 0.0,
        imageUrl = imagenUrl
    )

    private fun WaterPoint.toDto(): PuntoDto = PuntoDto(
        id = id,
        nombre = name,
        direccion = address,
        calificacion = rating,
        horario = hours,
        estado = status.name,
        tipo = type.name,
        latitud = latitude,
        longitud = longitude,
        imagenUrl = imageUrl
    )

    private fun ComentarioDto.toComment(): Comment = Comment(
        id = id ?: "",
        author = autor ?: "Anónimo",
        content = contenido ?: "",
        rating = calificacion ?: 5,
        date = fecha ?: ""
    )

    private fun ReporteDto.toReport(): WaterPointReport = WaterPointReport(
        id = id ?: "",
        pointId = puntoId ?: "",
        type = parseReportType(tipo),
        description = descripcion ?: "",
        date = fecha ?: "",
        imageUrl = imagenUrl
    )

    private fun NoticiaDto.toNews(): CommunityNews = CommunityNews(
        id = id ?: 0,
        title = titulo ?: "",
        content = contenido ?: "",
        date = fecha ?: ""
    )

    private fun parseStatus(value: String?): WaterPointStatus =
        try { WaterPointStatus.valueOf(value ?: "") } catch (e: Exception) { WaterPointStatus.OPERATIVO }

    private fun parseType(value: String?): WaterPointType =
        try { WaterPointType.valueOf(value ?: "") } catch (e: Exception) { WaterPointType.FUENTE }

    private fun parseReportType(value: String?): ReportType =
        try { ReportType.valueOf(value ?: "") } catch (e: Exception) { ReportType.OTRO }
}
