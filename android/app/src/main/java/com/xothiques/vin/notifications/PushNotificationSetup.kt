package com.xothiques.vin.notifications

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Invisible composable dropped once at the root of the logged-in app
 * (MainNavGraph): requests the notification permission on Android 13+ (a
 * push simply won't display without it on those versions) and registers
 * this device's current FCM token with the backend, so quart de tour
 * reminders (and later apogée alerts) actually reach the phone.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PushNotificationSetup(viewModel: PushNotificationViewModel = hiltViewModel()) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    // Independent of the permission above -- a token is worth registering
    // even if notifications can't display yet (e.g. denied for now), so a
    // later permission grant doesn't need a fresh app session to catch up.
    LaunchedEffect(Unit) {
        viewModel.registerCurrentDevice()
    }
}
