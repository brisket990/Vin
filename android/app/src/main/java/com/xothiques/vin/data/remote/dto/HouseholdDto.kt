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

/** One entry of GET /household/mine -- every foyer this account belongs to,
 *  with this account's role in that specific one. Drives the foyer switcher. */
@Serializable
data class HouseholdSummaryDto(
    val id: String,
    val name: String,
    val inviteCode: String,
    val role: String,
)

@Serializable
data class CreateHouseholdRequest(val name: String)

/** Join an existing foyer with the CURRENT account (no new login/password) --
 *  the counterpart to JoinHouseholdRequest, which instead creates a new account. */
@Serializable
data class JoinHouseholdByCodeRequest(val inviteCode: String)

@Serializable
data class SwitchHouseholdRequest(val householdId: String)

/** Response of DELETE /household/:id. When the deleted foyer was the one the
 *  caller's current token was scoped to, the backend reissues a token for the
 *  fallback foyer it fell back to (switched = true, accessToken/user set);
 *  otherwise it just confirms (switched = false, nothing else set). */
@Serializable
data class DeleteHouseholdResponseDto(
    val switched: Boolean,
    val accessToken: String? = null,
    val user: UserDto? = null,
)

/** Response of POST /household (create) and POST /household/join -- a new
 *  household plus a token already scoped to it, so the app can switch
 *  straight in without a second round trip. */
@Serializable
data class HouseholdSessionDto(
    val household: HouseholdDto,
    val accessToken: String,
    val user: UserDto,
)
