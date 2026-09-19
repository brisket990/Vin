package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.RegisterDeviceRequest
import com.xothiques.vin.data.remote.dto.TestNotificationResultDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationsApi {
    @POST("api/vin/notifications/devices")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest)

    @DELETE("api/vin/notifications/devices/{token}")
    suspend fun unregisterDevice(@Path("token") token: String)

    @POST("api/vin/notifications/devices/test")
    suspend fun sendTest(): TestNotificationResultDto
}
