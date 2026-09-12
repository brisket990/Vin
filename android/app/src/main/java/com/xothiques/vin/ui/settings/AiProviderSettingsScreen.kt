package com.xothiques.vin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.AiProviderConfigDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinHeader
import com.xothiques.vin.ui.common.VinIconBadge

private val PROVIDER_OPTIONS = listOf("anthropic" to "Anthropic (Claude)", "openai" to "OpenAI (GPT)", "google" to "Google (Gemini)")
private val USAGE_OPTIONS = listOf(
    "both" to "Reconnaissance + accords",
    "recognition" to "Reconnaissance uniquement",
    "pairing" to "Accords uniquement",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiProviderSettingsViewModel = hiltViewModel(),
) {
    val listState by viewModel.listState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        if (submitState is UiState.Success) {
            viewModel.resetSubmitState()
            showAddDialog = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un fournisseur")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(title = "Fournisseurs IA", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = listState) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::load)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Text(
                                    "Aucun fournisseur IA configuré. Ajoute ta clé API Claude, GPT ou Gemini pour activer la reconnaissance d'étiquette et les accords mets-vin.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.data, key = { it.id }) { config ->
                                    AiProviderCard(config = config, onDelete = { viewModel.remove(config.id) })
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAiProviderDialog(
                submitState = submitState,
                onDismiss = { showAddDialog = false },
                onConfirm = viewModel::upsert,
            )
        }
    }
}

@Composable
private fun AiProviderCard(config: AiProviderConfigDto, onDelete: () -> Unit) {
    Card(shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                VinIconBadge(icon = Icons.Filled.SmartToy)
                Column {
                    Text(
                        PROVIDER_OPTIONS.firstOrNull { it.first == config.provider }?.second ?: config.provider,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Modèle : ${config.model}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Clé : ${config.keyHint}" + if (config.isDefault) " · par défaut" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        USAGE_OPTIONS.firstOrNull { it.first == config.usage }?.second ?: config.usage,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAiProviderDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (provider: String, apiKey: String, model: String?, usage: String, isDefault: Boolean) -> Unit,
) {
    var provider by remember { mutableStateOf(PROVIDER_OPTIONS.first().first) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var usage by remember { mutableStateOf(USAGE_OPTIONS.first().first) }
    var usageMenuExpanded by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un fournisseur IA") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = providerMenuExpanded,
                    onExpandedChange = { providerMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = PROVIDER_OPTIONS.first { it.first == provider }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fournisseur") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false },
                    ) {
                        PROVIDER_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { provider = value; providerMenuExpanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Clé API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modèle (optionnel, ex: claude-sonnet-5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = usageMenuExpanded,
                    onExpandedChange = { usageMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = USAGE_OPTIONS.first { it.first == usage }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Utilisation") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = usageMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = usageMenuExpanded,
                        onDismissRequest = { usageMenuExpanded = false },
                    ) {
                        USAGE_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { usage = value; usageMenuExpanded = false },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Fournisseur par défaut", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }
                if (submitState is UiState.Error) {
                    Text(submitState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(provider, apiKey.trim(), model.trim().ifBlank { null }, usage, isDefault) },
                enabled = apiKey.isNotBlank() && submitState !is UiState.Loading,
            ) {
                if (submitState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Enregistrer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
