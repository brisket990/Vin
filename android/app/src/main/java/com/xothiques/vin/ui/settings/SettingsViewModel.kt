package com.xothiques.vin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.local.Session
import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.remote.dto.HouseholdSummaryDto
import com.xothiques.vin.data.repository.AuthRepository
import com.xothiques.vin.data.repository.HouseholdRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    val session: StateFlow<Session?> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _householdState = MutableStateFlow<UiState<HouseholdDto>>(UiState.Loading)
    val householdState: StateFlow<UiState<HouseholdDto>> = _householdState.asStateFlow()

    private val _regenerateState = MutableStateFlow<UiState<Unit>?>(null)
    val regenerateState: StateFlow<UiState<Unit>?> = _regenerateState.asStateFlow()

    // Every foyer this account belongs to -- drives the switcher UI in the
    // "Foyer" section. Loaded alongside the active household's full detail.
    private val _householdsState = MutableStateFlow<UiState<List<HouseholdSummaryDto>>>(UiState.Loading)
    val householdsState: StateFlow<UiState<List<HouseholdSummaryDto>>> = _householdsState.asStateFlow()

    private val _switchHouseholdState = MutableStateFlow<UiState<Unit>?>(null)
    val switchHouseholdState: StateFlow<UiState<Unit>?> = _switchHouseholdState.asStateFlow()

    private val _createHouseholdState = MutableStateFlow<UiState<Unit>?>(null)
    val createHouseholdState: StateFlow<UiState<Unit>?> = _createHouseholdState.asStateFlow()

    private val _joinHouseholdState = MutableStateFlow<UiState<Unit>?>(null)
    val joinHouseholdState: StateFlow<UiState<Unit>?> = _joinHouseholdState.asStateFlow()

    private val _deleteHouseholdState = MutableStateFlow<UiState<Unit>?>(null)
    val deleteHouseholdState: StateFlow<UiState<Unit>?> = _deleteHouseholdState.asStateFlow()

    init {
        load()
        loadHouseholds()
    }

    fun load() {
        viewModelScope.launch {
            _householdState.value = UiState.Loading
            _householdState.value = try {
                UiState.Success(householdRepository.getMine())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun loadHouseholds() {
        viewModelScope.launch {
            _householdsState.value = UiState.Loading
            _householdsState.value = try {
                UiState.Success(householdRepository.listMine())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun regenerateInviteCode() {
        viewModelScope.launch {
            _regenerateState.value = UiState.Loading
            _regenerateState.value = try {
                val household = householdRepository.regenerateInviteCode()
                _householdState.value = UiState.Success(household)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            _householdState.value = try {
                UiState.Success(householdRepository.rename(name))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSwitchHouseholdState() {
        _switchHouseholdState.value = null
    }

    /** Switches the active foyer -- HouseholdRepository already replaces the
     *  app's stored token, so every screen's next fetch (each refreshes on
     *  its own ON_RESUME, e.g. CellarGridScreen) is automatically scoped to
     *  the new foyer. Reloads this screen's own data right away. */
    fun switchHousehold(householdId: String) {
        viewModelScope.launch {
            _switchHouseholdState.value = UiState.Loading
            _switchHouseholdState.value = try {
                householdRepository.switch(householdId)
                load()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetCreateHouseholdState() {
        _createHouseholdState.value = null
    }

    /** Creates an additional foyer (e.g. "Appartement") and switches into it
     *  immediately. */
    fun createHousehold(name: String) {
        viewModelScope.launch {
            _createHouseholdState.value = UiState.Loading
            _createHouseholdState.value = try {
                householdRepository.create(name)
                load()
                loadHouseholds()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetJoinHouseholdState() {
        _joinHouseholdState.value = null
    }

    /** Joins an existing foyer with the CURRENT account via its invite code
     *  and switches into it immediately -- distinct from the unauthenticated
     *  "join a foyer" flow on the login screen, which creates a new account. */
    fun joinHousehold(inviteCode: String) {
        viewModelScope.launch {
            _joinHouseholdState.value = UiState.Loading
            _joinHouseholdState.value = try {
                householdRepository.joinByCode(inviteCode)
                load()
                loadHouseholds()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetDeleteHouseholdState() {
        _deleteHouseholdState.value = null
    }

    /** Permanently deletes a foyer. HouseholdRepository already replaces the
     *  app's stored token if the deleted foyer was the active one, so this
     *  just reloads both the active household and the switcher list -- the
     *  same pattern as [createHousehold]/[joinHousehold]. */
    fun deleteHousehold(householdId: String) {
        viewModelScope.launch {
            _deleteHouseholdState.value = UiState.Loading
            _deleteHouseholdState.value = try {
                householdRepository.delete(householdId)
                load()
                loadHouseholds()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
