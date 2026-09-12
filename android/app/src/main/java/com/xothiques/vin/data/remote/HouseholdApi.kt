package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.remote.dto.UpdateHouseholdRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface HouseholdApi {
    @GET("api/vin/household/me")
    suspend fun getMine(): HouseholdDto

    @PATCH("api/vin/household/me")
    suspend fun rename(@Body body: UpdateHouseholdRequest): HouseholdDto

    @POST("api/vin/household/me/regenerate-invite-code")
    suspend fun regenerateInviteCode(): HouseholdDto
}
