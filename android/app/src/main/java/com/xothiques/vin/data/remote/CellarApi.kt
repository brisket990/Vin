package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateCellarUnitRequest
import com.xothiques.vin.data.remote.dto.NextFreeLocationDto
import com.xothiques.vin.data.remote.dto.NextFreeLocationsRequest
import com.xothiques.vin.data.remote.dto.SuggestLocationRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.data.remote.dto.UpdateCellarUnitRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CellarApi {
    @GET("api/vin/cellar/units")
    suspend fun listUnits(): List<CellarUnitDto>

    @POST("api/vin/cellar/units")
    suspend fun createUnit(@Body body: CreateCellarUnitRequest): CellarUnitDto

    @DELETE("api/vin/cellar/units/{id}")
    suspend fun deleteUnit(@Path("id") id: String)

    @GET("api/vin/cellar/units/{id}")
    suspend fun getUnit(@Path("id") id: String): CellarUnitDto

    @PATCH("api/vin/cellar/units/{id}")
    suspend fun updateUnit(
        @Path("id") id: String,
        @Body body: UpdateCellarUnitRequest,
    ): CellarUnitDto

    /** Searches every casier of the household at once -- used as soon as
     *  there's more than one, so the user doesn't have to pick a unit
     *  before asking where a bottle should go. */
    @POST("api/vin/cellar/units/suggest-location")
    suspend fun suggestLocationAcrossUnits(@Body body: SuggestLocationRequest): List<SuggestedLocationDto>

    @POST("api/vin/cellar/units/{id}/suggest-location")
    suspend fun suggestLocation(
        @Path("id") unitId: String,
        @Body body: SuggestLocationRequest,
    ): List<SuggestedLocationDto>

    @POST("api/vin/cellar/locations/{id}/next-free")
    suspend fun nextFreeLocations(
        @Path("id") locationId: String,
        @Body body: NextFreeLocationsRequest,
    ): List<NextFreeLocationDto>
}
