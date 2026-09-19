package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String? = null,
)

/** Result of POST /notifications/devices/test -- tells the caller exactly
 *  what happened so it can explain any failure precisely rather than just
 *  "no notification arrived": [configured] false means Firebase isn't set
 *  up on the backend yet, [deviceCount] 0 means this backend has no device
 *  token on file for the household (nothing to send to), and otherwise
 *  [successCount] (out of [deviceCount]) actually accepted the push. */
@Serializable
data class TestNotificationResultDto(
    val configured: Boolean,
    val deviceCount: Int,
    val successCount: Int,
)
