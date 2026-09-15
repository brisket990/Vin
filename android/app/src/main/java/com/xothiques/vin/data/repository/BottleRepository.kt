package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.BottleApi
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.ConsumeBottleRequest
import com.xothiques.vin.data.remote.dto.ConsumeBottleResponse
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import javax.inject.Inject
import javax.inject.Singleton

data class BottleFilters(
    val color: String? = null,
    val status: String? = null,
    val region: String? = null,
    val grape: String? = null,
    val vintageMin: Int? = null,
    val vintageMax: Int? = null,
    val priceMaxCents: Int? = null,
    val search: String? = null,
)

@Singleton
class BottleRepository @Inject constructor(
    private val bottleApi: BottleApi,
    private val cellarRepository: CellarRepository,
) {
    suspend fun create(request: CreateBottleRequest): BottleDto = bottleApi.create(request)

    /**
     * Creates a bottle, splitting it across consecutive free casiers when
     * quantity > 1 and a starting location was chosen -- one physical
     * bottle per slot (e.g. picking R1-C3 for 10 bottles fills roughly
     * R1-C3 through R1-C12, skipping any already-occupied slot), instead of
     * stacking all 10 behind a single location's quantity count. Returns
     * every row created; the first is the "primary" one (e.g. to link a
     * scan result to). If the unit runs out of free slots, any leftover
     * quantity is created as a single unassigned row (shows up under
     * "Bouteilles sans emplacement" in the cellar grid).
     */
    suspend fun createExpandingLocations(request: CreateBottleRequest): List<BottleDto> {
        val quantity = request.quantity ?: 1
        val locationId = request.locationId
        if (quantity <= 1 || locationId == null) {
            return listOf(bottleApi.create(request))
        }

        val freeLocations = cellarRepository.nextFreeLocations(locationId, quantity)
        val created = mutableListOf<BottleDto>()
        for (location in freeLocations) {
            created += bottleApi.create(request.copy(quantity = 1, locationId = location.locationId))
        }
        val leftover = quantity - freeLocations.size
        if (leftover > 0) {
            created += bottleApi.create(request.copy(quantity = leftover, locationId = null))
        }
        return created
    }

    suspend fun findAll(filters: BottleFilters = BottleFilters()): List<BottleDto> =
        bottleApi.findAll(
            color = filters.color,
            status = filters.status,
            region = filters.region,
            grape = filters.grape,
            vintageMin = filters.vintageMin,
            vintageMax = filters.vintageMax,
            priceMaxCents = filters.priceMaxCents,
            search = filters.search,
        )

    suspend fun findOne(id: String): BottleDto = bottleApi.findOne(id)

    suspend fun update(id: String, request: CreateBottleRequest): BottleDto =
        bottleApi.update(id, request)

    suspend fun remove(id: String) = bottleApi.remove(id)

    suspend fun consume(
        id: String,
        quantity: Int? = null,
        rating: Int? = null,
        comment: String? = null,
        consumedDate: String? = null,
    ): ConsumeBottleResponse = bottleApi.consume(
        id,
        ConsumeBottleRequest(quantity, rating, comment, consumedDate),
    )
}
