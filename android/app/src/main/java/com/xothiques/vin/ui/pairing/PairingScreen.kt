package com.xothiques.vin.ui.pairing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.PairingCellarSuggestionDto
import com.xothiques.vin.data.remote.dto.PairingShoppingSuggestionDto
import com.xothiques.vin.data.remote.dto.PairingSuggestionDto
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(viewModel: PairingViewModel = hiltViewModel()) {
    val historyState by viewModel.historyState.collectAsState()
    val suggestState by viewModel.suggestState.collectAsState()
    var dish by remember { mutableStateOf("") }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                VinHeader(
                    title = "Accords mets-vin",
                    subtitle = "Décris un plat : on te propose 3 bouteilles de ta cave et 3 idées à acheter, chacune notée",
                )
            }
            item {
                OutlinedTextField(
                    value = dish,
                    onValueChange = { dish = it },
                    label = { Text("Plat (ex: magret de canard aux figues)") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            item {
                Button(
                    onClick = { viewModel.suggest(dish.trim()) },
                    enabled = dish.isNotBlank() && suggestState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    if (suggestState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    } else {
                        Text("Suggérer un accord")
                    }
                }
            }
            when (val state = suggestState) {
                is UiState.Error -> item {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                is UiState.Success -> item {
                    PairingResultCard(state.data, highlighted = true, modifier = Modifier.padding(horizontal = 16.dp))
                }
                else -> Unit
            }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item {
                Text(
                    "Historique",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when (val state = historyState) {
                is UiState.Loading -> item { CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp)) }
                is UiState.Error -> item {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            Text(
                                "Aucune suggestion pour l'instant.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { suggestion ->
                            PairingResultCard(suggestion, highlighted = false, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingResultCard(suggestion: PairingSuggestionDto, highlighted: Boolean, modifier: Modifier = Modifier) {
    var rawExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(suggestion.dishDescription, style = MaterialTheme.typography.titleSmall)

            Text(
                "Dans ta cave",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (suggestion.cellarSuggestions.isEmpty()) {
                Text("Aucune bouteille correspondante trouvée en cave.", style = MaterialTheme.typography.bodySmall)
            } else {
                suggestion.cellarSuggestions.forEach { cellarSuggestion ->
                    CellarSuggestionRow(cellarSuggestion)
                }
            }

            HorizontalDivider()

            Text(
                "À acheter en magasin",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (suggestion.shoppingSuggestions.isEmpty()) {
                Text("Aucune suggestion d'achat.", style = MaterialTheme.typography.bodySmall)
            } else {
                suggestion.shoppingSuggestions.forEach { shoppingSuggestion ->
                    ShoppingSuggestionRow(shoppingSuggestion)
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

@Composable
private fun CellarSuggestionRow(suggestion: PairingCellarSuggestionDto) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "• ${suggestion.bottle.name}" + (suggestion.bottle.vintage?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (suggestion.score != null) {
                ScoreBadge(suggestion.score)
            }
        }
        if (suggestion.reasoning.isNotBlank()) {
            Text(
                suggestion.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ShoppingSuggestionRow(suggestion: PairingShoppingSuggestionDto) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("• ${suggestion.name}", style = MaterialTheme.typography.bodyMedium)
            if (suggestion.score != null) {
                ScoreBadge(suggestion.score)
            }
        }
        val details = listOfNotNull(
            suggestion.region,
            suggestion.grapeVarieties?.takeIf { it.isNotEmpty() }?.joinToString(", "),
        ).joinToString(" • ")
        if (details.isNotBlank()) {
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        if (suggestion.reasoning.isNotBlank()) {
            Text(
                suggestion.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ScoreBadge(score: Double) {
    val formatted = if (score == score.toInt().toDouble()) {
        score.toInt().toString()
    } else {
        String.format("%.1f", score)
    }
    Text(
        "$formatted/10",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
