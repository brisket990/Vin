package com.xothiques.vin.ui.bottle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.CreateBottleRequest
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.theme.wineColorFor

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
fun BottleFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: BottleFormViewModel = hiltViewModel(),
) {
    val loadState by viewModel.loadState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    LaunchedEffect(submitState) {
        if (submitState is UiState.Success) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Modifier la bouteille" else "Nouvelle bouteille") },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = loadState) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> FullScreenError(state.message)
                else -> BottleFormContent(
                    initial = (loadState as? UiState.Success)?.data,
                    preselectedLocationId = viewModel.preselectedLocationId,
                    submitState = submitState,
                    suggestions = suggestions,
                    onRequestSuggestions = viewModel::suggestLocations,
                    onClearSuggestions = viewModel::clearSuggestions,
                    onSubmit = viewModel::submit,
                    onBack = onBack,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottleFormContent(
    initial: BottleDto?,
    preselectedLocationId: String?,
    submitState: UiState<Unit>?,
    suggestions: UiState<List<SuggestedLocationDto>>?,
    onRequestSuggestions: (color: String, region: String?, drinkFromYear: Int?, drinkUntilYear: Int?) -> Unit,
    onClearSuggestions: () -> Unit,
    onSubmit: (CreateBottleRequest) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var producer by remember { mutableStateOf(initial?.producer.orEmpty()) }
    var region by remember { mutableStateOf(initial?.region.orEmpty()) }
    var appellation by remember { mutableStateOf(initial?.appellation.orEmpty()) }
    var grapes by remember { mutableStateOf(initial?.grapeVarieties?.joinToString(", ").orEmpty()) }
    var vintage by remember { mutableStateOf(initial?.vintage?.toString().orEmpty()) }
    var color by remember { mutableStateOf(initial?.color ?: "red") }
    var quantity by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var priceEuros by remember {
        mutableStateOf(initial?.purchasePriceCents?.let { "%.2f".format(it / 100.0) }.orEmpty())
    }
    var drinkFromYear by remember { mutableStateOf(initial?.drinkFromYear?.toString().orEmpty()) }
    var drinkUntilYear by remember { mutableStateOf(initial?.drinkUntilYear?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var locationId by remember { mutableStateOf(initial?.locationId ?: preselectedLocationId) }
    var colorMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
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
                    label = { Text("Producteur / domaine") },
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
                        value = COLOR_OPTIONS.first { it.first == color }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Couleur *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = colorMenuExpanded,
                        onDismissRequest = { colorMenuExpanded = false },
                    ) {
                        COLOR_OPTIONS.forEach { (value, labelFr) ->
                            DropdownMenuItem(
                                text = { Text(labelFr) },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vintage,
                        onValueChange = { vintage = it.filter(Char::isDigit) },
                        label = { Text("Millésime") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit) },
                        label = { Text("Quantité *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = priceEuros,
                    onValueChange = { priceEuros = it },
                    label = { Text("Prix d'achat (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = drinkFromYear,
                        onValueChange = { drinkFromYear = it.filter(Char::isDigit) },
                        label = { Text("Apogée à partir de") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = drinkUntilYear,
                        onValueChange = { drinkUntilYear = it.filter(Char::isDigit) },
                        label = { Text("Apogée jusqu'à") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LocationPicker(
                    locationId = locationId,
                    color = color,
                    region = region,
                    drinkFromYear = drinkFromYear.toIntOrNull(),
                    drinkUntilYear = drinkUntilYear.toIntOrNull(),
                    suggestions = suggestions,
                    onRequestSuggestions = onRequestSuggestions,
                    onPick = { locationId = it; onClearSuggestions() },
                    onClear = { locationId = null },
                )
            }
            if (submitState is UiState.Error) {
                item {
                    Text(submitState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        val isValid = name.isNotBlank() && quantity.toIntOrNull()?.let { it > 0 } == true
        Button(
            onClick = {
                onSubmit(
                    CreateBottleRequest(
                        name = name.trim(),
                        producer = producer.ifBlank { null },
                        region = region.ifBlank { null },
                        appellation = appellation.ifBlank { null },
                        grapeVarieties = grapes.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            .ifEmpty { null },
                        vintage = vintage.toIntOrNull(),
                        color = color,
                        quantity = quantity.toIntOrNull(),
                        purchasePriceCents = priceEuros.replace(",", ".").toDoubleOrNull()
                            ?.let { (it * 100).toInt() },
                        drinkFromYear = drinkFromYear.toIntOrNull(),
                        drinkUntilYear = drinkUntilYear.toIntOrNull(),
                        locationId = locationId,
                        notes = notes.ifBlank { null },
                    ),
                )
            },
            enabled = isValid && submitState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (submitState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
            } else {
                Text("Enregistrer")
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Annuler")
        }
    }
}

@Composable
private fun LocationPicker(
    locationId: String?,
    color: String,
    region: String,
    drinkFromYear: Int?,
    drinkUntilYear: Int?,
    suggestions: UiState<List<SuggestedLocationDto>>?,
    onRequestSuggestions: (color: String, region: String?, drinkFromYear: Int?, drinkUntilYear: Int?) -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Emplacement en cave", style = MaterialTheme.typography.titleSmall)
            Text(
                if (locationId != null) "Casier sélectionné." else "Aucun casier choisi pour l'instant.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onRequestSuggestions(color, region.ifBlank { null }, drinkFromYear, drinkUntilYear)
                    },
                ) { Text("Suggérer un emplacement") }
                if (locationId != null) {
                    TextButton(onClick = onClear) { Text("Retirer") }
                }
            }
            when (suggestions) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                is UiState.Error -> Text(suggestions.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (suggestions.data.isEmpty()) {
                        Text("Aucun casier libre trouvé.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestions.data, key = { it.locationId }) { suggestion ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            wineColorFor(color).copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable { onPick(suggestion.locationId) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(suggestion.label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}
