package com.xothiques.vin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.AiProviderConfigDto
import com.xothiques.vin.data.repository.AiProviderRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProviderSettingsViewModel @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<UiState<List<AiProviderConfigDto>>>(UiState.Loading)
    val listState: StateFlow<UiState<List<AiProviderConfigDto>>> = _listState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Unit>?>(null)
    val submitState: StateFlow<UiState<Unit>?> = _submitState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _listState.value = UiState.Loading
            _listState.value = try {
                UiState.Success(aiProviderRepository.list())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun upsert(
        provider: String,
        apiKey: String,
        model: String?,
        usage: String,
        isDefault: Boolean,
    ) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                aiProviderRepository.upsert(provider, apiKey, model, usage, isDefault)
                load()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try {
                aiProviderRepository.remove(id)
                load()
            } catch (t: Throwable) {
                _listState.value = UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = null
    }
}
