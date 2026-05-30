package com.aguamap.app.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    //URL DE SUPABASE
    private const val BASE_URL = "https://ivivwimqhgkdmdygdwns.supabase.co/"

    val supabaseApi: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Traduce JSON a Kotlin automáticamente
            .build()
            .create(SupabaseApiService::class.java)
    }
}