package com.xothiques.vin.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            modifier = Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            // SelectionContainer lets the user long-press and copy the raw
            // error text (e.g. to paste it when reporting a bug), and the
            // verticalScroll above ensures a long provider error is never
            // cut off at the bottom of the screen.
            SelectionContainer {
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
            }
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Réessayer") }
            }
        }
    }
}

/**
 * Turns a caught Throwable into a user-facing French message. Retrofit
 * HttpException carries the server's JSON error body, which our NestJS API
 * always shapes as {"message": "...", "error": "...", "statusCode": ...}
 * -- "message" can be a plain string (our own BadRequestException/
 * BadGatewayException calls) or an array of strings (class-validator's
 * default ValidationPipe errors).
 *
 * This used to be extracted with a hand-rolled regex
 * (`"message"\s*:\s*"([^"]+)"`), which silently truncated the message at
 * the first *escaped* quote inside it -- and provider error bodies (e.g.
 * Google's raw Gemini error JSON, itself embedded as a string inside our
 * own JSON) are full of those. That's why a message like "Le fournisseur
 * IA a répondu une erreur (404) : { ... }" used to get cut off right after
 * the opening brace. Parsing the body as real JSON fixes this properly.
 */
fun Throwable.toUserMessage(): String {
    val httpException = this as? retrofit2.HttpException
    if (httpException != null) {
        val body = httpException.response()?.errorBody()?.string()
        val extracted = body?.let { raw ->
            runCatching {
                when (val messageElement = Json.parseToJsonElement(raw).jsonObject["message"]) {
                    is JsonArray -> messageElement.joinToString("\n") { it.jsonPrimitive.content }
                    is JsonPrimitive -> messageElement.contentOrNull
                    else -> null
                }
            }.getOrNull()
        }
        if (!extracted.isNullOrBlank()) return extracted
        return "Erreur serveur (${httpException.code()})."
    }
    return message ?: "Une erreur inattendue est survenue."
}
