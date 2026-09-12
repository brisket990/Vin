package com.xothiques.vin.ui.cellar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.xothiques.vin.data.remote.dto.CellarLocationDto
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.theme.wineColorFor

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CellarGridScreen(
    onOpenBottle: (String) -> Unit,
    onAddBottle: (locationId: String?) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CellarViewModel = hiltViewModel(),
) {
    val unitsState by viewModel.unitsState.collectAsState()
    val selectedUnitId by viewModel.selectedUnitId.collectAsState()
    val createUnitState by viewModel.createUnitState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(createUnitState) {
        if (createUnitState is UiState.Success) {
            viewModel.resetCreateUnitState()
            showCreateDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ma cave") },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("Réglages") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddBottle(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une bouteille")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = unitsState) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::loadUnits)
                is UiState.Success -> {
                    val units = state.data
                    if (units.isEmpty()) {
                        EmptyCellarPrompt(onCreate = { showCreateDialog = true })
                    } else {
                        val selectedUnit = units.firstOrNull { it.id == selectedUnitId } ?: units.first()
                        CellarUnitContent(
                            units = units,
                            selectedUnit = selectedUnit,
                            onSelectUnit = viewModel::selectUnit,
                            onOpenBottle = onOpenBottle,
                            onAddBottle = onAddBottle,
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateUnitDialog(
                submitState = createUnitState,
                onDismiss = { showCreateDialog = false },
                onConfirm = viewModel::createUnit,
            )
        }
    }
}

@Composable
private fun EmptyCellarPrompt(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Tu n'as pas encore de cave configurée.",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Crée une première grille de casiers (par exemple 6 rangées x 8 colonnes) pour commencer à ranger tes bouteilles.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onCreate) { Text("Créer ma cave") }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateUnitDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, rowCount: Int, columnCount: Int) -> Unit,
) {
    var name by remember { mutableStateOf("Cave principale") }
    var rows by remember { mutableStateOf("6") }
    var columns by remember { mutableStateOf("8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle cave") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rows,
                    onValueChange = { rows = it.filter(Char::isDigit) },
                    label = { Text("Nombre de rangées") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = columns,
                    onValueChange = { columns = it.filter(Char::isDigit) },
                    label = { Text("Nombre de colonnes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (submitState is UiState.Error) {
                    Text(
                        submitState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val r = rows.toIntOrNull() ?: 0
                    val c = columns.toIntOrNull() ?: 0
                    if (name.isNotBlank() && r > 0 && c > 0) onConfirm(name.trim(), r, c)
                },
                enabled = submitState !is UiState.Loading,
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CellarUnitContent(
    units: List<CellarUnitDto>,
    selectedUnit: CellarUnitDto,
    onSelectUnit: (String) -> Unit,
    onOpenBottle: (String) -> Unit,
    onAddBottle: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (units.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                OutlinedTextField(
                    value = selectedUnit.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cave") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    units.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.name) },
                            onClick = {
                                onSelectUnit(unit.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        Text(
            "${selectedUnit.rowCount} rangées x ${selectedUnit.columnCount} colonnes — appuie sur un casier occupé pour voir la bouteille, ou sur un casier vide pour y ranger une nouvelle bouteille.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(selectedUnit.columnCount),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                selectedUnit.locations.sortedWith(compareBy({ it.row }, { it.column })),
                key = { it.id },
            ) { location ->
                CellarCell(
                    location = location,
                    onClick = {
                        val bottle = location.bottle
                        if (bottle != null) onOpenBottle(bottle.id) else onAddBottle(location.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun CellarCell(location: CellarLocationDto, onClick: () -> Unit) {
    val bottle = location.bottle
    val backgroundColor = if (bottle != null) wineColorFor(bottle.color) else Color.LightGray.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = location.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (bottle != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
