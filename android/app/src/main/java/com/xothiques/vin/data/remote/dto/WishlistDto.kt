package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WishlistItemDto(
    val id: String,
    val householdId: String,
    val name: String,
    val region: String? = null,
    val notes: String? = null,
    val targetPriceCents: Int? = null,
    val createdAt: String,
)

@Serializable
data class CreateWishlistItemRequest(
    val name: String,
    val region: String? = null,
    val notes: String? = null,
    val targetPriceCents: Int? = null,
)

typealias UpdateWishlistItemRequest = CreateWishlistItemRequest

@Serializable
data class ConvertWishlistItemRequest(
    val color: String,
    val locationId: String? = null,
)
