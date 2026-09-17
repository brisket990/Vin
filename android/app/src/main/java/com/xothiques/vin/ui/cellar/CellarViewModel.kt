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
 * Drives the cellar screen. All of the household's casiers are shown
 * together in one view (the screen adds visual spacing between them) --
 * unitsState holds every one of them, in no particular grouping.
 */
@HiltViewModel
class CellarViewModel @Inject constructor(
    private val cellarRepository: CellarRepository,
    private val bottleRepository: BottleRepository,
) : ViewModel() {

    private val _unitsState = MutableStateFlow<UiState<List<CellarUnitDto>>>(UiState.Loading)
    val unitsState: StateFlow<UiState<List<CellarUnitDto>>> = _unitsState.asStateFlow()

    private val _createUnitState = MutableStateFlow<UiState<Unit>?>(null)
    val createUnitState: StateFlow<UiState<Unit>?> = _createUnitState.asStateFlow()

    private val _updateUnitState = MutableStateFlow<UiState<Unit>?>(null)
    val updateUnitState: StateFlow<UiState<Unit>?> = _updateUnitState.asStateFlow()

    private val _deleteUnitState = MutableStateFlow<UiState<Unit>?>(null)
    val deleteUnitState: StateFlow<UiState<Unit>?> = _deleteUnitState.asStateFlow()

    // Bottles saved without a cellar location (e.g. a scan confirmed without
    // picking a casier) never occupy a grid cell, so the per-unit grids alone
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
                UiState.Success(cellarRepository.listUnits())
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

    /** Call after returning from the bottle form/detail screens, in case a
     *  location was just assigned. Also fired on every ON_RESUME of the
     *  cellar screen (it stays alive underneath Scan/Ajouter/Détail on the
     *  back stack), so a plain [loadUnits] here would flash the list back to
     *  [UiState.Loading] on every return trip -- which unmounts and remounts
     *  the grid's LazyColumn and resets the user's scroll position even
     *  though nothing actually changed. Once the units are already loaded,
     *  refresh them silently in place instead; only a genuine first load (or
     *  a retry from the error screen) goes through [loadUnits]. */
    fun refresh() {
        val current = _unitsState.value
        if (current is UiState.Success) {
            reloadUnitsSilently()
        } else {
            loadUnits()
        }
        loadUnassignedBottles()
    }

    private fun reloadUnitsSilently() {
        viewModelScope.launch {
            try {
                _unitsState.value = UiState.Success(cellarRepository.listUnits())
            } catch (t: Throwable) {
                // Keep showing the last known-good list rather than surfacing
                // a transient background-refresh failure on top of it.
            }
        }
    }

    fun resetCreateUnitState() {
        _createUnitState.value = null
    }

    fun createUnit(name: String, rowCount: Int, columnCount: Int, preferredColor: String?) {
        viewModelScope.launch {
            _createUnitState.value = UiState.Loading
            _createUnitState.value = try {
                cellarRepository.createUnit(
                    name = name,
                    rowCount = rowCount,
                    columnCount = columnCount,
                    preferredColor = preferredColor,
                )
                loadUnits()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetUpdateUnitState() {
        _updateUnitState.value = null
    }

    /** [preferredColor] is one of the wine colors, or "none" to clear it back
     *  to "mixed". [rowCount]/[columnCount] resize the grid when non-null --
     *  shrinking is refused server-side (BadRequest) if it would delete an
     *  occupied location; that message is surfaced to the user as-is. */
    fun updateUnit(
        unitId: String,
        name: String,
        preferredColor: String?,
        rowCount: Int? = null,
        columnCount: Int? = null,
    ) {
        viewModelScope.launch {
            _updateUnitState.value = UiState.Loading
            _updateUnitState.value = try {
                cellarRepository.updateUnit(unitId, name, preferredColor, rowCount, columnCount)
                loadUnits()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetDeleteUnitState() {
        _deleteUnitState.value = null
    }

    /** Refused server-side (BadRequest) while the casier still holds an
     *  in-cellar bottle -- that message is surfaced to the user as-is. */
    fun deleteUnit(unitId: String) {
        viewModelScope.launch {
            _deleteUnitState.value = UiState.Loading
            _deleteUnitState.value = try {
                cellarRepository.deleteUnit(unitId)
                loadUnits()
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
