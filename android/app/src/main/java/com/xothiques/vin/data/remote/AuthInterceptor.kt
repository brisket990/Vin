package com.xothiques.vin.data.remote

import com.xothiques.vin.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private val PUBLIC_PATH_SUFFIXES = listOf(
    "api/vin/auth/login",
    "api/vin/auth/register-household",
    "api/vin/auth/join-household",
    "api/vin/health",
)

/**
 * Two jobs in one interceptor, both needed on every request since Retrofit's
 * baseUrl is fixed at build time but this app talks to a self-hosted server
 * the user configures at runtime:
 *  1. Rewrite scheme/host/port to the currently configured server address.
 *  2. Attach the JWT (unless the request is one of the public auth routes).
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val session = runBlocking { sessionManager.currentSession() }

        var urlBuilder = original.url.newBuilder()
        if (session.serverBaseUrl.isNotBlank()) {
            session.serverBaseUrl.toHttpUrlOrNull()?.let { configured ->
                urlBuilder = urlBuilder
                    .scheme(configured.scheme)
                    .host(configured.host)
                    .port(configured.port)
            }
        }

        val requestBuilder = original.newBuilder().url(urlBuilder.build())

        val path = original.url.encodedPath.removePrefix("/")
        val isPublic = PUBLIC_PATH_SUFFIXES.any { path == it }
        if (!isPublic && session.accessToken != null) {
            requestBuilder.header("Authorization", "Bearer ${session.accessToken}")
        }

        return chain.proceed(requestBuilder.build())
    }
}
