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
 * Three jobs in one interceptor, all needed on every request since
 * Retrofit's baseUrl is fixed at build time but this app talks to a
 * self-hosted server the user configures at runtime:
 *  1. Rewrite scheme/host/port to the currently configured server address.
 *  2. Attach the JWT (unless the request is one of the public auth routes).
 *  3. On a 401 for an authenticated request, the token is expired or
 *     otherwise invalid (the backend issues JWTs with a finite lifetime --
 *     see JWT_EXPIRES_IN) -- clear the stored session so VinNavHost's
 *     `!session.isLoggedIn` check swaps the whole app over to the login
 *     screen on its own. Without this, every screen was stuck re-showing
 *     the same "Unauthorized" error forever: Réessayer just replayed the
 *     same dead token, and there was no sign-out entry point reachable from
 *     that error state.
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

        val response = chain.proceed(requestBuilder.build())

        if (!isPublic && session.accessToken != null && response.code == 401) {
            runBlocking { sessionManager.signOut() }
        }

        return response
    }
}
