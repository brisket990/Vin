package com.xothiques.vin.data.repository

import com.xothiques.vin.data.local.Session
import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.AuthApi
import com.xothiques.vin.data.remote.dto.AuthResponseDto
import com.xothiques.vin.data.remote.dto.JoinHouseholdRequest
import com.xothiques.vin.data.remote.dto.LoginRequest
import com.xothiques.vin.data.remote.dto.RegisterHouseholdRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
) {
    val session: Flow<Session> = sessionManager.session

    suspend fun setServerBaseUrl(url: String) = sessionManager.setServerBaseUrl(url)

    suspend fun currentServerBaseUrl(): String = sessionManager.currentSession().serverBaseUrl

    suspend fun registerHousehold(
        householdName: String,
        email: String,
        password: String,
        displayName: String,
    ): AuthResponseDto {
        val response = authApi.registerHousehold(
            RegisterHouseholdRequest(householdName, email, password, displayName),
        )
        persist(response)
        return response
    }

    suspend fun joinHousehold(
        inviteCode: String,
        email: String,
        password: String,
        displayName: String,
    ): AuthResponseDto {
        val response = authApi.joinHousehold(
            JoinHouseholdRequest(inviteCode, email, password, displayName),
        )
        persist(response)
        return response
    }

    suspend fun login(email: String, password: String): AuthResponseDto {
        val response = authApi.login(LoginRequest(email, password))
        persist(response)
        return response
    }

    suspend fun signOut() = sessionManager.signOut()

    private suspend fun persist(response: AuthResponseDto) {
        sessionManager.signIn(
            accessToken = response.accessToken,
            userId = response.user.id,
            householdId = response.user.householdId,
            role = response.user.role,
            displayName = response.user.displayName,
            email = response.user.email,
        )
    }
}
