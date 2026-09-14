package com.xothiques.vin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.ui.theme.wineColorFor

/**
 * Shared "pick a cellar location" card, used by both the bottle form (create
 * from scratch / edit) and the scan confirmation screen. Lets the user ask
 * the backend to suggest a free casier (grouping by wine type / région /
 * fenêtre d'apogée) rather than hunting through the grid manually.
 *
 * Important: this is opt-in, not required. A bottle saved without a
 * locationId is still created (status "in_cellar"), but it won't appear in
 * the cellar grid (CellarGridScreen only renders bottles that occupy a
 * location) -- CellarGridScreen shows those under "Bouteilles sans
 * emplacement" instead so they're never simply lost.
 */
@Composable
fun LocationPicker(
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
    Card(shape = MaterialTheme.shapes.large) {
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
