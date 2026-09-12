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
) {
    suspend fun create(request: CreateBottleRequest): BottleDto = bottleApi.create(request)

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
