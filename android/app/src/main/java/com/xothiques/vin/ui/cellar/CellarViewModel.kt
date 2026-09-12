package com.xothiques.vin.ui.cellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.repository.CellarRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the cellar grid screen. Loads every cellar unit for the household;
 * most households will only ever have one, but the backend supports several
 * (e.g. a garage cellar + a kitchen rack), so the UI lets the user switch
 * between them once more than one exists.
 */
@HiltViewModel
class CellarViewModel @Inject constructor(
    private val cellarRepository: CellarRepository,
) : ViewModel() {

    private val _unitsState = MutableStateFlow<UiState<List<CellarUnitDto>>>(UiState.Loading)
    val unitsState: StateFlow<UiState<List<CellarUnitDto>>> = _unitsState.asStateFlow()

    private val _selectedUnitId = MutableStateFlow<String?>(null)
    val selectedUnitId: StateFlow<String?> = _selectedUnitId.asStateFlow()

    private val _createUnitState = MutableStateFlow<UiState<Unit>?>(null)
    val createUnitState: StateFlow<UiState<Unit>?> = _createUnitState.asStateFlow()

    init {
        loadUnits()
    }

    fun loadUnits() {
        viewModelScope.launch {
            _unitsState.value = UiState.Loading
            _unitsState.value = try {
                val units = cellarRepository.listUnits()
                if (_selectedUnitId.value == null) {
                    _selectedUnitId.value = units.firstOrNull()?.id
                }
                UiState.Success(units)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun selectUnit(unitId: String) {
        _selectedUnitId.value = unitId
    }

    fun resetCreateUnitState() {
        _createUnitState.value = null
    }

    fun createUnit(name: String, rowCount: Int, columnCount: Int) {
        viewModelScope.launch {
            _createUnitState.value = UiState.Loading
            _createUnitState.value = try {
                val unit = cellarRepository.createUnit(name, rowCount, columnCount)
                _selectedUnitId.value = unit.id
                loadUnits()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
