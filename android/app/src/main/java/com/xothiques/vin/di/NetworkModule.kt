package com.xothiques.vin.di

import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.AiProviderApi
import com.xothiques.vin.data.remote.AuthApi
import com.xothiques.vin.data.remote.AuthInterceptor
import com.xothiques.vin.data.remote.BottleApi
import com.xothiques.vin.data.remote.CellarApi
import com.xothiques.vin.data.remote.DashboardApi
import com.xothiques.vin.data.remote.ExportApi
import com.xothiques.vin.data.remote.HouseholdApi
import com.xothiques.vin.data.remote.PairingApi
import com.xothiques.vin.data.remote.ScanApi
import com.xothiques.vin.data.remote.TastingApi
import com.xothiques.vin.data.remote.WishlistApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC rather than BODY: request bodies can carry AI provider API
            // keys or photo bytes -- avoid ever writing those to logcat.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Base URL is a placeholder: every real request has its scheme/host/port
     * rewritten by AuthInterceptor to the household's configured server, so
     * only the path (always starting with "api/vin/...", see each *Api
     * interface) actually matters here.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides @Singleton fun provideHouseholdApi(retrofit: Retrofit): HouseholdApi = retrofit.create(HouseholdApi::class.java)
    @Provides @Singleton fun provideCellarApi(retrofit: Retrofit): CellarApi = retrofit.create(CellarApi::class.java)
    @Provides @Singleton fun provideBottleApi(retrofit: Retrofit): BottleApi = retrofit.create(BottleApi::class.java)
    @Provides @Singleton fun provideAiProviderApi(retrofit: Retrofit): AiProviderApi = retrofit.create(AiProviderApi::class.java)
    @Provides @Singleton fun provideScanApi(retrofit: Retrofit): ScanApi = retrofit.create(ScanApi::class.java)
    @Provides @Singleton fun providePairingApi(retrofit: Retrofit): PairingApi = retrofit.create(PairingApi::class.java)
    @Provides @Singleton fun provideTastingApi(retrofit: Retrofit): TastingApi = retrofit.create(TastingApi::class.java)
    @Provides @Singleton fun provideWishlistApi(retrofit: Retrofit): WishlistApi = retrofit.create(WishlistApi::class.java)
    @Provides @Singleton fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)
    @Provides @Singleton fun provideExportApi(retrofit: Retrofit): ExportApi = retrofit.create(ExportApi::class.java)
}
