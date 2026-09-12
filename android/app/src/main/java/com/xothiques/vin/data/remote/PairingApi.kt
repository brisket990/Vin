package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.CreatePairingRequest
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PairingApi {
    @POST("api/vin/pairing")
    suspend fun suggest(@Body body: CreatePairingRequest): PairingSuggestionDto

    @GET("api/vin/pairing")
    suspend fun findAll(): List<PairingSuggestionDto>

    @GET("api/vin/pairing/{id}")
    suspend fun findOne(@Path("id") id: String): PairingSuggestionDto
}
