package com.xothiques.vin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.data.repository.AuthRepository
import com.xothiques.vin.data.repository.HouseholdRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _householdState = MutableStateFlow<UiState<HouseholdDto>>(UiState.Loading)
    val householdState: StateFlow<UiState<HouseholdDto>> = _householdState.asStateFlow()

    private val _regenerateState = MutableStateFlow<UiState<Unit>?>(null)
    val regenerateState: StateFlow<UiState<Unit>?> = _regenerateState.asStateFlow()

    init {
        load()
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

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
