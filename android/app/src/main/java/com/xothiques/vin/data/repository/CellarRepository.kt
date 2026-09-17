package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.CellarApi
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateCellarUnitRequest
import com.xothiques.vin.data.remote.dto.NextFreeLocationDto
import com.xothiques.vin.data.remote.dto.NextFreeLocationsRequest
import com.xothiques.vin.data.remote.dto.SuggestLocationRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.data.remote.dto.UpdateCellarUnitRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellarRepository @Inject constructor(
    private val cellarApi: CellarApi,
) {
    suspend fun listUnits(): List<CellarUnitDto> = cellarApi.listUnits()

    suspend fun getUnit(id: String): CellarUnitDto = cellarApi.getUnit(id)

    /** [preferredColor] dedicates this unit to a wine color (e.g. "red"),
     *  null for "mixed" -- see CellarUnitDto. */
    suspend fun createUnit(
        name: String,
        rowCount: Int,
        columnCount: Int,
        preferredColor: String? = null,
    ): CellarUnitDto = cellarApi.createUnit(
        CreateCellarUnitRequest(
            name = name,
            rowCount = rowCount,
            columnCount = columnCount,
            preferredColor = preferredColor,
        ),
    )

    /** Refused server-side (BadRequest) while the casier still holds an
     *  in-cellar bottle -- callers should surface that error message as-is. */
    suspend fun deleteUnit(unitId: String) = cellarApi.deleteUnit(unitId)

    /** [preferredColor] is one of the wine colors, "none" to clear it back
     *  to "mixed", or null to leave it unchanged (only renaming).
     *  [rowCount]/[columnCount] resize the grid -- growing adds empty
     *  slots, shrinking is refused server-side (BadRequest) if it would
     *  delete an occupied location; leave null to keep the current size. */
    suspend fun updateUnit(
        unitId: String,
        name: String? = null,
        preferredColor: String? = null,
        rowCount: Int? = null,
        columnCount: Int? = null,
    ): CellarUnitDto = cellarApi.updateUnit(
        unitId,
        UpdateCellarUnitRequest(
            name = name,
            preferredColor = preferredColor,
            rowCount = rowCount,
            columnCount = columnCount,
        ),
    )

    suspend fun suggestLocation(
        unitId: String,
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
        quantity: Int? = null,
    ): List<SuggestedLocationDto> = cellarApi.suggestLocation(
        unitId,
        SuggestLocationRequest(
            color = color,
            region = region,
            drinkFromYear = drinkFromYear,
            drinkUntilYear = drinkUntilYear,
            quantity = quantity,
        ),
    )

    /** Searches every casier of the household at once -- see
     *  CellarApi.suggestLocationAcrossUnits. */
    suspend fun suggestLocationAcrossUnits(
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
        quantity: Int? = null,
    ): List<SuggestedLocationDto> = cellarApi.suggestLocationAcrossUnits(
        SuggestLocationRequest(
            color = color,
            region = region,
            drinkFromYear = drinkFromYear,
            drinkUntilYear = drinkUntilYear,
            quantity = quantity,
        ),
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
