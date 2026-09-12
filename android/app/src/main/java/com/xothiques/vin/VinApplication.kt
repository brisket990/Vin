package com.xothiques.vin

import android.app.Application
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

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .build()
}
