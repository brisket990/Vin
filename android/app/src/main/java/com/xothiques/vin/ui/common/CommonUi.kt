package com.xothiques.vin.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Generic UI state wrapper used by most ViewModels in this app. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun FullScreenError(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Réessayer") }
            }
        }
    }
}

/**
 * Turns a caught Throwable into a user-facing French message. Retrofit
 * HttpException carries the server's JSON error body, which our NestJS API
 * always shapes as {"message": "...", "error": "...", "statusCode": ...}.
 */
fun Throwable.toUserMessage(): String {
    val httpException = this as? retrofit2.HttpException
    if (httpException != null) {
        val body = httpException.response()?.errorBody()?.string()
        val extracted = body?.let { raw ->
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.get(1)
        }
        if (!extracted.isNullOrBlank()) return extracted
        return "Erreur serveur (${httpException.code()})."
    }
    return message ?: "Une erreur inattendue est survenue."
}
