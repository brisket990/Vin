package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.AuthResponseDto
import com.xothiques.vin.data.remote.dto.JoinHouseholdRequest
import com.xothiques.vin.data.remote.dto.LoginRequest
import com.xothiques.vin.data.remote.dto.RegisterHouseholdRequest
import com.xothiques.vin.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** No Authorization header required for these three -- see AuthInterceptor. */
interface AuthApi {
    @POST("api/vin/auth/register-household")
    suspend fun registerHousehold(@Body body: RegisterHouseholdRequest): AuthResponseDto

    @POST("api/vin/auth/join-household")
    suspend fun joinHousehold(@Body body: JoinHouseholdRequest): AuthResponseDto

    @POST("api/vin/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @GET("api/vin/auth/me")
    suspend fun me(): UserDto
}
