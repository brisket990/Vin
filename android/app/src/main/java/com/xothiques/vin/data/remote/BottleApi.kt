package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.ConsumeBottleRequest
import com.xothiques.vin.data.remote.dto.ConsumeBottleResponse
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.UpdateBottleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BottleApi {
    @POST("api/vin/bottles")
    suspend fun create(@Body body: CreateBottleRequest): BottleDto

    @GET("api/vin/bottles")
    suspend fun findAll(
        @Query("color") color: String? = null,
        @Query("status") status: String? = null,
        @Query("region") region: String? = null,
        @Query("grape") grape: String? = null,
        @Query("vintageMin") vintageMin: Int? = null,
        @Query("vintageMax") vintageMax: Int? = null,
        @Query("priceMaxCents") priceMaxCents: Int? = null,
        @Query("search") search: String? = null,
    ): List<BottleDto>

    @GET("api/vin/bottles/{id}")
    suspend fun findOne(@Path("id") id: String): BottleDto

    @PATCH("api/vin/bottles/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateBottleRequest): BottleDto

    @DELETE("api/vin/bottles/{id}")
    suspend fun remove(@Path("id") id: String)

    @POST("api/vin/bottles/{id}/consume")
    suspend fun consume(
        @Path("id") id: String,
        @Body body: ConsumeBottleRequest,
    ): ConsumeBottleResponse

    /** Every in-cellar bottle overdue for its "quart de tour" -- see BottleDto.lastTurnedAt. */
    @GET("api/vin/bottles/needing-turn")
    suspend fun findNeedingTurn(): List<BottleDto>

    /** Marks the bottle as turned today, resetting the overdue baseline. */
    @POST("api/vin/bottles/{id}/turn")
    suspend fun turn(@Path("id") id: String): BottleDto
}
