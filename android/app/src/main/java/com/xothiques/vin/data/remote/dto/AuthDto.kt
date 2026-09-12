package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val householdId: String,
    val role: String, // "owner" | "member"
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val user: UserDto,
)

@Serializable
data class RegisterHouseholdRequest(
    val householdName: String,
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class JoinHouseholdRequest(
    val inviteCode: String,
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)
