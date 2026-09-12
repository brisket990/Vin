package com.xothiques.vin.ui.pairing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import com.xothiques.vin.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(viewModel: PairingViewModel = hiltViewModel()) {
    val historyState by viewModel.historyState.collectAsState()
    val suggestState by viewModel.suggestState.collectAsState()
    var dish by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Accords mets-vin") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Décris ton plat, on te propose une bouteille de ta cave qui devrait bien s'accorder.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                OutlinedTextField(
                    value = dish,
                    onValueChange = { dish = it },
                    label = { Text("Plat (ex: magret de canard aux figues)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { viewModel.suggest(dish.trim()) },
                    enabled = dish.isNotBlank() && suggestState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (suggestState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    } else {
                        Text("Suggérer un accord")
                    }
                }
            }
            when (val state = suggestState) {
                is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                is UiState.Success -> item { PairingResultCard(state.data, highlighted = true) }
                else -> Unit
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { Text("Historique", style = MaterialTheme.typography.titleMedium) }
            when (val state = historyState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { Text(state.message, color = MaterialTheme.colorScheme.error) }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item { Text("Aucune suggestion pour l'instant.", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        items(state.data, key = { it.id }) { suggestion ->
                            PairingResultCard(suggestion, highlighted = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingResultCard(suggestion: PairingSuggestionDto, highlighted: Boolean) {
    var rawExpanded by remember { mutableStateOf(false) }
    Card(
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(suggestion.dishDescription, style = MaterialTheme.typography.titleSmall)
            if (suggestion.suggestedBottles.isEmpty()) {
                Text("Aucune bouteille correspondante trouvée en cave.", style = MaterialTheme.typography.bodySmall)
            } else {
                suggestion.suggestedBottles.forEach { bottle ->
                    Text(
                        "• ${bottle.name}" + (bottle.vintage?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                if (rawExpanded) suggestion.rawResponse else "Voir la réponse complète de l'IA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { rawExpanded = !rawExpanded },
            )
        }
    }
}
