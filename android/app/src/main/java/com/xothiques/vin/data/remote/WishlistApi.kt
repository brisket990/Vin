package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.ConvertWishlistItemRequest
import com.xothiques.vin.data.remote.dto.CreateWishlistItemRequest
import com.xothiques.vin.data.remote.dto.UpdateWishlistItemRequest
import com.xothiques.vin.data.remote.dto.WishlistItemDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface WishlistApi {
    @POST("api/vin/wishlist")
    suspend fun create(@Body body: CreateWishlistItemRequest): WishlistItemDto

    @GET("api/vin/wishlist")
    suspend fun findAll(): List<WishlistItemDto>

    @PATCH("api/vin/wishlist/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateWishlistItemRequest,
    ): WishlistItemDto

    @DELETE("api/vin/wishlist/{id}")
    suspend fun remove(@Path("id") id: String)

    @POST("api/vin/wishlist/{id}/convert-to-bottle")
    suspend fun convertToBottle(
        @Path("id") id: String,
        @Body body: ConvertWishlistItemRequest,
    ): BottleDto
}
