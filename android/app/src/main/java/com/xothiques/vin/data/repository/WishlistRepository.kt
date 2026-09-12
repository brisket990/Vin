package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.WishlistApi
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.ConvertWishlistItemRequest
import com.xothiques.vin.data.remote.dto.CreateWishlistItemRequest
import com.xothiques.vin.data.remote.dto.WishlistItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepository @Inject constructor(
    private val wishlistApi: WishlistApi,
) {
    suspend fun create(
        name: String,
        region: String?,
        notes: String?,
        targetPriceCents: Int?,
    ): WishlistItemDto =
        wishlistApi.create(CreateWishlistItemRequest(name, region, notes, targetPriceCents))

    suspend fun findAll(): List<WishlistItemDto> = wishlistApi.findAll()

    suspend fun update(
        id: String,
        name: String,
        region: String?,
        notes: String?,
        targetPriceCents: Int?,
    ): WishlistItemDto =
        wishlistApi.update(id, CreateWishlistItemRequest(name, region, notes, targetPriceCents))

    suspend fun remove(id: String) = wishlistApi.remove(id)

    suspend fun convertToBottle(id: String, color: String, locationId: String?): BottleDto =
        wishlistApi.convertToBottle(id, ConvertWishlistItemRequest(color, locationId))
}
