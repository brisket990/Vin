package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.AiProviderConfigDto
import com.xothiques.vin.data.remote.dto.UpsertAiProviderConfigRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AiProviderApi {
    @GET("api/vin/ai-providers")
    suspend fun list(): List<AiProviderConfigDto>

    @POST("api/vin/ai-providers")
    suspend fun upsert(@Body body: UpsertAiProviderConfigRequest): AiProviderConfigDto

    @DELETE("api/vin/ai-providers/{id}")
    suspend fun remove(@Path("id") id: String)
}
