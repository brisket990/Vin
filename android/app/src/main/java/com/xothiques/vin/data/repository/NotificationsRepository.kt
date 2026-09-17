package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.NotificationsApi
import com.xothiques.vin.data.remote.dto.RegisterDeviceRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val notificationsApi: NotificationsApi,
) {
    suspend fun registerDevice(token: String, platform: String = "android") =
        notificationsApi.registerDevice(RegisterDeviceRequest(token, platform))

    suspend fun unregisterDevice(token: String) = notificationsApi.unregisterDevice(token)
}
