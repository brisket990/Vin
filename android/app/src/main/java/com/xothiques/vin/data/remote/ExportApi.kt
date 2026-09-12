package com.xothiques.vin.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming

interface ExportApi {
    @Streaming
    @GET("api/vin/export/cellar.csv")
    suspend fun exportCsv(): ResponseBody

    @Streaming
    @GET("api/vin/export/cellar.pdf")
    suspend fun exportPdf(): ResponseBody
}
