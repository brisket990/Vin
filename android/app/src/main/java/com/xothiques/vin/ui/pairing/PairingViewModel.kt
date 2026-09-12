package com.xothiques.vin.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import com.xothiques.vin.data.repository.PairingRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
) : ViewModel() {

    private val _historyState = MutableStateFlow<UiState<List<PairingSuggestionDto>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<PairingSuggestionDto>>> = _historyState.asStateFlow()

    private val _suggestState = MutableStateFlow<UiState<PairingSuggestionDto>?>(null)
    val suggestState: StateFlow<UiState<PairingSuggestionDto>?> = _suggestState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = UiState.Loading
            _historyState.value = try {
                UiState.Success(pairingRepository.findAll().sortedByDescending { it.createdAt })
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun suggest(dishDescription: String) {
        viewModelScope.launch {
            _suggestState.value = UiState.Loading
            _suggestState.value = try {
                val result = pairingRepository.suggest(dishDescription)
                loadHistory()
                UiState.Success(result)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSuggestState() {
        _suggestState.value = null
    }
}
