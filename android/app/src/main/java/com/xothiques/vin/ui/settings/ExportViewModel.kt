package com.xothiques.vin.ui.settings

import android.content.Context
import com.xothiques.vin.data.repository.ExportRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _exportState = MutableStateFlow<UiState<File>?>(null)
    val exportState: StateFlow<UiState<File>?> = _exportState.asStateFlow()

    private fun exportDir(): File = File(appContext.cacheDir, "exports").apply { mkdirs() }

    fun exportCsv() {
        viewModelScope.launch {
            _exportState.value = UiState.Loading
            _exportState.value = try {
                UiState.Success(exportRepository.downloadCsv(File(exportDir(), "cave.csv")))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            _exportState.value = UiState.Loading
            _exportState.value = try {
                UiState.Success(exportRepository.downloadPdf(File(exportDir(), "cave.pdf")))
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    fun resetExportState() {
        _exportState.value = null
    }
}
