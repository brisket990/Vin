package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CellarLocationDto(
    val id: String,
    val row: Int,
    val column: Int,
    val label: String,
    val bottle: CellarOccupantDto? = null,
)

@Serializable
data class CellarOccupantDto(
    val id: String,
    val color: String,
    val region: String? = null,
)

@Serializable
data class CellarUnitDto(
    val id: String,
    val householdId: String,
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    /** Wine color this unit is dedicated to (e.g. "red"), or null for
     *  "mixed" -- no preference. Drives cross-unit placement suggestions
     *  once a household has more than one unit. */
    val preferredColor: String? = null,
    val createdAt: String,
    val locations: List<CellarLocationDto> = emptyList(),
)

@Serializable
data class CreateCellarUnitRequest(
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    val preferredColor: String? = null,
)

@Serializable
data class UpdateCellarUnitRequest(
    val name: String? = null,
    /** One of the wine colors, or the literal string "none" to clear the
     *  preference back to "mixed". A real `null` here would be silently
     *  dropped by the app's JSON encoder (explicitNulls = false), which is
     *  why this is a sentinel string rather than an actual null. */
    val preferredColor: String? = null,
    /** New grid dimensions -- growing adds empty slots, shrinking removes
     *  the highest rows/columns (refused by the backend if any occupied
     *  location would be deleted). Null leaves that dimension unchanged. */
    val rowCount: Int? = null,
    val columnCount: Int? = null,
)

@Serializable
data class SuggestLocationRequest(
    val color: String,
    val region: String? = null,
    val drinkFromYear: Int? = null,
    val drinkUntilYear: Int? = null,
    val quantity: Int? = null,
)

@Serializable
data class SuggestedLocationDto(
    /** Present when the suggestion came from the cross-unit search
     *  (several casiers configured); absent for the single-unit endpoint. */
    val unitId: String? = null,
    val unitName: String? = null,
    val locationId: String,
    val label: String,
    val row: Int,
    val column: Int,
    val score: Int,
    val runLength: Int = 1,
)

@Serializable
data class NextFreeLocationsRequest(
    val count: Int,
)

@Serializable
data class NextFreeLocationDto(
    val locationId: String,
    val label: String,
    val row: Int,
    val column: Int,
)
