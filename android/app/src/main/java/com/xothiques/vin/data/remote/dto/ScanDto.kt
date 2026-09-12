package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ScanResultDto(
    val id: String,
    val householdId: String,
    val bottleId: String? = null,
    val provider: String,
    val photoUrl: String,
    val rawResponse: String,
    val structuredFields: JsonObject,
    val createdAt: String,
)

@Serializable
data class LinkScanBottleRequest(val bottleId: String)
