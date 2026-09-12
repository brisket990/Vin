package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HouseholdMemberDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
)

@Serializable
data class HouseholdDto(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdAt: String,
    val members: List<HouseholdMemberDto> = emptyList(),
)

@Serializable
data class UpdateHouseholdRequest(val name: String)
