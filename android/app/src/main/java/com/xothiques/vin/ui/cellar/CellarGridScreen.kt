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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import com.xothiques.vin.ui.theme.WINE_COLOR_LABELS
import com.xothiques.vin.ui.theme.wineColorFor

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CellarGridScreen(
    onOpenBottle: (String) -> Unit,
    onAddBottle: (locationId: String?) -> Unit,
    onScan: () -> Unit,
    onOpenBottleList: (color: String?) -> Unit,
    viewModel: CellarViewModel = hiltViewModel(),
) {
    val unitsState by viewModel.unitsState.collectAsState()
    val createUnitState by viewModel.createUnitState.collectAsState()
    val updateUnitState by viewModel.updateUnitState.collectAsState()
    val deleteUnitState by viewModel.deleteUnitState.collectAsState()
    val unassignedBottlesState by viewModel.unassignedBottlesState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<CellarUnitDto?>(null) }
    var unitPendingDelete by remember { mutableStateOf<CellarUnitDto?>(null) }

    LaunchedEffect(createUnitState) {
        if (createUnitState is UiState.Success) {
            viewModel.resetCreateUnitState()
            showCreateDialog = false
        }
    }
    LaunchedEffect(updateUnitState) {
        if (updateUnitState is UiState.Success) {
            viewModel.resetUpdateUnitState()
            editingUnit = null
        }
    }
    LaunchedEffect(deleteUnitState) {
        if (deleteUnitState is UiState.Success) {
            viewModel.resetDeleteUnitState()
            unitPendingDelete = null
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
    val occupied = units.sumOf { unit -> unit.locations.count { it.bottle != null } }
    val totalCells = units.sumOf { it.locations.size }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(
                title = "Ma cave",
                subtitle = if (units.isNotEmpty()) "$occupied bouteilles rangées • $totalCells casiers" else null,
                trailing = {
                    Row {
                        IconButton(onClick = { onOpenBottleList(null) }) {
                            Icon(
                                Icons.Filled.FormatListBulleted,
                                contentDescription = "Voir la liste des bouteilles",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        if (units.isNotEmpty()) {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Ajouter un casier",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
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
                        } else {
                            CellarUnitsContent(
                                units = units,
                                unassignedBottles = unassignedBottles,
                                onOpenBottle = onOpenBottle,
                                onAddBottle = onAddBottle,
                                onScan = onScan,
                                onEditUnit = { editingUnit = it },
                                onDeleteUnit = { unitPendingDelete = it },
                                onOpenColorList = onOpenBottleList,
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

        editingUnit?.let { unit ->
            EditUnitDialog(
                unit = unit,
                submitState = updateUnitState,
                onDismiss = {
                    editingUnit = null
                    viewModel.resetUpdateUnitState()
                },
                onConfirm = { name, preferredColor, rowCount, columnCount ->
                    viewModel.updateUnit(unit.id, name, preferredColor, rowCount, columnCount)
                },
            )
        }

        unitPendingDelete?.let { unit ->
            DeleteUnitDialog(
                unit = unit,
                submitState = deleteUnitState,
                onDismiss = {
                    unitPendingDelete = null
                    viewModel.resetDeleteUnitState()
                },
                onConfirm = { viewModel.deleteUnit(unit.id) },
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
            "Cette cave n'a pas encore de casier configuré.",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Crée une première grille de casiers (par exemple 6 rangées x 8 colonnes) pour commencer à ranger tes bouteilles. Tu pourras en ajouter d'autres ensuite (un par pièce/meuble, par exemple), chacune éventuellement dédiée à une couleur.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onCreate) { Text("Créer un casier") }
    }
}

/** null (no selection) means "Mixte" -- see CellarUnitDto.preferredColor. */
private val COLOR_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Mixte (aucune préférence)",
    "red" to "Rouges",
    "white" to "Blancs",
    "rose" to "Rosés",
    "sparkling" to "Effervescents",
    "sweet" to "Doux",
    "fortified" to "Fortifiés",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ColorPreferenceField(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = COLOR_OPTIONS.firstOrNull { it.first == selected }?.second ?: "Mixte (aucune préférence)"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Couleur principale") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            COLOR_OPTIONS.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateUnitDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, rowCount: Int, columnCount: Int, preferredColor: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf("6") }
    var columns by remember { mutableStateOf("8") }
    var preferredColor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau casier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    placeholder = { Text("Casier 1") },
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
                ColorPreferenceField(
                    selected = preferredColor,
                    onSelect = { preferredColor = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "Si tu as plusieurs casiers, en dédier un à une couleur permet à l'app de te proposer directement le bon casier quand tu ajoutes une bouteille.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
                    val finalName = name.trim().ifBlank { "Casier 1" }
                    if (r > 0 && c > 0) onConfirm(finalName, r, c, preferredColor)
                },
                enabled = submitState !is UiState.Loading,
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/** Rename a unit, change its dedicated color, and/or resize its grid.
 *  Growing adds empty slots; shrinking is refused server-side (BadRequest,
 *  surfaced below) if it would delete a location that still holds a bottle. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditUnitDialog(
    unit: CellarUnitDto,
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, preferredColor: String, rowCount: Int, columnCount: Int) -> Unit,
) {
    var name by remember(unit.id) { mutableStateOf(unit.name) }
    var preferredColor by remember(unit.id) { mutableStateOf(unit.preferredColor) }
    var rows by remember(unit.id) { mutableStateOf(unit.rowCount.toString()) }
    var columns by remember(unit.id) { mutableStateOf(unit.columnCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ce casier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorPreferenceField(
                    selected = preferredColor,
                    onSelect = { preferredColor = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = rows,
                        onValueChange = { rows = it.filter(Char::isDigit) },
                        label = { Text("Rangées") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = columns,
                        onValueChange = { columns = it.filter(Char::isDigit) },
                        label = { Text("Colonnes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Réduire une dimension supprime les derniers casiers correspondants -- refusé si l'un d'eux contient encore une bouteille.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
                    if (name.isNotBlank() && r > 0 && c > 0) {
                        // "none" clears the preference back to mixed -- a real
                        // null would be silently dropped by the app's JSON
                        // encoder, so the request always carries an explicit value.
                        onConfirm(name.trim(), preferredColor ?: "none", r, c)
                    }
                },
                enabled = submitState !is UiState.Loading,
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/** Confirmation before deleting a casier -- the backend refuses (and this
 *  dialog then shows why) while it still holds an in-cellar bottle. */
@Composable
private fun DeleteUnitDialog(
    unit: CellarUnitDto,
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer \"${unit.name}\" ?") },
        text = {
            Column {
                Text("Cette action est définitive. Les emplacements de ce casier seront supprimés.")
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
                onClick = onConfirm,
                enabled = submitState !is UiState.Loading,
            ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/**
 * Whole cellar page as ONE scrollable list: action cards, the
 * household-wide "Ma collection" summary, unassigned bottles, then every
 * cellar unit (casier/armoire) in turn -- each with its own header (name,
 * occupancy, dedicated color if any) and its own grid, one after another
 * with visual spacing between them so scrolling down moves from the first
 * unit to the second, third, etc.
 *
 * Each unit's grid is laid out as plain Rows inside this single LazyColumn
 * (not a nested LazyVerticalGrid) so the whole page shares one scroll
 * container -- two independently-scrolling containers previously meant only
 * the grid portion could scroll, leaving the header content stuck off-screen.
 */
@Composable
private fun CellarUnitsContent(
    units: List<CellarUnitDto>,
    unassignedBottles: List<BottleDto>,
    onOpenBottle: (String) -> Unit,
    onAddBottle: (String?) -> Unit,
    onScan: () -> Unit,
    onEditUnit: (CellarUnitDto) -> Unit,
    onDeleteUnit: (CellarUnitDto) -> Unit,
    onOpenColorList: (color: String?) -> Unit,
) {
    val byColor = remember(units) {
        units.flatMap { it.locations }.mapNotNull { it.bottle }
            .groupingBy { it.color }.eachCount()
            .entries.sortedByDescending { it.value }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
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

        if (byColor.isNotEmpty()) {
            item {
                Text(
                    "Ma collection",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byColor.forEach { (color, count) ->
                        VinListRow(
                            icon = Icons.Filled.WineBar,
                            title = WINE_COLOR_LABELS[color] ?: color,
                            subtitle = "$count bouteille" + if (count > 1) "s" else "",
                            badgeColor = wineColorFor(color).copy(alpha = 0.18f),
                            badgeContentColor = wineColorFor(color),
                            onClick = { onOpenColorList(color) },
                        )
                    }
                }
            }
        }

        if (unassignedBottles.isNotEmpty()) {
            item {
                Column {
                    Text(
                        "Bouteilles sans emplacement",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        "Ajoutées sans choisir de casier -- elles ne s'affichent pas dans les grilles ci-dessous. Touche-en une pour lui assigner un emplacement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            item {
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

        items(units, key = { it.id }) { unit ->
            CellarUnitSection(
                unit = unit,
                showDivider = unit.id != units.last().id,
                onOpenBottle = onOpenBottle,
                onAddBottle = onAddBottle,
                onEditUnit = { onEditUnit(unit) },
                onDeleteUnit = { onDeleteUnit(unit) },
            )
        }
    }
}

/** One casier's header (name, occupancy, dedicated color) + its full grid,
 *  laid out as plain Rows rather than a lazy grid -- see CellarUnitsContent. */
@Composable
private fun CellarUnitSection(
    unit: CellarUnitDto,
    showDivider: Boolean,
    onOpenBottle: (String) -> Unit,
    onAddBottle: (String?) -> Unit,
    onEditUnit: () -> Unit,
    onDeleteUnit: () -> Unit,
) {
    val occupied = remember(unit) { unit.locations.count { it.bottle != null } }
    val colorLabel = COLOR_OPTIONS.firstOrNull { it.first == unit.preferredColor }?.second

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(unit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$occupied / ${unit.locations.size} casiers occupés" +
                        if (unit.preferredColor != null) " • $colorLabel" else " • Mixte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onEditUnit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier ${unit.name}")
                }
                IconButton(onClick = onDeleteUnit) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer ${unit.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Text(
            "${unit.rowCount} rangées x ${unit.columnCount} colonnes. Appuie sur un casier occupé pour voir la bouteille, ou sur un casier vide pour y ranger une nouvelle bouteille.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        val rowsByNumber = remember(unit) { unit.locations.groupBy { it.row }.toSortedMap() }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rowsByNumber.forEach { (_, rowLocations) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowLocations.sortedBy { it.column }.forEach { location ->
                        CellarCell(
                            location = location,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val bottle = location.bottle
                                if (bottle != null) onOpenBottle(bottle.id) else onAddBottle(location.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun CellarCell(location: CellarLocationDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bottle = location.bottle
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = if (bottle != null) wineColorFor(bottle.color) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier
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
