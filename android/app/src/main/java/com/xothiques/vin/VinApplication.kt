package com.xothiques.vin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class VinApplication : Application(), ImageLoaderFactory {

    // Same client used by Retrofit: it already rewrites the placeholder host
    // to the configured server and attaches the JWT, both of which the
    // authenticated /api/vin/photos/* route needs.
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // Required on API 26+ for a notification to reliably show (some OEMs
    // silently drop notifications with no channel) -- referenced by id from
    // AndroidManifest's default_notification_channel_id meta-data, so any
    // FCM notification-payload push (quart de tour, later apogée alerts)
    // lands here without VinFirebaseMessagingService having to build one itself.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            "vin_reminders",
            "Rappels de cave",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Quart de tour, fenêtres d'apogée, et autres rappels sur ta cave."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .build()
}
