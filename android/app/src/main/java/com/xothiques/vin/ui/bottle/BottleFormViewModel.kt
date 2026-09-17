package com.xothiques.vin.ui.bottle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
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
 * Handles both creation (optionally pre-filled with a locationId tapped from
 * the empty cellar cell) and editing of an existing bottle. bottleId is only
 * present in the edit case; the presence/absence of it in the back-stack
 * entry's arguments is what BottleFormScreen uses to pick the mode.
 */
@HiltViewModel
class BottleFormViewModel @Inject constructor(
    private val bottleRepository: BottleRepository,
    private val cellarRepository: CellarRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bottleId: String? = savedStateHandle["bottleId"]
    val preselectedLocationId: String? = savedStateHandle["locationId"]
    val isEditing: Boolean get() = bottleId != null

    private val _loadState = MutableStateFlow<UiState<BottleDto>?>(if (isEditing) UiState.Loading else null)
    val loadState: StateFlow<UiState<BottleDto>?> = _loadState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Unit>?>(null)
    val submitState: StateFlow<UiState<Unit>?> = _submitState.asStateFlow()

    private val _suggestions = MutableStateFlow<UiState<List<SuggestedLocationDto>>?>(null)
    val suggestions: StateFlow<UiState<List<SuggestedLocationDto>>?> = _suggestions.asStateFlow()

    // Full casier list (with their grids) so LocationPicker can offer an
    // explicit "choisis un casier, puis une niche" flow instead of only the
    // algorithm's top suggestions.
    private val _unitsState = MutableStateFlow<UiState<List<CellarUnitDto>>>(UiState.Loading)
    val unitsState: StateFlow<UiState<List<CellarUnitDto>>> = _unitsState.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                _loadState.value = try {
                    UiState.Success(bottleRepository.findOne(bottleId!!))
                } catch (t: Throwable) {
                    UiState.Error(t.toUserMessage())
                }
            }
        }
        loadUnits()
    }

    fun loadUnits() {
        viewModelScope.launch {
            _unitsState.value = try {
                UiState.Success(cellarRepository.listUnits())
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    /**
     * Searches across every cellar unit the household has (see
     * CellarRepository.suggestLocationAcrossUnits) rather than assuming a
     * single one -- a unit dedicated to a color (CellarUnitDto.preferredColor)
     * is strongly favored/avoided automatically, so the user never has to
     * pick a casier by hand just to add a bottle.
     */
    fun suggestLocations(
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
        quantity: Int? = null,
    ) {
        viewModelScope.launch {
            _suggestions.value = UiState.Loading
            val units = (_unitsState.value as? UiState.Success)?.data
                ?: try {
                    cellarRepository.listUnits().also { _unitsState.value = UiState.Success(it) }
                } catch (t: Throwable) {
                    _suggestions.value = UiState.Error(t.toUserMessage())
                    return@launch
                }
            if (units.isEmpty()) {
                _suggestions.value = UiState.Error("Crée d'abord un casier depuis l'onglet Cave.")
                return@launch
            }
            _suggestions.value = try {
                UiState.Success(
                    cellarRepository.suggestLocationAcrossUnits(
                        color = color,
                        region = region,
                        drinkFromYear = drinkFromYear,
                        drinkUntilYear = drinkUntilYear,
                        quantity = quantity,
                    ),
                )
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = null
    }

    fun resetSubmitState() {
        _submitState.value = null
    }

    fun submit(request: CreateBottleRequest) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = try {
                if (isEditing) {
                    bottleRepository.update(bottleId!!, request)
                } else {
                    // Splits across consecutive casiers when quantity > 1 --
                    // see BottleRepository.createExpandingLocations doc comment.
                    bottleRepository.createExpandingLocations(request)
                }
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }
}
