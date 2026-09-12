package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateCellarUnitRequest
import com.xothiques.vin.data.remote.dto.SuggestLocationRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CellarApi {
    @GET("api/vin/cellar/units")
    suspend fun listUnits(): List<CellarUnitDto>

    @POST("api/vin/cellar/units")
    suspend fun createUnit(@Body body: CreateCellarUnitRequest): CellarUnitDto

    @GET("api/vin/cellar/units/{id}")
    suspend fun getUnit(@Path("id") id: String): CellarUnitDto

    @POST("api/vin/cellar/units/{id}/suggest-location")
    suspend fun suggestLocation(
        @Path("id") unitId: String,
        @Body body: SuggestLocationRequest,
    ): List<SuggestedLocationDto>
}
