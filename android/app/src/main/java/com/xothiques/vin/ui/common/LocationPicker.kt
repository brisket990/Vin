package com.xothiques.vin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.xothiques.vin.data.remote.dto.CellarUnitDto
import com.xothiques.vin.data.remote.dto.SuggestedLocationDto
import com.xothiques.vin.ui.theme.wineColorFor

/**
 * Shared "pick a cellar location" card, used by both the bottle form (create
 * from scratch / edit) and the scan confirmation screen. Two steps, in
 * order: first the casier (only shown as a choice when the household has
 * more than one), then the specific niche within it -- the user picks both
 * explicitly rather than being handed a handful of algorithm-ranked
 * suggestions mixed across casiers.
 *
 * A sensible default is still pre-selected automatically as soon as
 * color/quantity are known (see CellarService.suggestLocations
 * server-side, grouped by wine color and run-length), but only while
 * nothing more specific is already chosen -- a casier/niche the user picked
 * by hand is never silently overridden.
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
    unitsState: UiState<List<CellarUnitDto>>,
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
    var selectedUnitId by remember { mutableStateOf<String?>(null) }
    val units = (unitsState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(color, quantity) {
        onRequestSuggestions(color, region.ifBlank { null }, drinkFromYear, drinkUntilYear, quantity)
    }
    LaunchedEffect(suggestions) {
        val top = (suggestions as? UiState.Success)?.data?.firstOrNull() ?: return@LaunchedEffect
        if (locationId == null || locationId == autoAppliedLocationId) {
            onPick(top.locationId)
            autoAppliedLocationId = top.locationId
            if (top.unitId != null) selectedUnitId = top.unitId
        }
    }
    // Keeps the casier selector in sync with whichever unit currently owns
    // locationId (e.g. after the auto-suggestion above, or when editing a
    // bottle that already has a spot), and falls back to the first casier
    // once the list loads if nothing is chosen yet.
    LaunchedEffect(locationId, units) {
        val owningUnit = units.firstOrNull { unit -> unit.locations.any { it.id == locationId } }
        if (owningUnit != null) {
            selectedUnitId = owningUnit.id
        } else if (selectedUnitId == null || units.none { it.id == selectedUnitId }) {
            selectedUnitId = units.firstOrNull()?.id
        }
    }

    val selectedUnit = units.firstOrNull { it.id == selectedUnitId }
    // Free niches of the selected casier, plus the currently chosen one even
    // if it's "occupied" -- while editing a bottle, its own spot shows up as
    // occupied by itself, and it must stay pickable/visible.
    val availableLocations = remember(selectedUnit, locationId) {
        selectedUnit?.locations
            ?.filter { it.bottle == null || it.id == locationId }
            ?.sortedWith(compareBy({ it.row }, { it.column }))
            .orEmpty()
    }

    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Emplacement en cave", style = MaterialTheme.typography.titleSmall)
            Text(
                if (locationId != null) "Casier sélectionné." else "Aucun casier choisi pour l'instant.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (unitsState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            } else if (unitsState is UiState.Error) {
                Text(unitsState.message, color = MaterialTheme.colorScheme.error)
            } else if (units.isEmpty()) {
                Text(
                    "Crée d'abord un casier depuis l'onglet Cave.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                if (units.size > 1) {
                    Text("1. Choisis un casier", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(units, key = { it.id }) { unit ->
                            PickerChip(
                                label = unit.name,
                                selected = unit.id == selectedUnitId,
                                onClick = {
                                    if (unit.id != selectedUnitId) {
                                        selectedUnitId = unit.id
                                        // The previous niche belonged to the old
                                        // casier -- clear it so step 2 below
                                        // prompts a fresh pick within this one.
                                        onClear()
                                        autoAppliedLocationId = null
                                    }
                                },
                            )
                        }
                    }
                }

                Text(
                    if (units.size > 1) "2. Choisis une niche" else "Choisis une niche",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (availableLocations.isEmpty()) {
                    Text(
                        "Aucune niche libre dans ce casier.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableLocations, key = { it.id }) { location ->
                            PickerChip(
                                label = location.label,
                                selected = location.id == locationId,
                                highlightColor = wineColorFor(color),
                                onClick = {
                                    onPick(location.id)
                                    autoAppliedLocationId = location.id
                                },
                            )
                        }
                    }
                }
            }

            if (locationId != null) {
                TextButton(onClick = { onClear(); autoAppliedLocationId = null }) { Text("Retirer") }
            }
        }
    }
}

@Composable
private fun PickerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlightColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) highlightColor.copy(alpha = 0.35f) else highlightColor.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
