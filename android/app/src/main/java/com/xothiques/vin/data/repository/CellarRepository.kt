package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.CellarApi
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateCellarUnitRequest
import com.xothiques.vin.data.remote.dto.SuggestLocationRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellarRepository @Inject constructor(
    private val cellarApi: CellarApi,
) {
    suspend fun listUnits(): List<CellarUnitDto> = cellarApi.listUnits()

    suspend fun getUnit(id: String): CellarUnitDto = cellarApi.getUnit(id)

    suspend fun createUnit(name: String, rowCount: Int, columnCount: Int): CellarUnitDto =
        cellarApi.createUnit(CreateCellarUnitRequest(name, rowCount, columnCount))

    suspend fun suggestLocation(
        unitId: String,
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
    ): List<SuggestedLocationDto> = cellarApi.suggestLocation(
        unitId,
        SuggestLocationRequest(color, region, drinkFromYear, drinkUntilYear),
    )
}
