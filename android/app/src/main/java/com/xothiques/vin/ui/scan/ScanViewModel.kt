package com.xothiques.vin.ui.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.ScanResultDto
import com.xothiques.vin.data.repository.BottleRepository
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val preselectedLocationId: String? = savedStateHandle["locationId"]

    private val _scanState = MutableStateFlow<UiState<ScanResultDto>?>(null)
    val scanState: StateFlow<UiState<ScanResultDto>?> = _scanState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>?>(null)
    val saveState: StateFlow<UiState<Unit>?> = _saveState.asStateFlow()

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
    }

    fun saveBottle(scanId: String, request: CreateBottleRequest) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = try {
                val bottle = bottleRepository.create(request)
                scanRepository.linkBottle(scanId, bottle.id)
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
