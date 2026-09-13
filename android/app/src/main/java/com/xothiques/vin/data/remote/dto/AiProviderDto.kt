package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiProviderConfigDto(
    val id: String,
    // "anthropic" | "openai" | "google" | "mistral" | "openrouter" | "deepseek" | "ollama"
    val provider: String,
    val model: String,
    // Only set for "ollama": the household's self-hosted server address.
    val baseUrl: String? = null,
    val usage: String, // "recognition" | "pairing" | "both"
    val isDefault: Boolean,
    val keyHint: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class UpsertAiProviderConfigRequest(
    val provider: String,
    // Not required for "ollama" -- pass an empty string, the server accepts it.
    val apiKey: String,
    val model: String? = null,
    val baseUrl: String? = null,
    val usage: String? = null,
    val isDefault: Boolean? = null,
)
