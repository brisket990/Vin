package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.DashboardStatsDto
import retrofit2.http.GET

interface DashboardApi {
    @GET("api/vin/dashboard")
    suspend fun getStats(): DashboardStatsDto
}
