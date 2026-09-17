package com.xothiques.vin.ui.bottle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinHeader
import com.xothiques.vin.ui.common.VinListRow
import com.xothiques.vin.ui.theme.wineColorFor
import com.xothiques.vin.util.needsTurn

/**
 * Flat, scrollable listing of every bottle currently in the cellar -- the
 * grid view is great for seeing physical placement, but browsing/searching
 * the collection as a plain list is faster than scanning a grid of casiers.
 * Reached from the cave screen's header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottleListScreen(
    onBack: () -> Unit,
    onOpenBottle: (String) -> Unit,
    viewModel: BottleListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    // Bottles can be added/edited/consumed/deleted from other screens pushed
    // on top of this one -- refresh whenever we come back into view.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(title = "Liste des bouteilles", onBack = onBack)

            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = state) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(s.message, onRetry = viewModel::load)
                    is UiState.Success -> BottleListContent(
                        bottles = s.data,
                        query = query,
                        onQueryChange = { query = it },
                        onOpenBottle = onOpenBottle,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottleListContent(
    bottles: List<BottleDto>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenBottle: (String) -> Unit,
) {
    val filtered = remember(bottles, query) {
        if (query.isBlank()) {
            bottles
        } else {
            val needle = query.trim()
            bottles.filter { bottle ->
                bottle.name.contains(needle, ignoreCase = true) ||
                    bottle.producer?.contains(needle, ignoreCase = true) == true ||
                    bottle.region?.contains(needle, ignoreCase = true) == true ||
                    bottle.appellation?.contains(needle, ignoreCase = true) == true ||
                    bottle.notes?.contains(needle, ignoreCase = true) == true ||
                    bottle.grapeVarieties?.any { it.contains(needle, ignoreCase = true) } == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Rechercher (nom, producteur, région, appellation, cépage, notes)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        Text(
            "${filtered.size} bouteille" + if (filtered.size > 1) "s" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (bottles.isEmpty()) {
            Text(
                "Aucune bouteille en cave pour l'instant.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else if (filtered.isEmpty()) {
            Text(
                "Aucune bouteille ne correspond à cette recherche.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { bottle ->
                    BottleRow(bottle, onClick = { onOpenBottle(bottle.id) })
                }
            }
        }
    }
}

@Composable
private fun BottleRow(bottle: BottleDto, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        bottle.producer,
        bottle.vintage?.toString(),
        listOfNotNull(bottle.region, bottle.appellation).joinToString(" / ").ifBlank { null },
    ).joinToString(" — ").ifBlank { null }

    VinListRow(
        icon = Icons.Filled.WineBar,
        title = bottle.name,
        subtitle = subtitle,
        badgeColor = wineColorFor(bottle.color).copy(alpha = 0.18f),
        badgeContentColor = wineColorFor(bottle.color),
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bottle.needsTurn()) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = "Quart de tour à faire",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 6.dp).size(18.dp),
                    )
                }
                Text(
                    "×${bottle.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
