package com.xothiques.vin.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xothiques.vin.data.remote.dto.ImportCsvResultDto
import com.xothiques.vin.data.repository.ImportRepository
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importRepository: ImportRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _importState = MutableStateFlow<UiState<ImportCsvResultDto>?>(null)
    val importState: StateFlow<UiState<ImportCsvResultDto>?> = _importState.asStateFlow()

    /** A picked file is a content:// Uri -- copy it into our cache dir first
     *  (same pattern as the scan screen's gallery picker) since OkHttp's
     *  multipart body needs a stable file it can read (and re-read on a
     *  retry) rather than a one-shot content stream. */
    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _importState.value = UiState.Loading
            _importState.value = try {
                val file = copyUriToCacheFile(uri)
                    ?: throw IllegalStateException("Impossible de lire ce fichier.")
                val result = importRepository.importCsv(file)
                file.delete()
                UiState.Success(result)
            } catch (t: Throwable) {
                UiState.Error(t.toUserMessage())
            }
        }
    }

    private fun copyUriToCacheFile(uri: Uri): File? = try {
        val file = File(appContext.cacheDir, "import_${System.currentTimeMillis()}.csv")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        if (file.exists() && file.length() > 0) file else null
    } catch (e: Exception) {
        null
    }

    fun resetImportState() {
        _importState.value = null
    }
}
