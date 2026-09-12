package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.AiProviderApi
import com.xothiques.vin.data.remote.dto.AiProviderConfigDto
import com.xothiques.vin.data.remote.dto.UpsertAiProviderConfigRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderRepository @Inject constructor(
    private val aiProviderApi: AiProviderApi,
) {
    suspend fun list(): List<AiProviderConfigDto> = aiProviderApi.list()

    suspend fun upsert(
        provider: String,
        apiKey: String,
        model: String? = null,
        usage: String? = null,
        isDefault: Boolean? = null,
    ): AiProviderConfigDto = aiProviderApi.upsert(
        UpsertAiProviderConfigRequest(provider, apiKey, model, usage, isDefault),
    )

    suspend fun remove(id: String) = aiProviderApi.remove(id)
}
