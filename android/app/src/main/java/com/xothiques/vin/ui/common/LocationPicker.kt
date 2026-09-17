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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.ui.theme.wineColorFor

/**
 * Shared "pick a cellar location" card, used by both the bottle form (create
 * from scratch / edit) and the scan confirmation screen. Asks the backend to
 * suggest a free casier (grouped by wine color and, for several bottles at
 * once, right-sized to a run of contiguous free slots -- see
 * CellarService.suggestLocations server-side) instead of making the user
 * hunt through the grid manually.
 *
 * The top suggestion is applied automatically as soon as color/quantity are
 * known (no button press needed), but only while nothing more specific is
 * already chosen -- a location the user picked by hand (a different chip, a
 * grid tap before scanning) is never silently overridden, and "Suggérer un
 * emplacement" stays available to re-roll or browse alternatives.
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
    quantity: Int,
    suggestions: UiState<List<SuggestedLocationDto>>?,
    onRequestSuggestions: (
        color: String,
        region: String?,
        drinkFromYear: Int?,
        drinkUntilYear: Int?,
        quantity: Int?,
    ) -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    // Tracks the locationId this picker itself last auto-applied, so a
    // later quantity/color change can refine that choice, but a location
    // the user picked by hand (different from this value) is left alone.
    var autoAppliedLocationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(color, quantity) {
        onRequestSuggestions(color, region.ifBlank { null }, drinkFromYear, drinkUntilYear, quantity)
    }
    LaunchedEffect(suggestions) {
        val top = (suggestions as? UiState.Success)?.data?.firstOrNull() ?: return@LaunchedEffect
        if (locationId == null || locationId == autoAppliedLocationId) {
            onPick(top.locationId)
            autoAppliedLocationId = top.locationId
        }
    }

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
                        onRequestSuggestions(color, region.ifBlank { null }, drinkFromYear, drinkUntilYear, quantity)
                    },
                ) { Text("Voir d'autres emplacements") }
                if (locationId != null) {
                    TextButton(onClick = { onClear(); autoAppliedLocationId = null }) { Text("Retirer") }
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
                                            wineColorFor(color).copy(
                                                alpha = if (suggestion.locationId == locationId) 0.35f else 0.15f,
                                            ),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable {
                                            onPick(suggestion.locationId)
                                            autoAppliedLocationId = suggestion.locationId
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        if (suggestion.unitName != null) {
                                            "${suggestion.unitName} · ${suggestion.label}"
                                        } else {
                                            suggestion.label
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                    )
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
