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
    val createdAt: String,
    val locations: List<CellarLocationDto> = emptyList(),
)

@Serializable
data class CreateCellarUnitRequest(
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
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
