package com.xothiques.vin.ui.cellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.CellarSiteDto
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the cellar screen. A household can manage several caves (e.g. the
 * house plus an apartment) -- sitesState holds all of them for the tab
 * selector, activeSiteId is the one currently displayed (persisted via
 * SessionManager so it survives app restarts), and unitsState is already
 * filtered down to that site's casiers only, per the "one cave shown at a
 * time" design.
 */
@HiltViewModel
class CellarViewModel @Inject constructor(
    private val cellarRepository: CellarRepository,
    private val bottleRepository: BottleRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _sitesState = MutableStateFlow<UiState<List<CellarSiteDto>>>(UiState.Loading)
    val sitesState: StateFlow<UiState<List<CellarSiteDto>>> = _sitesState.asStateFlow()

    private val _activeSiteId = MutableStateFlow<String?>(null)
    val activeSiteId: StateFlow<String?> = _activeSiteId.asStateFlow()

    private val _createSiteState = MutableStateFlow<UiState<Unit>?>(null)
    val createSiteState: StateFlow<UiState<Unit>?> = _createSiteState.asStateFlow()

    private val _renameSiteState = MutableStateFlow<UiState<Unit>?>(null)
    val renameSiteState: StateFlow<UiState<Unit>?> = _renameSiteState.asStateFlow()

    // Every casier of the household, across all sites -- unitsState exposed
    // below is this list filtered down to the active site.
    private var allUnits: List<CellarUnitDto> = emptyList()

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
        loadAll()
        loadUnassignedBottles()
    }

    /** Loads sites and units together and resolves which site is active --
     *  the one persisted from last time if it still exists, otherwise the
     *  first one available (backfilling that choice to SessionManager so
     *  it sticks next time). */
    fun loadAll() {
        viewModelScope.launch {
            _sitesState.value = UiState.Loading
            _unitsState.value = UiState.Loading
            try {
                val sites = cellarRepository.listSites()
                allUnits = cellarRepository.listUnits()
                _sitesState.value = UiState.Success(sites)

                val stored = sessionManager.activeSiteId.first()
                val resolved = sites.firstOrNull { it.id == stored } ?: sites.firstOrNull()
                _activeSiteId.value = resolved?.id
                if (resolved != null && resolved.id != stored) {
                    sessionManager.setActiveSiteId(resolved.id)
                }
                applyUnitFilter()
            } catch (t: Throwable) {
                val message = t.toUserMessage()
                _sitesState.value = UiState.Error(message)
                _unitsState.value = UiState.Error(message)
            }
        }
    }

    fun loadUnits() {
        viewModelScope.launch {
            _unitsState.value = UiState.Loading
            _unitsState.value = try {
                allUnits = cellarRepository.listUnits()
                UiState.Success(unitsForActiveSite())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    private fun unitsForActiveSite(): List<CellarUnitDto> {
        val siteId = _activeSiteId.value ?: return emptyList()
        return allUnits.filter { it.siteId == siteId }
    }

    private fun applyUnitFilter() {
        _unitsState.value = UiState.Success(unitsForActiveSite())
    }

    /** Switches which cave is displayed -- persisted so it's remembered
     *  next time the app opens. */
    fun selectSite(siteId: String) {
        if (siteId == _activeSiteId.value) return
        _activeSiteId.value = siteId
        applyUnitFilter()
        viewModelScope.launch { sessionManager.setActiveSiteId(siteId) }
    }

    fun resetCreateSiteState() {
        _createSiteState.value = null
    }

    fun createSite(name: String) {
        viewModelScope.launch {
            _createSiteState.value = UiState.Loading
            _createSiteState.value = try {
                val site = cellarRepository.createSite(name)
                _sitesState.value = UiState.Success(cellarRepository.listSites())
                selectSite(site.id)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetRenameSiteState() {
        _renameSiteState.value = null
    }

    fun renameSite(siteId: String, name: String) {
        viewModelScope.launch {
            _renameSiteState.value = UiState.Loading
            _renameSiteState.value = try {
                cellarRepository.updateSite(siteId, name)
                _sitesState.value = UiState.Success(cellarRepository.listSites())
                UiState.Success(Unit)
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
        loadAll()
        loadUnassignedBottles()
    }

    fun resetCreateUnitState() {
        _createUnitState.value = null
    }

    fun createUnit(name: String, rowCount: Int, columnCount: Int, preferredColor: String?) {
        val siteId = _activeSiteId.value
        if (siteId == null) {
            _createUnitState.value = UiState.Error("Crée d'abord une cave.")
            return
        }
        viewModelScope.launch {
            _createUnitState.value = UiState.Loading
            _createUnitState.value = try {
                cellarRepository.createUnit(
                    siteId = siteId,
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

    /** [preferredColor] is one of the wine colors, or "none" to clear it back to "mixed". */
    fun updateUnit(unitId: String, name: String, preferredColor: String?) {
        viewModelScope.launch {
            _updateUnitState.value = UiState.Loading
            _updateUnitState.value = try {
                cellarRepository.updateUnit(unitId, name, preferredColor)
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
