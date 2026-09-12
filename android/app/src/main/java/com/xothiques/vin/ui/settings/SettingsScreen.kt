package com.xothiques.vin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    onOpenAiProviderSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel(),
) {
    val householdState by viewModel.householdState.collectAsState()
    val regenerateState by viewModel.regenerateState.collectAsState()
    val exportState by exportViewModel.exportState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportState) {
        val file = (exportState as? UiState.Success)?.data ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Partager l'export"))
        exportViewModel.resetExportState()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Réglages") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = householdState) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> FullScreenError(state.message, onRetry = viewModel::load)
                is UiState.Success -> SettingsContent(
                    household = state.data,
                    regenerateState = regenerateState,
                    exportState = exportState,
                    onRegenerateInviteCode = viewModel::regenerateInviteCode,
                    onOpenAiProviderSettings = onOpenAiProviderSettings,
                    onExportCsv = exportViewModel::exportCsv,
                    onExportPdf = exportViewModel::exportPdf,
                    onSignOut = {
                        viewModel.signOut()
                        onSignedOut()
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    household: HouseholdDto,
    regenerateState: UiState<Unit>?,
    exportState: UiState<File>?,
    onRegenerateInviteCode: () -> Unit,
    onOpenAiProviderSettings: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onSignOut: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Foyer", style = MaterialTheme.typography.titleMedium)
                Text(household.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Membres", style = MaterialTheme.typography.titleSmall)
                household.members.forEach { member ->
                    Text(
                        "${member.displayName} (${member.email}) — ${if (member.role == "owner") "propriétaire" else "membre"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    "Code d'invitation — partage-le avec ton/ta partenaire pour qu'iel rejoigne la cave :",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        household.inviteCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(household.inviteCode))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copier le code")
                    }
                }
                if (regenerateState is UiState.Error) {
                    Text(regenerateState.message, color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = onRegenerateInviteCode, enabled = regenerateState !is UiState.Loading) {
                    Text("Régénérer le code")
                }
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Intelligence artificielle", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Configure tes clés API (Claude, Gemini, GPT) pour la reconnaissance d'étiquette et les accords mets-vin.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenAiProviderSettings) { Text("Configurer les fournisseurs IA") }
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Exporte l'inventaire de ta cave pour le partager ou l'archiver.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (exportState is UiState.Error) {
                    Text(exportState.message, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        enabled = exportState !is UiState.Loading,
                        modifier = Modifier.weight(1f),
                    ) { Text("Export CSV") }
                    OutlinedButton(
                        onClick = onExportPdf,
                        enabled = exportState !is UiState.Loading,
                        modifier = Modifier.weight(1f),
                    ) { Text("Export PDF") }
                }
            }
        }

        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
        }
    }
}
