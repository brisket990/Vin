package com.xothiques.vin.ui.bottle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.repository.BottleRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottleDetailViewModel @Inject constructor(
    private val bottleRepository: BottleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bottleId: String = checkNotNull(savedStateHandle["bottleId"])

    private val _bottleState = MutableStateFlow<UiState<BottleDto>>(UiState.Loading)
    val bottleState: StateFlow<UiState<BottleDto>> = _bottleState.asStateFlow()

    private val _consumeState = MutableStateFlow<UiState<Unit>?>(null)
    val consumeState: StateFlow<UiState<Unit>?> = _consumeState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Unit>?>(null)
    val deleteState: StateFlow<UiState<Unit>?> = _deleteState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _bottleState.value = UiState.Loading
            _bottleState.value = try {
                UiState.Success(bottleRepository.findOne(bottleId))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetConsumeState() {
        _consumeState.value = null
    }

    fun consume(quantity: Int?, rating: Int?, comment: String?) {
        viewModelScope.launch {
            _consumeState.value = UiState.Loading
            _consumeState.value = try {
                bottleRepository.consume(bottleId, quantity = quantity, rating = rating, comment = comment)
                load()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            _deleteState.value = try {
                bottleRepository.remove(bottleId)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
