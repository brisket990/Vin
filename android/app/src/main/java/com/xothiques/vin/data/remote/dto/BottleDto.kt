package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BottleDto(
    val id: String,
    val householdId: String,
    val name: String,
    val producer: String? = null,
    val region: String? = null,
    val appellation: String? = null,
    val grapeVarieties: List<String>? = null,
    val vintage: Int? = null,
    val color: String,
    val quantity: Int,
    val purchasePriceCents: Int? = null,
    val purchaseDate: String? = null,
    val drinkFromYear: Int? = null,
    val drinkUntilYear: Int? = null,
    val locationId: String? = null,
    val labelPhotoUrl: String? = null,
    val status: String,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateBottleRequest(
    val name: String,
    val producer: String? = null,
    val region: String? = null,
    val appellation: String? = null,
    val grapeVarieties: List<String>? = null,
    val vintage: Int? = null,
    val color: String,
    val quantity: Int? = null,
    val purchasePriceCents: Int? = null,
    val purchaseDate: String? = null,
    val drinkFromYear: Int? = null,
    val drinkUntilYear: Int? = null,
    val locationId: String? = null,
    val labelPhotoUrl: String? = null,
    val notes: String? = null,
)

// The API accepts a partial update (PartialType); reuse the same shape.
typealias UpdateBottleRequest = CreateBottleRequest

@Serializable
data class ConsumeBottleRequest(
    val quantity: Int? = null,
    val rating: Int? = null,
    val comment: String? = null,
    val consumedDate: String? = null,
)

@Serializable
data class TastingNoteDto(
    val id: String,
    val bottleId: String,
    val userId: String,
    val rating: Int? = null,
    val comment: String? = null,
    val consumedDate: String,
    val createdAt: String,
)

@Serializable
data class ConsumeBottleResponse(
    val bottle: BottleDto,
    val tastingNote: TastingNoteDto,
)
