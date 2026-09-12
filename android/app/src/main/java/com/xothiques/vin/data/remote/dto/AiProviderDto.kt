package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiProviderConfigDto(
    val id: String,
    val provider: String, // "anthropic" | "openai" | "google"
    val model: String,
    val usage: String, // "recognition" | "pairing" | "both"
    val isDefault: Boolean,
    val keyHint: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class UpsertAiProviderConfigRequest(
    val provider: String,
    val apiKey: String,
    val model: String? = null,
    val usage: String? = null,
    val isDefault: Boolean? = null,
)
