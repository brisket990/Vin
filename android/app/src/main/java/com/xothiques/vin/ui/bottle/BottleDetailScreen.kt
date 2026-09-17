package com.xothiques.vin.ui.bottle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xothiques.vin.data.remote.dto.BottleDto
import com.xothiques.vin.data.remote.dto.FoodPairingResultDto
import com.xothiques.vin.data.remote.dto.RecipeSuggestionResultDto
import com.xothiques.vin.data.remote.resolvePhotoUrl
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinHeader
import com.xothiques.vin.ui.theme.wineColorFor
import com.xothiques.vin.util.daysSinceLastTurn
import com.xothiques.vin.util.needsTurn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottleDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: BottleDetailViewModel = hiltViewModel(),
) {
    val bottleState by viewModel.bottleState.collectAsState()
    val consumeState by viewModel.consumeState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val foodPairingState by viewModel.foodPairingState.collectAsState()
    val recipeState by viewModel.recipeState.collectAsState()
    val turnState by viewModel.turnState.collectAsState()
    var showConsumeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(consumeState) {
        if (consumeState is UiState.Success) {
            viewModel.resetConsumeState()
            showConsumeDialog = false
        }
    }
    LaunchedEffect(deleteState) {
        if (deleteState is UiState.Success) onBack()
    }
    LaunchedEffect(turnState) {
        if (turnState is UiState.Success) {
            viewModel.resetTurnState()
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VinHeader(
                title = "Bouteille",
                onBack = onBack,
                trailing = {
                    val bottle = (bottleState as? UiState.Success)?.data
                    if (bottle != null) {
                        Row {
                            if (bottle.status == "in_cellar") {
                                IconButton(onClick = { showConsumeDialog = true }) {
                                    Icon(
                                        Icons.Filled.WineBar,
                                        contentDescription = "Dégustée",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                            IconButton(onClick = { onEdit(bottle.id) }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = bottleState) {
                    is UiState.Loading -> FullScreenLoading()
                    is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::load)
                    is UiState.Success -> BottleDetailContent(
                        bottle = state.data,
                        onConsume = { showConsumeDialog = true },
                        foodPairingState = foodPairingState,
                        onSuggestFoodPairing = viewModel::suggestFoodPairing,
                        recipeState = recipeState,
                        onSuggestRecipe = viewModel::suggestRecipe,
                        turnState = turnState,
                        onTurn = viewModel::turn,
                    )
                }
            }
        }

        if (showConsumeDialog) {
            ConsumeDialog(
                submitState = consumeState,
                maxQuantity = (bottleState as? UiState.Success)?.data?.quantity ?: 1,
                onDismiss = { showConsumeDialog = false },
                onConfirm = viewModel::consume,
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Supprimer cette bouteille ?") },
                text = { Text("Cette action est définitive.") },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
                },
            )
        }
    }
}

@Composable
private fun BottleDetailContent(
    bottle: BottleDto,
    onConsume: () -> Unit,
    foodPairingState: UiState<FoodPairingResultDto>?,
    onSuggestFoodPairing: () -> Unit,
    recipeState: UiState<RecipeSuggestionResultDto>?,
    onSuggestRecipe: () -> Unit,
    turnState: UiState<Unit>?,
    onTurn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val photoUrl = resolvePhotoUrl(bottle.labelPhotoUrl)
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Étiquette",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(wineColorFor(bottle.color), RoundedCornerShape(4.dp)),
            )
            Text(
                text = bottle.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val subtitle = listOfNotNull(bottle.producer, bottle.vintage?.toString())
            .joinToString(" — ")
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.titleMedium)
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Région", listOfNotNull(bottle.region, bottle.appellation).joinToString(" / ").ifBlank { "—" })
                InfoRow("Cépages", bottle.grapeVarieties?.joinToString(", ")?.ifBlank { "—" } ?: "—")
                InfoRow("Quantité en cave", bottle.quantity.toString())
                InfoRow(
                    "Prix d'achat",
                    bottle.purchasePriceCents?.let { "%.2f €".format(it / 100.0) } ?: "—",
                )
                InfoRow(
                    "Fenêtre d'apogée",
                    if (bottle.drinkFromYear != null || bottle.drinkUntilYear != null) {
                        "${bottle.drinkFromYear ?: "?"} — ${bottle.drinkUntilYear ?: "?"}"
                    } else "—",
                )
                InfoRow("Statut", if (bottle.status == "in_cellar") "En cave" else "Bue")
            }
        }

        if (!bottle.notes.isNullOrBlank()) {
            Text("Notes", style = MaterialTheme.typography.titleSmall)
            Text(bottle.notes)
        }

        if (!bottle.tastingNose.isNullOrBlank() || !bottle.tastingPalate.isNullOrBlank() ||
            !bottle.tastingSweetness.isNullOrBlank()
        ) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Profil de dégustation", style = MaterialTheme.typography.titleSmall)
                    if (!bottle.tastingNose.isNullOrBlank()) InfoRow("Nez", bottle.tastingNose)
                    if (!bottle.tastingPalate.isNullOrBlank()) InfoRow("Bouche", bottle.tastingPalate)
                    if (!bottle.tastingSweetness.isNullOrBlank()) InfoRow("Sucrosité", bottle.tastingSweetness)
                }
            }
        }

        if (bottle.status == "in_cellar") {
            TurnReminderCard(bottle = bottle, turnState = turnState, onTurn = onTurn)
        }

        if (bottle.status == "in_cellar") {
            OutlinedButton(
                onClick = onSuggestFoodPairing,
                modifier = Modifier.fillMaxWidth(),
                enabled = foodPairingState !is UiState.Loading,
            ) {
                if (foodPairingState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Suggérer un accord mets-vin pour cette bouteille")
                }
            }
        }

        if (foodPairingState != null) {
            FoodPairingCard(foodPairingState)
        }

        if (bottle.status == "in_cellar") {
            OutlinedButton(
                onClick = onSuggestRecipe,
                modifier = Modifier.fillMaxWidth(),
                enabled = recipeState !is UiState.Loading,
            ) {
                if (recipeState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Suggérer une idée de recette pour cette bouteille")
                }
            }
        }

        if (recipeState != null) {
            RecipeSuggestionCard(recipeState)
        }

        if (bottle.status == "in_cellar") {
            Button(onClick = onConsume, modifier = Modifier.fillMaxWidth()) {
                Text("Marquer comme bue")
            }
        }
    }
}

