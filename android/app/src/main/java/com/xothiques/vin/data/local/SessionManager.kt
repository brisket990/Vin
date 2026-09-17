package com.xothiques.vin.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vin_session")

data class Session(
    val serverBaseUrl: String,
    val accessToken: String?,
    val userId: String?,
    val householdId: String?,
    val role: String?,
    val displayName: String?,
    val email: String?,
) {
    val isLoggedIn: Boolean get() = accessToken != null
}

/**
 * Persists the self-hosted server URL and the logged-in session (JWT +
 * basic user/household info) across app restarts. There is no
 * multi-tenant backend here -- the server URL is whatever Coolify instance
 * the household points the app at, configured once on first launch.
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val ROLE = stringPreferencesKey("role")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACTIVE_SITE_ID = stringPreferencesKey("active_site_id")
    }

    val session: Flow<Session> = dataStore.data.map { prefs ->
        Session(
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: "",
            accessToken = prefs[Keys.ACCESS_TOKEN],
            userId = prefs[Keys.USER_ID],
            householdId = prefs[Keys.HOUSEHOLD_ID],
            role = prefs[Keys.ROLE],
            displayName = prefs[Keys.DISPLAY_NAME],
            email = prefs[Keys.EMAIL],
        )
    }

    suspend fun currentSession(): Session = session.first()

    suspend fun setServerBaseUrl(url: String) {
        dataStore.edit { it[Keys.SERVER_BASE_URL] = url.trimEnd('/') }
    }

    suspend fun signIn(
        accessToken: String,
        userId: String,
        householdId: String,
        role: String,
        displayName: String,
        email: String,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.HOUSEHOLD_ID] = householdId
            prefs[Keys.ROLE] = role
            prefs[Keys.DISPLAY_NAME] = displayName
            prefs[Keys.EMAIL] = email
        }
    }

    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.HOUSEHOLD_ID)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.EMAIL)
        }
    }

    /** "system" (default, follows the phone setting), "light", or "dark". */
    val themeMode: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.THEME_MODE] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    /** Which cave (CellarSiteDto.id) is currently selected -- shared between
     *  the cave screen's tab selector and the location-suggestion flows
     *  (scan, add bottle) so they scope to the same site. Null until the
     *  user has picked one (or before any site exists). */
    val activeSiteId: Flow<String?> = dataStore.data.map { prefs -> prefs[Keys.ACTIVE_SITE_ID] }

    suspend fun setActiveSiteId(siteId: String) {
        dataStore.edit { it[Keys.ACTIVE_SITE_ID] = siteId }
    }
}
