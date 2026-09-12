package com.xothiques.vin.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.WishlistItemDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinHeader

private val COLOR_OPTIONS = listOf(
    "red" to "Rouge",
    "white" to "Blanc",
    "rose" to "Rosé",
    "sparkling" to "Effervescent",
    "sweet" to "Liquoreux",
    "fortified" to "Muté / fortifié",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: WishlistViewModel = hiltViewModel()) {
    val listState by viewModel.listState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val convertState by viewModel.convertState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var convertTarget by remember { mutableStateOf<WishlistItemDto?>(null) }

    LaunchedEffect(submitState) {
        if (submitState is UiState.Success) {
            viewModel.resetSubmitState()
            showAddDialog = false
        }
    }
    LaunchedEffect(convertState) {
        if (convertState is UiState.Success) {
            viewModel.resetConvertState()
            convertTarget = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter à la liste d'envies")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(title = "Liste d'envies", subtitle = "Les bouteilles que tu voudrais acquérir")
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = listState) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::load)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Text(
                                    "Ta liste d'envies est vide. Ajoute les bouteilles que tu voudrais acquérir.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.data, key = { it.id }) { item ->
                                    WishlistCard(
                                        item = item,
                                        onDelete = { viewModel.remove(item.id) },
                                        onConvert = { convertTarget = item },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddWishlistDialog(
                submitState = submitState,
                onDismiss = { showAddDialog = false },
                onConfirm = viewModel::create,
            )
        }

        convertTarget?.let { item ->
            ConvertWishlistDialog(
                item = item,
                convertState = convertState,
                onDismiss = { convertTarget = null },
                onConfirm = { color -> viewModel.convertToBottle(item.id, color) },
            )
        }
    }
}

@Composable
private fun WishlistCard(item: WishlistItemDto, onDelete: () -> Unit, onConvert: () -> Unit) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                }
            }
            item.region?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            item.targetPriceCents?.let {
                Text("Prix cible : %.2f €".format(it / 100.0), style = MaterialTheme.typography.bodySmall)
            }
            item.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onConvert) { Text("J'ai acquis cette bouteille") }
        }
    }
}

@Composable
private fun AddWishlistDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, region: String?, notes: String?, targetPriceCents: Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var targetPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à la liste d'envies") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Région") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { targetPrice = it },
                    label = { Text("Prix cible (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (submitState is UiState.Error) {
                    Text(submitState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim(),
                        region.trim().ifBlank { null },
                        notes.trim().ifBlank { null },
                        targetPrice.replace(",", ".").toDoubleOrNull()?.let { (it * 100).toInt() },
                    )
                },
                enabled = name.isNotBlank() && submitState !is UiState.Loading,
            ) {
                if (submitState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Ajouter")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertWishlistDialog(
    item: WishlistItemDto,
    convertState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (color: String) -> Unit,
) {
    var color by remember { mutableStateOf(COLOR_OPTIONS.first().first) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter \"${item.name}\" à la cave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "La bouteille sera créée dans ta cave (quantité 1, sans emplacement) ; tu pourras compléter les détails et lui trouver une place ensuite.",
                    style = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = COLOR_OPTIONS.first { it.first == color }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Couleur") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        COLOR_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { color = value; expanded = false },
                            )
                        }
                    }
                }
                if (convertState is UiState.Error) {
                    Text(convertState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(color) },
                enabled = convertState !is UiState.Loading,
            ) {
                if (convertState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Ajouter à la cave")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
