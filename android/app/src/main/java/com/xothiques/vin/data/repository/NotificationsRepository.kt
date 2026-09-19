package com.xothiques.vin.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.xothiques.vin.data.remote.NotificationsApi
import com.xothiques.vin.data.remote.dto.RegisterDeviceRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val notificationsApi: NotificationsApi,
) {
    suspend fun registerDevice(token: String, platform: String = "android") =
        notificationsApi.registerDevice(RegisterDeviceRequest(token, platform))

    suspend fun unregisterDevice(token: String) = notificationsApi.unregisterDevice(token)

    suspend fun sendTest() = notificationsApi.sendTest()

    /** Fetches this device's current FCM token and registers it with the
     *  backend. Unlike [PushNotificationViewModel]'s automatic call to this
     *  same flow (which silently swallows failures -- push is a nice-to-have,
     *  never something that should nag the user), callers that need to know
     *  *why* registration failed (e.g. the Settings "send a test
     *  notification" button, to explain a persistent "no device registered")
     *  should let this one's exception propagate. */
    suspend fun registerCurrentDevice() {
        val token = FirebaseMessaging.getInstance().token.await()
        registerDevice(token)
    }
}
