package com.xothiques.vin.ui.cellar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.CellarLocationDto
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinActionCard
import com.xothiques.vin.ui.common.VinHeader
import com.xothiques.vin.ui.common.VinListRow
import com.xothiques.vin.ui.theme.wineColorFor

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CellarGridScreen(
    onOpenBottle: (String) -> Unit,
    onAddBottle: (locationId: String?) -> Unit,
    onScan: () -> Unit,
    onOpenBottleList: () -> Unit,
    viewModel: CellarViewModel = hiltViewModel(),
) {
    val unitsState by viewModel.unitsState.collectAsState()
    val selectedUnitId by viewModel.selectedUnitId.collectAsState()
    val createUnitState by viewModel.createUnitState.collectAsState()
    val unassignedBottlesState by viewModel.unassignedBottlesState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(createUnitState) {
        if (createUnitState is UiState.Success) {
            viewModel.resetCreateUnitState()
            showCreateDialog = false
        }
    }

    // Navigating to Scan/Ajouter des vins/Détail bouteille pushes a new
    // back-stack entry on top of this one rather than recreating it, so this
    // screen's ViewModel (and its cached lists) would otherwise go stale the
    // moment a bottle is added, edited, or (re)located. Re-fetch whenever
    // this destination comes back into view.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val units = (unitsState as? UiState.Success)?.data.orEmpty()
    val unassignedBottles = (unassignedBottlesState as? UiState.Success)?.data.orEmpty()
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId } ?: units.firstOrNull()
    val occupied = selectedUnit?.locations?.count { it.bottle != null } ?: 0
    val totalCells = selectedUnit?.locations?.size ?: 0

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(
                title = "Ma cave",
                subtitle = if (selectedUnit != null) "$occupied bouteilles rangées • $totalCells casiers" else null,
                trailing = {
                    Row {
                        IconButton(onClick = onOpenBottleList) {
                            Icon(
                                Icons.Filled.FormatListBulleted,
                                contentDescription = "Voir la liste des bouteilles",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        IconButton(onClick = onScan) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = "Scanner une étiquette",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                },
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = unitsState) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::loadUnits)
                    is UiState.Success -> {
                        if (units.isEmpty()) {
                            EmptyCellarPrompt(onCreate = { showCreateDialog = true })
                        } else if (selectedUnit != null) {
                            CellarUnitContent(
                                units = units,
                                selectedUnit = selectedUnit,
                                unassignedBottles = unassignedBottles,
                                onSelectUnit = viewModel::selectUnit,
                                onOpenBottle = onOpenBottle,
                                onAddBottle = onAddBottle,
                                onScan = onScan,
                            )
                        }
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

private val WINE_COLOR_LABELS = mapOf(
    "red" to "Vins Rouges",
    "white" to "Vins Blancs",
    "rose" to "Vins Rosés",
    "sparkling" to "Vins Effervescents",
    "sweet" to "Vins Doux",
    "fortified" to "Vins Fortifiés",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CellarUnitContent(
    units: List<CellarUnitDto>,
    selectedUnit: CellarUnitDto,
    unassignedBottles: List<BottleDto>,
    onSelectUnit: (String) -> Unit,
    onOpenBottle: (String) -> Unit,
    onAddBottle: (String?) -> Unit,
    onScan: () -> Unit,
) {
    val byColor = remember(selectedUnit) {
        selectedUnit.locations.mapNotNull { it.bottle }.groupingBy { it.color }.eachCount()
            .entries.sortedByDescending { it.value }
    }

    // Everything -- action cards, the "Ma collection" summary, unassigned
    // bottles, and the casier grid -- lives in ONE LazyVerticalGrid instead
    // of a fixed Column wrapping a separately-scrolling grid. Two nested
    // independently-scrolling containers meant only the grid portion could
    // scroll, leaving the header content stuck off-screen whenever it (plus
    // the visible rows) didn't fit the viewport. Full-width header blocks
    // use item(span = { GridItemSpan(maxLineSpan) }) so they scroll together
    // with the casier cells as one list.
    LazyVerticalGrid(
        columns = GridCells.Fixed(selectedUnit.columnCount),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VinActionCard(
                    icon = Icons.Filled.Add,
                    label = "Ajouter des vins",
                    onClick = { onAddBottle(null) },
                    modifier = Modifier.weight(1f),
                )
                VinActionCard(
                    icon = Icons.Filled.PhotoCamera,
                    label = "Scanner",
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (units.size > 1) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
        }

        if (byColor.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Ma collection",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byColor.forEach { (color, count) ->
                        VinListRow(
                            icon = Icons.Filled.WineBar,
                            title = WINE_COLOR_LABELS[color] ?: color,
                            subtitle = "$count bouteille" + if (count > 1) "s" else "",
                            badgeColor = wineColorFor(color).copy(alpha = 0.18f),
                            badgeContentColor = wineColorFor(color),
                        )
                    }
                }
            }
        }

        if (unassignedBottles.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        "Bouteilles sans emplacement",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        "Ajoutées sans choisir de casier -- elles ne s'affichent pas dans la grille. Touche-en une pour lui assigner un emplacement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    unassignedBottles.forEach { bottle ->
                        VinListRow(
                            icon = Icons.Filled.WineBar,
                            title = bottle.name,
                            subtitle = listOfNotNull(bottle.producer, bottle.vintage?.toString())
                                .joinToString(" · ")
                                .ifBlank { "Quantité : ${bottle.quantity}" },
                            badgeColor = wineColorFor(bottle.color).copy(alpha = 0.18f),
                            badgeContentColor = wineColorFor(bottle.color),
                            onClick = { onOpenBottle(bottle.id) },
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Grille de casiers — ${selectedUnit.rowCount} rangées x ${selectedUnit.columnCount} colonnes. Appuie sur un casier occupé pour voir la bouteille, ou sur un casier vide pour y ranger une nouvelle bouteille.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

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

@Composable
private fun CellarCell(location: CellarLocationDto, onClick: () -> Unit) {
    val bottle = location.bottle
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = if (bottle != null) wineColorFor(bottle.color) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(backgroundColor, shape)
            .then(
                if (bottle == null) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
                } else {
                    Modifier
                },
            )
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
