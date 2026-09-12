package com.xothiques.vin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.repository.AuthRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _submitState = MutableStateFlow<UiState<Unit>?>(null)
    val submitState: StateFlow<UiState<Unit>?> = _submitState.asStateFlow()

    fun resetSubmitState() {
        _submitState.value = null
    }

    suspend fun currentServerBaseUrl(): String = authRepository.currentServerBaseUrl()

    fun saveServerBaseUrl(url: String) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            authRepository.setServerBaseUrl(url)
            _submitState.value = UiState.Success(Unit)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                authRepository.login(email, password)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun registerHousehold(
        householdName: String,
        email: String,
        password: String,
        displayName: String,
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                authRepository.registerHousehold(householdName, email, password, displayName)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun joinHousehold(
        inviteCode: String,
        email: String,
        password: String,
        displayName: String,
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                authRepository.joinHousehold(inviteCode, email, password, displayName)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
