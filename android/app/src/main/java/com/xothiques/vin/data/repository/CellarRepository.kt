package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.CellarApi
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateCellarUnitRequest
import com.xothiques.vin.data.remote.dto.NextFreeLocationDto
import com.xothiques.vin.data.remote.dto.NextFreeLocationsRequest
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
        quantity: Int? = null,
    ): List<SuggestedLocationDto> = cellarApi.suggestLocation(
        unitId,
        SuggestLocationRequest(color, region, drinkFromYear, drinkUntilYear, quantity),
    )

    /**
     * Row-major walk starting at [locationId] (inclusive), returning up to
     * [count] free slots -- used to spread several physical bottles of the
     * same wine across consecutive casiers instead of stacking them all
     * behind one location's quantity count. May return fewer than [count]
     * entries if the unit runs out of free slots.
     */
    suspend fun nextFreeLocations(locationId: String, count: Int): List<NextFreeLocationDto> =
        cellarApi.nextFreeLocations(locationId, NextFreeLocationsRequest(count))
}
