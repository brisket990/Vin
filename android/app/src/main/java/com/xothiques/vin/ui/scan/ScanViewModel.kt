package com.xothiques.vin.ui.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.ScanResultDto
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.data.repository.BottleRepository
import com.xothiques.vin.data.repository.CellarRepository
import com.xothiques.vin.data.repository.ScanRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val bottleRepository: BottleRepository,
    private val cellarRepository: CellarRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val preselectedLocationId: String? = savedStateHandle["locationId"]

    private val _scanState = MutableStateFlow<UiState<ScanResultDto>?>(null)
    val scanState: StateFlow<UiState<ScanResultDto>?> = _scanState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>?>(null)
    val saveState: StateFlow<UiState<Unit>?> = _saveState.asStateFlow()

    private val _suggestions = MutableStateFlow<UiState<List<SuggestedLocationDto>>?>(null)
    val suggestions: StateFlow<UiState<List<SuggestedLocationDto>>?> = _suggestions.asStateFlow()

    /** Mirrors BottleFormViewModel.suggestLocations -- see its doc comment. */
    fun suggestLocations(
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
        quantity: Int? = null,
    ) {
        viewModelScope.launch {
            _suggestions.value = UiState.Loading
            val unit = try {
                cellarRepository.listUnits().firstOrNull()
            } catch (t: Throwable) {
                _suggestions.value = UiState.Error(t.toUserMessage())
                return@launch
            }
            if (unit == null) {
                _suggestions.value = UiState.Error("Crée d'abord une cave depuis l'onglet Cave.")
                return@launch
            }
            _suggestions.value = try {
                UiState.Success(
                    cellarRepository.suggestLocation(
                        unit.id, color, region, drinkFromYear, drinkUntilYear, quantity,
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

    fun scan(photoFile: File) {
        viewModelScope.launch {
            _scanState.value = UiState.Loading
            _scanState.value = try {
                UiState.Success(scanRepository.scan(photoFile))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    /** Discards the current scan result/photo and returns to the camera. */
    fun retake() {
        _scanState.value = null
        _saveState.value = null
        _suggestions.value = null
    }

    fun saveBottle(scanId: String, request: CreateBottleRequest) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = try {
                // Splits across consecutive casiers when quantity > 1 -- see
                // BottleRepository.createExpandingLocations doc comment.
                val bottles = bottleRepository.createExpandingLocations(request)
                scanRepository.linkBottle(scanId, bottles.first().id)
                UiState.Success(Unit)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }
}
