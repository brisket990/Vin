package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.DashboardApi
import com.xothiques.vin.data.remote.dto.DashboardStatsDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi,
) {
    suspend fun getStats(): DashboardStatsDto = dashboardApi.getStats()
}
