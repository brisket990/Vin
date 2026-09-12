package com.xothiques.vin.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegisterHousehold: () -> Unit,
    onJoinHousehold: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ma cave à vin", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Gère ta cave, scanne tes étiquettes et trouve le bon accord pour ce soir.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Se connecter")
        }
        OutlinedButton(
            onClick = onRegisterHousehold,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text("Créer un foyer")
        }
        TextButton(
            onClick = onJoinHousehold,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text("Rejoindre le foyer d'une autre personne")
        }
    }
}
