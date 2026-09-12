package com.xothiques.vin.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Manual light/dark override, matching the toggle in Réglages -- "system" follows the phone. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    val themeMode: StateFlow<String> = sessionManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setThemeMode(mode: String) {
        viewModelScope.launch { sessionManager.setThemeMode(mode) }
    }
}