/**
 * "Quart de tour" -- classic advice for a bottle stored lying down under
 * natural cork: give it a quarter turn every few months so sediment/the
 * cork don't always settle on one side. Shows how long it's been since the
 * last turn (or since the bottle was added, if never explicitly turned)
 * and highlights it once overdue, with a one-tap action to log today's turn.
 */
@Composable
private fun TurnReminderCard(
    bottle: BottleDto,
    turnState: UiState<Unit>?,
    onTurn: () -> Unit,
) {
    val days = bottle.daysSinceLastTurn()
    val overdue = bottle.needsTurn()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = if (overdue) {
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            )
        } else {
            androidx.compose.material3.CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quart de tour", style = MaterialTheme.typography.titleSmall)
            Text(
                text = when {
                    days == null -> "Date d'ajout inconnue."
                    days == 0L -> "Tournée aujourd'hui."
                    days == 1L -> "Tournée il y a 1 jour."
                    else -> "Tournée il y a $days jours."
                } + if (overdue) " Il est temps de lui donner un quart de tour." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            )
            if (turnState is UiState.Error) {
                Text(turnState.message, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(
                onClick = onTurn,
                modifier = Modifier.fillMaxWidth(),
                enabled = turnState !is UiState.Loading,
            ) {
                if (turnState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Tournée aujourd'hui")
                }
            }
        }
    }
}

@Composable
private fun FoodPairingCard(state: UiState<FoodPairingResultDto>) {
    when (state) {
        is UiState.Error -> {
            Card(shape = MaterialTheme.shapes.large) {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        is UiState.Success -> {
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Accords suggérés", style = MaterialTheme.typography.titleSmall)
                    state.data.suggestedDishes.forEach { dish ->
                        Text("• $dish", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.data.reasoning.isNotBlank()) {
                        Text(
                            state.data.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun RecipeSuggestionCard(state: UiState<RecipeSuggestionResultDto>) {
    when (state) {
        is UiState.Error -> {
            Card(shape = MaterialTheme.shapes.large) {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        is UiState.Success -> {
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Idée de recette", style = MaterialTheme.typography.titleSmall)
                    Text(state.data.recipeTitle, fontWeight = FontWeight.Bold)
                    Text(state.data.recipeDescription, style = MaterialTheme.typography.bodyMedium)
                    if (state.data.reasoning.isNotBlank()) {
                        Text(
                            state.data.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConsumeDialog(
    submitState: UiState<Unit>?,
    maxQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int?, rating: Int?, comment: String?) -> Unit,
) {
    var quantity by remember { mutableStateOf("1") }
    var rating by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bouteille bue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ajoute une note de dégustation pour garder une trace de ce que tu en as pensé.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter(Char::isDigit) },
                    label = { Text("Quantité bue (sur $maxQuantity)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rating,
                    onValueChange = { rating = it.filter(Char::isDigit) },
                    label = { Text("Note sur 5 (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (submitState is UiState.Error) {
                    Text(submitState.message, color = MaterialTheme.colorScheme.error)
                }
                if (submitState is UiState.Loading) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        quantity.toIntOrNull(),
                        rating.toIntOrNull(),
                        comment.ifBlank { null },
                    )
                },
                enabled = submitState !is UiState.Loading,
            ) { Text("Confirmer") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
