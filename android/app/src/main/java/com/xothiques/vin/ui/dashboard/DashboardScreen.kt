package com.xothiques.vin.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.DashboardStatsDto
import com.xothiques.vin.data.remote.dto.RecentTastingDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState

private val COLOR_LABELS = mapOf(
    "red" to "Rouge",
    "white" to "Blanc",
    "rose" to "Rosé",
    "sparkling" to "Effervescent",
    "sweet" to "Liquoreux",
    "fortified" to "Muté / fortifié",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Tableau de bord") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> FullScreenError(s.message, onRetry = viewModel::load)
                is UiState.Success -> DashboardContent(s.data)
            }
        }
    }
}

@Composable
private fun DashboardContent(stats: DashboardStatsDto) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column {
                        Text("${stats.totalBottles}", style = MaterialTheme.typography.headlineMedium)
                        Text("bouteilles", style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        Text(
                            "%.2f €".format(stats.totalValueCents / 100.0),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text("valeur estimée", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (stats.byColor.isNotEmpty()) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Répartition par type", style = MaterialTheme.typography.titleMedium)
                        stats.byColor.entries.sortedByDescending { it.value }.forEach { (color, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(COLOR_LABELS[color] ?: color, style = MaterialTheme.typography.bodyMedium)
                                Text("$count", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fenêtres d'apogée à venir", style = MaterialTheme.typography.titleMedium)
                    if (stats.upcomingApogee.isEmpty()) {
                        Text("Rien de particulier à signaler.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        stats.upcomingApogee.forEach { bottle -> ApogeeRow(bottle) }
                    }
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dernières dégustations", style = MaterialTheme.typography.titleMedium)
                    if (stats.recentTastings.isEmpty()) {
                        Text("Aucune bouteille dégustée pour l'instant.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        stats.recentTastings.forEachIndexed { index, tasting ->
                            TastingRow(tasting)
                            if (index != stats.recentTastings.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApogeeRow(bottle: BottleDto) {
    val range = when {
        bottle.drinkFromYear != null && bottle.drinkUntilYear != null ->
            "${bottle.drinkFromYear} – ${bottle.drinkUntilYear}"
        bottle.drinkFromYear != null -> "à partir de ${bottle.drinkFromYear}"
        bottle.drinkUntilYear != null -> "jusqu'à ${bottle.drinkUntilYear}"
        else -> null
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(bottle.name, style = MaterialTheme.typography.bodyMedium)
        if (range != null) Text(range, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TastingRow(tasting: RecentTastingDto) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tasting.bottleName, style = MaterialTheme.typography.bodyMedium)
            tasting.rating?.let { Text("$it/5", style = MaterialTheme.typography.bodyMedium) }
        }
        tasting.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
