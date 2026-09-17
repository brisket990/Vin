package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.ImportCsvResultDto
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImportApi {
    @Multipart
    @POST("api/vin/import/cellar/csv")
    suspend fun importCsv(@Part file: MultipartBody.Part): ImportCsvResultDto
}
