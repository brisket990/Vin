package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecentTastingDto(
    val id: String,
    val bottleId: String,
    val bottleName: String,
    val rating: Int? = null,
    val comment: String? = null,
    val consumedDate: String,
)

@Serializable
data class DashboardStatsDto(
    val totalBottles: Int,
    val totalValueCents: Int,
    val byColor: Map<String, Int>,
    val upcomingApogee: List<BottleDto>,
    val recentTastings: List<RecentTastingDto>,
)
