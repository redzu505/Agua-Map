package com.aguamap.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseApiService {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: SignUpRequest
    ): Response<AuthResponse>
}