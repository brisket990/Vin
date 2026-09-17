package com.xothiques.vin.data.repository

import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.HouseholdApi
import com.xothiques.vin.data.remote.dto.AuthResponseDto
import com.xothiques.vin.data.remote.dto.CreateHouseholdRequest
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.remote.dto.HouseholdSessionDto
import com.xothiques.vin.data.remote.dto.HouseholdSummaryDto
import com.xothiques.vin.data.remote.dto.JoinHouseholdByCodeRequest
import com.xothiques.vin.data.remote.dto.SwitchHouseholdRequest
import com.xothiques.vin.data.remote.dto.UpdateHouseholdRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(
    private val householdApi: HouseholdApi,
    private val sessionManager: SessionManager,
) {
    /** Every foyer the current account belongs to -- drives the switcher UI. */
    suspend fun listMine(): List<HouseholdSummaryDto> = householdApi.listMine()

    suspend fun getMine(): HouseholdDto = householdApi.getMine()

    suspend fun rename(name: String): HouseholdDto =
        householdApi.rename(UpdateHouseholdRequest(name))

    suspend fun regenerateInviteCode(): HouseholdDto = householdApi.regenerateInviteCode()

    /** Creates an additional foyer owned by the current account and switches
     *  straight into it (the returned token becomes the app's active one),
     *  so the app's next requests are scoped to the new foyer. */
    suspend fun create(name: String): HouseholdSessionDto {
        val result = householdApi.create(CreateHouseholdRequest(name))
        persist(result)
        return result
    }

    /** Joins an existing foyer with the current account via its invite code
     *  and switches straight into it -- see [create]. */
    suspend fun joinByCode(inviteCode: String): HouseholdSessionDto {
        val result = householdApi.joinByCode(JoinHouseholdByCodeRequest(inviteCode))
        persist(result)
        return result
    }

    /** Switches the active foyer, replacing the app's stored token so every
     *  subsequent request (AuthInterceptor reads the session fresh each
     *  time) is scoped to the new foyer. */
    suspend fun switch(householdId: String): AuthResponseDto {
        val result = householdApi.switch(SwitchHouseholdRequest(householdId))
        sessionManager.updateActiveHousehold(result.accessToken, result.user.householdId, result.user.role)
        return result
    }

    private suspend fun persist(session: HouseholdSessionDto) {
        sessionManager.updateActiveHousehold(session.accessToken, session.household.id, session.user.role)
    }
}
