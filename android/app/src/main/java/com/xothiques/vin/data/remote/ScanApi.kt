package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.LinkScanBottleRequest
import com.xothiques.vin.data.remote.dto.ScanResultDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ScanApi {
    @Multipart
    @POST("api/vin/scan")
    suspend fun scan(
        @Part photo: MultipartBody.Part,
        @Part("provider") provider: RequestBody? = null,
    ): ScanResultDto

    @GET("api/vin/scan")
    suspend fun findAll(): List<ScanResultDto>

    @GET("api/vin/scan/{id}")
    suspend fun findOne(@Path("id") id: String): ScanResultDto

    @PATCH("api/vin/scan/{id}/link-bottle")
    suspend fun linkBottle(
        @Path("id") id: String,
        @Body body: LinkScanBottleRequest,
    ): ScanResultDto
}
