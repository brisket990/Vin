package com.xothiques.vin.ui.bottle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.local.SessionManager
import com.xothiques.vin.data.remote.dto.BottleDto
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
import kotlinx.coroutines.flow.first
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
    private val sessionManager: SessionManager,
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
            val siteId = try {
                resolveActiveSiteId()
            } catch (t: Throwable) {
                _suggestions.value = UiState.Error(t.toUserMessage())
                return@launch
            }
            if (siteId == null) {
                _suggestions.value = UiState.Error(
                    "Choisis d'abord une cave dans l'onglet Cave -- la suggestion se limite à la cave active.",
                )
                return@launch
            }
            val hasUnits = try {
                cellarRepository.listUnits().any { it.siteId == siteId }
            } catch (t: Throwable) {
                _suggestions.value = UiState.Error(t.toUserMessage())
                return@launch
            }
            if (!hasUnits) {
                _suggestions.value = UiState.Error("Crée d'abord un casier dans cette cave depuis l'onglet Cave.")
                return@launch
            }
            _suggestions.value = try {
                UiState.Success(
                    cellarRepository.suggestLocationAcrossUnits(
                        siteId = siteId,
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

    /** Falls back to the household's only cave when none has been explicitly
     *  selected yet (e.g. no site is stored on this device/session) so the
     *  user isn't forced through the Cave tab just to get a suggestion.
     *  Returns null when there's no cave yet, or when there are several and
     *  none is marked active -- the caller can't guess which one to use. */
    private suspend fun resolveActiveSiteId(): String? {
        sessionManager.activeSiteId.first()?.let { return it }
        val sites = cellarRepository.listSites()
        return sites.singleOrNull()?.id
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
