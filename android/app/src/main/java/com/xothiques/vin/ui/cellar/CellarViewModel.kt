package com.xothiques.vin.ui.cellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.repository.BottleFilters
import com.xothiques.vin.data.repository.BottleRepository
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
    private val bottleRepository: BottleRepository,
) : ViewModel() {

    private val _unitsState = MutableStateFlow<UiState<List<CellarUnitDto>>>(UiState.Loading)
    val unitsState: StateFlow<UiState<List<CellarUnitDto>>> = _unitsState.asStateFlow()

    private val _selectedUnitId = MutableStateFlow<String?>(null)
    val selectedUnitId: StateFlow<String?> = _selectedUnitId.asStateFlow()

    private val _createUnitState = MutableStateFlow<UiState<Unit>?>(null)
    val createUnitState: StateFlow<UiState<Unit>?> = _createUnitState.asStateFlow()

    // Bottles saved without a cellar location (e.g. a scan confirmed without
    // picking a casier) never occupy a grid cell, so CellarUnitContent alone
    // would never show them -- they'd silently exist only in the dashboard
    // count. Surfaced separately here so nothing a household adds is ever
    // lost/unreachable.
    private val _unassignedBottlesState = MutableStateFlow<UiState<List<BottleDto>>>(UiState.Loading)
    val unassignedBottlesState: StateFlow<UiState<List<BottleDto>>> = _unassignedBottlesState.asStateFlow()

    init {
        loadUnits()
        loadUnassignedBottles()
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

    fun loadUnassignedBottles() {
        viewModelScope.launch {
            _unassignedBottlesState.value = try {
                val bottles = bottleRepository.findAll(BottleFilters(status = "in_cellar"))
                UiState.Success(bottles.filter { it.locationId == null })
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    /** Call after returning from the bottle form/detail screens, in case a location was just assigned. */
    fun refresh() {
        loadUnits()
        loadUnassignedBottles()
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
