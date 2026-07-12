package com.aguamap.app.data.remote

import com.aguamap.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // URL de Supabase (se inyecta desde BuildConfig / local.properties)
    private val BASE_URL = BuildConfig.SUPABASE_URL

    /**
     * Interceptor que escribe en Logcat CADA petición a Supabase:
     *  --> GET https://...supabase.co/rest/v1/puntos_agua
     *  <-- 200 OK (123ms)   ó   <-- 404 Not Found (88ms)
     *
     * Usamos nivel BASIC (método + URL + código + tiempo). NO mostramos el cuerpo,
     * así NO se filtran las contraseñas en el log. La razón del error de Supabase la
     * registra el RemoteDataSource con el tag AGUAMAP_NET.
     *
     * Solo se activa en compilaciones de depuración (BuildConfig.DEBUG).
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val supabaseApi: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Traduce JSON a Kotlin automáticamente
            .build()
            .create(SupabaseApiService::class.java)
    }
}
