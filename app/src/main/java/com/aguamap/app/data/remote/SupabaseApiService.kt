package com.aguamap.app.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SupabaseApiService {

    // ==========================================
    // AUTENTICACIÓN (Supabase Auth)
    // ==========================================
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: SignUpRequest
    ): Response<AuthResponse>

    //ENDPOINT PARA INICIO DE SESIÓN
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: LoginRequest
    ): Response<AuthResponse>

    // Actualiza la metadata del usuario logueado (nombre, teléfono, etc.)
    @PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: UpdateUserRequest
    ): Response<AuthResponse>

    // Cierra la sesión del lado del servidor (invalida el token)
    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): Response<ResponseBody>

    // Lee el perfil (rol) del usuario logueado. Gracias a RLS, devuelve solo SU fila.
    @GET("rest/v1/perfiles")
    suspend fun getMiPerfil(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "rol"
    ): Response<List<PerfilDto>>

    // ==========================================
    // PUNTOS DE AGUA (tabla: puntos_agua)
    // ==========================================
    @GET("rest/v1/puntos_agua")
    suspend fun getPuntos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*"
    ): Response<List<PuntoDto>>

    @POST("rest/v1/puntos_agua")
    suspend fun crearPunto(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body punto: PuntoDto
    ): Response<ResponseBody>

    // ==========================================
    // COMENTARIOS (tabla: comentarios)
    // ==========================================
    @GET("rest/v1/comentarios")
    suspend fun getComentarios(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("punto_id") puntoId: String,      // formato PostgREST: "eq.<uuid>"
        @Query("select") select: String = "*",
        @Query("order") order: String = "fecha.desc"
    ): Response<List<ComentarioDto>>

    @POST("rest/v1/comentarios")
    suspend fun crearComentario(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body comentario: ComentarioDto
    ): Response<ResponseBody>

    // ==========================================
    // REPORTES (tabla: reportes)
    // ==========================================
    @GET("rest/v1/reportes")
    suspend fun getReportes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("punto_id") puntoId: String,      // formato PostgREST: "eq.<uuid>"
        @Query("select") select: String = "*",
        @Query("order") order: String = "fecha.desc"
    ): Response<List<ReporteDto>>

    // Reportes recientes de TODA la comunidad (para la pantalla de Comunidad)
    @GET("rest/v1/reportes")
    suspend fun getReportesRecientes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 20
    ): Response<List<ReporteDto>>

    @POST("rest/v1/reportes")
    suspend fun crearReporte(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body reporte: ReporteDto
    ): Response<ResponseBody>

    // ==========================================
    // NOTICIAS (tabla: noticias_comunidad)
    // ==========================================
    @GET("rest/v1/noticias_comunidad")
    suspend fun getNoticias(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "fecha.desc"
    ): Response<List<NoticiaDto>>

    // ==========================================
    // FAVORITOS (puntos guardados del usuario; tabla: favoritos)
    // ==========================================
    @GET("rest/v1/favoritos")
    suspend fun getFavoritos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "punto_id"
    ): Response<List<FavoritoDto>>

    @POST("rest/v1/favoritos")
    suspend fun agregarFavorito(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body favorito: FavoritoDto
    ): Response<ResponseBody>

    @DELETE("rest/v1/favoritos")
    suspend fun quitarFavorito(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("punto_id") puntoId: String       // formato PostgREST: "eq.<id>"
    ): Response<ResponseBody>

    // ==========================================
    // STORAGE (subir foto de un reporte al bucket "reportes")
    // ==========================================
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun subirArchivo(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Content-Type") contentType: String,
        @Header("x-upsert") upsert: String = "true",
        @Path("bucket") bucket: String,
        @Path("path") path: String,
        @Body file: RequestBody
    ): Response<ResponseBody>
}
