package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatePairingRequest(
    val dishDescription: String,
    val provider: String? = null,
)

/** One of the household's own bottles suggested for the dish, with an
 *  individual match score (1-10) and reasoning specific to that bottle. */
@Serializable
data class PairingCellarSuggestionDto(
    val bottle: BottleDto,
    val score: Double? = null,
    val reasoning: String = "",
)

/** A wine NOT in the household's cellar, described by style/region/grape
 *  so it can be looked for in a shop, with its own match score/reasoning. */
@Serializable
data class PairingShoppingSuggestionDto(
    val name: String,
    val color: String,
    val region: String? = null,
    val grapeVarieties: List<String>? = null,
    val score: Double? = null,
    val reasoning: String = "",
)

@Serializable
data class PairingSuggestionDto(
    val id: String,
    val householdId: String,
    val dishDescription: String,
    val provider: String,
    val rawResponse: String,
    val createdAt: String,
    val cellarSuggestions: List<PairingCellarSuggestionDto> = emptyList(),
    val shoppingSuggestions: List<PairingShoppingSuggestionDto> = emptyList(),
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

/** Like FoodPairingResultDto but a full recipe idea (title + description)
 *  rather than a short dish name -- the same suggestion the "apogée"
 *  (drinking window) push notification includes, available here for an
 *  on-demand look without waiting for the daily alert. Not persisted
 *  server-side. */
@Serializable
data class RecipeSuggestionResultDto(
    val bottleId: String,
    val provider: String,
    val recipeTitle: String,
    val recipeDescription: String,
    val reasoning: String,
    val rawResponse: String,
)
