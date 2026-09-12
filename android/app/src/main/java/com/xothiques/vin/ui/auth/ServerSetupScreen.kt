package com.xothiques.vin.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.xothiques.vin.ui.common.UiState

/**
 * First screen ever shown: the app talks to a self-hosted backend, not a
 * fixed public API, so the household's server address has to be entered
 * once before anything else can work.
 */
@Composable
fun ServerSetupScreen(viewModel: AuthViewModel = hiltViewModel()) {
    var url by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    val submitState by viewModel.submitState.collectAsState()

    LaunchedEffect(Unit) {
        if (!prefilled) {
            url = viewModel.currentServerBaseUrl()
            prefilled = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Bienvenue dans Vin", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Indique l'adresse de ton serveur (celui que tu as déployé sur Coolify), par exemple https://cave.xothiques.duckdns.org.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Adresse du serveur") },
            placeholder = { Text("https://cave.mondomaine.fr") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        if (submitState is UiState.Error) {
            Text(
                (submitState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = { viewModel.saveServerBaseUrl(url.trim()) },
            enabled = url.isNotBlank() && submitState !is UiState.Loading,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        ) {
            Text("Continuer")
        }
    }
}
