package com.xothiques.vin.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fetches the current device's FCM token and registers it with the backend.
 * Called once per login session (see PushNotificationSetup) -- covers both
 * "just logged in, backend has never seen this device" and "token rotated
 * since last time" without needing a separate onNewToken hook to fire.
 *
 * Silently gives up on failure (no Firebase project configured yet, no
 * network, whatever): push notifications are a nice-to-have on top of the
 * app's core cellar management, never something that should block or nag
 * the user if unavailable.
 */
@HiltViewModel
class PushNotificationViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) : ViewModel() {

    fun registerCurrentDevice() {
        viewModelScope.launch {
            runCatching { notificationsRepository.registerCurrentDevice() }
        }
    }
}
