package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.HouseholdApi
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.remote.dto.UpdateHouseholdRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(
    private val householdApi: HouseholdApi,
) {
    suspend fun getMine(): HouseholdDto = householdApi.getMine()

    suspend fun rename(name: String): HouseholdDto =
        householdApi.rename(UpdateHouseholdRequest(name))

    suspend fun regenerateInviteCode(): HouseholdDto = householdApi.regenerateInviteCode()
}
