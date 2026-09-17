package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.CreatePairingRequest
import com.xothiques.vin.data.remote.dto.FoodPairingResultDto
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PairingApi {
    @POST("api/vin/pairing")
    suspend fun suggest(@Body body: CreatePairingRequest): PairingSuggestionDto

    @POST("api/vin/pairing/for-bottle/{bottleId}")
    suspend fun suggestForBottle(
        @Path("bottleId") bottleId: String,
        @Query("provider") provider: String? = null,
    ): FoodPairingResultDto

    @GET("api/vin/pairing")
    suspend fun findAll(): List<PairingSuggestionDto>

    @GET("api/vin/pairing/{id}")
    suspend fun findOne(@Path("id") id: String): PairingSuggestionDto
}
