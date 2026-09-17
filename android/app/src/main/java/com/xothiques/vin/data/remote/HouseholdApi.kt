package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.AuthResponseDto
import com.xothiques.vin.data.remote.dto.CreateHouseholdRequest
import com.xothiques.vin.data.remote.dto.DeleteHouseholdResponseDto
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.remote.dto.HouseholdSessionDto
import com.xothiques.vin.data.remote.dto.HouseholdSummaryDto
import com.xothiques.vin.data.remote.dto.JoinHouseholdByCodeRequest
import com.xothiques.vin.data.remote.dto.SwitchHouseholdRequest
import com.xothiques.vin.data.remote.dto.UpdateHouseholdRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface HouseholdApi {
    /** Every foyer the current account belongs to -- drives the switcher UI. */
    @GET("api/vin/household/mine")
    suspend fun listMine(): List<HouseholdSummaryDto>

    @GET("api/vin/household/me")
    suspend fun getMine(): HouseholdDto

    @PATCH("api/vin/household/me")
    suspend fun rename(@Body body: UpdateHouseholdRequest): HouseholdDto

    @POST("api/vin/household/me/regenerate-invite-code")
    suspend fun regenerateInviteCode(): HouseholdDto

    /** Creates an additional foyer owned by the current account and returns
     *  a token already scoped to it. */
    @POST("api/vin/household")
    suspend fun create(@Body body: CreateHouseholdRequest): HouseholdSessionDto

    /** Joins an existing foyer with the current account via its invite code. */
    @POST("api/vin/household/join")
    suspend fun joinByCode(@Body body: JoinHouseholdByCodeRequest): HouseholdSessionDto

    /** Switches the active foyer by issuing a token scoped to it. */
    @POST("api/vin/household/switch")
    suspend fun switch(@Body body: SwitchHouseholdRequest): AuthResponseDto

    /** Permanently deletes a foyer -- owner only, only while solo in it, and
     *  never the caller's last one. See HouseholdService.deleteHousehold on
     *  the backend for the exact rules and refusal messages. */
    @DELETE("api/vin/household/{id}")
    suspend fun delete(@Path("id") id: String): DeleteHouseholdResponseDto
}
