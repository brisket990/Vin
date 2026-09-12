package com.xothiques.vin.ui.scan

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.ScanResultDto
import com.xothiques.vin.data.remote.resolvePhotoUrl
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.io.File
import kotlin.coroutines.resume

private val SCAN_COLOR_OPTIONS = listOf(
    "red" to "Rouge",
    "white" to "Blanc",
    "rose" to "Rosé",
    "sparkling" to "Effervescent",
    "sweet" to "Liquoreux",
    "fortified" to "Muté / fortifié",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val scanState by viewModel.scanState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner une étiquette") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = scanState) {
                null -> CameraCaptureView(onCaptured = viewModel::scan)
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::retake, modifier = Modifier.padding(top = 12.dp)) {
                            Text("Reprendre une photo")
                        }
                    }
                }
                is UiState.Success -> ScanResultReview(
                    result = state.data,
                    saveState = saveState,
                    preselectedLocationId = viewModel.preselectedLocationId,
                    onRetake = viewModel::retake,
                    onSave = { request -> viewModel.saveBottle(state.data.id, request) },
                )
            }
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraCaptureView(onCaptured: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "L'accès à l'appareil photo est nécessaire pour scanner une étiquette.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Autoriser l'appareil photo") }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(previewView) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        FloatingActionButton(
            onClick = {
                val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onCaptured(photoFile)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(
                                context,
                                "Échec de la capture : ${exception.message}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        ) {
            Icon(Icons.Filled.Camera, contentDescription = "Prendre la photo")
        }
    }
}

private data class ParsedScanFields(
    val name: String,
    val producer: String,
    val region: String,
    val appellation: String,
    val grapes: String,
    val vintage: String,
    val color: String,
    val confidence: String?,
)

/**
 * The backend's recognition prompt (see RECOGNITION_SYSTEM_PROMPT server-side)
 * always returns this exact shape: name/producer/region/appellation (string
 * or null), grapeVarieties (string[] or null), vintage (number or null),
 * color (enum or null), confidence, notes.
 */
private fun parseStructuredFields(json: JsonObject): ParsedScanFields {
    fun str(key: String): String = (json[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    val grapes = (json["grapeVarieties"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?.joinToString(", ")
        .orEmpty()
    val vintage = (json["vintage"] as? JsonPrimitive)?.let { it.intOrNull?.toString() ?: it.contentOrNull }.orEmpty()
    val color = (json["color"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    val confidence = (json["confidence"] as? JsonPrimitive)?.contentOrNull
    return ParsedScanFields(
        name = str("name"),
        producer = str("producer"),
        region = str("region"),
        appellation = str("appellation"),
        grapes = grapes,
        vintage = vintage,
        color = color,
        confidence = confidence,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultReview(
    result: ScanResultDto,
    saveState: UiState<Unit>?,
    preselectedLocationId: String?,
    onRetake: () -> Unit,
    onSave: (CreateBottleRequest) -> Unit,
) {
    val parsed = remember(result.id) { parseStructuredFields(result.structuredFields) }
    var name by remember(result.id) { mutableStateOf(parsed.name) }
    var producer by remember(result.id) { mutableStateOf(parsed.producer) }
    var region by remember(result.id) { mutableStateOf(parsed.region) }
    var appellation by remember(result.id) { mutableStateOf(parsed.appellation) }
    var grapes by remember(result.id) { mutableStateOf(parsed.grapes) }
    var vintage by remember(result.id) { mutableStateOf(parsed.vintage) }
    var color by remember(result.id) { mutableStateOf(parsed.color.ifBlank { "red" }) }
    var colorMenuExpanded by remember { mutableStateOf(false) }
    var rawExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AsyncImage(
                model = resolvePhotoUrl(result.photoUrl),
                contentDescription = "Photo de l'étiquette",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        if (!parsed.confidence.isNullOrBlank()) {
            item {
                Text(
                    "Confiance de l'IA : ${parsed.confidence}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = producer,
                onValueChange = { producer = it },
                label = { Text("Producteur") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ExposedDropdownMenuBox(
                expanded = colorMenuExpanded,
                onExpandedChange = { colorMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = SCAN_COLOR_OPTIONS.firstOrNull { it.first == color }?.second ?: color,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Couleur") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = colorMenuExpanded,
                    onDismissRequest = { colorMenuExpanded = false },
                ) {
                    SCAN_COLOR_OPTIONS.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { color = value; colorMenuExpanded = false },
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Région") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = appellation,
                    onValueChange = { appellation = it },
                    label = { Text("Appellation") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            OutlinedTextField(
                value = grapes,
                onValueChange = { grapes = it },
                label = { Text("Cépages (séparés par des virgules)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = vintage,
                onValueChange = { vintage = it.filter(Char::isDigit) },
                label = { Text("Millésime") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                if (rawExpanded) result.rawResponse else "Voir la réponse complète de l'IA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { rawExpanded = !rawExpanded },
            )
        }
        if (saveState is UiState.Error) {
            item { Text(saveState.message, color = MaterialTheme.colorScheme.error) }
        }
        item {
            val isValid = name.isNotBlank()
            Button(
                onClick = {
                    onSave(
                        CreateBottleRequest(
                            name = name.trim(),
                            producer = producer.trim().ifBlank { null },
                            region = region.trim().ifBlank { null },
                            appellation = appellation.trim().ifBlank { null },
                            grapeVarieties = grapes.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                .ifEmpty { null },
                            vintage = vintage.toIntOrNull(),
                            color = color,
                            quantity = 1,
                            locationId = preselectedLocationId,
                            labelPhotoUrl = result.photoUrl,
                        ),
                    )
                },
                enabled = isValid && saveState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saveState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Ajouter cette bouteille à la cave")
                }
            }
        }
        item {
            TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
                Text("Reprendre une photo")
            }
        }
    }
}
