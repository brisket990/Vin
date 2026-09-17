package com.xothiques.vin.ui.bottle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.repository.BottleFilters
import com.xothiques.vin.data.repository.BottleRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the flat "list" view of the cellar (as opposed to the grid), reached
 * from the cave screen's header. Shows every bottle actually in the cellar
 * (not consumed), regardless of whether it has a location assigned, sorted
 * alphabetically -- a quick, scrollable way to scan the whole collection
 * without navigating the grid.
 */
@HiltViewModel
class BottleListViewModel @Inject constructor(
    private val bottleRepository: BottleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<BottleDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BottleDto>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val bottles = bottleRepository.findAll(BottleFilters(status = "in_cellar"))
                    .sortedBy { it.name.lowercase() }
                UiState.Success(bottles)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
