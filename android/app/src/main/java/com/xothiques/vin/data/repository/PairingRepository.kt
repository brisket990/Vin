package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.PairingApi
import com.xothiques.vin.data.remote.dto.CreatePairingRequest
import com.xothiques.vin.data.remote.dto.FoodPairingResultDto
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingRepository @Inject constructor(
    private val pairingApi: PairingApi,
) {
    suspend fun suggest(dishDescription: String, provider: String? = null): PairingSuggestionDto =
        pairingApi.suggest(CreatePairingRequest(dishDescription, provider))

    suspend fun suggestForBottle(bottleId: String, provider: String? = null): FoodPairingResultDto =
        pairingApi.suggestForBottle(bottleId, provider)

    suspend fun findAll(): List<PairingSuggestionDto> = pairingApi.findAll()

    suspend fun findOne(id: String): PairingSuggestionDto = pairingApi.findOne(id)
}
