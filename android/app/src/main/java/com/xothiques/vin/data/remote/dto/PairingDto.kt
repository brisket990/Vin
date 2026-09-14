package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CreatePairingRequest(
    val dishDescription: String,
    val provider: String? = null,
)

@Serializable
data class PairingSuggestionDto(
    val id: String,
    val householdId: String,
    val dishDescription: String,
    val suggestedBottleIds: List<String>,
    val provider: String,
    val rawResponse: String,
    val structuredFields: JsonObject,
    val createdAt: String,
    val suggestedBottles: List<BottleDto> = emptyList(),
)

/** Reverse of PairingSuggestionDto: given one bottle, dishes that pair well with it. Not persisted server-side. */
@Serializable
data class FoodPairingResultDto(
    val bottleId: String,
    val provider: String,
    val suggestedDishes: List<String>,
    val reasoning: String,
    val rawResponse: String,
)
