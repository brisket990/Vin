package com.xothiques.vin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.xothiques.vin.data.local.Session
import com.xothiques.vin.data.remote.dto.HouseholdDto
import com.xothiques.vin.ui.common.FullScreenError
import com.xothiques.vin.ui.common.FullScreenLoading
import com.xothiques.vin.ui.common.UiState
import com.xothiques.vin.ui.common.VinListRow
import com.xothiques.vin.ui.theme.ThemeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    onOpenAiProviderSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val householdState by viewModel.householdState.collectAsState()
    val regenerateState by viewModel.regenerateState.collectAsState()
    val exportState by exportViewModel.exportState.collectAsState()
    val session by viewModel.session.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
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

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ProfileHeader(
                session = session,
                themeMode = themeMode,
                onThemeModeChange = themeViewModel::setThemeMode,
            )
            Box(modifier = Modifier.fillMaxSize()) {
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
}

@Composable
private fun ProfileHeader(
    session: Session?,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
) {
    val displayName = session?.displayName?.takeIf { it.isNotBlank() } ?: "Toi"
    val email = session?.email

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayName.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (email != null) {
                        Text(
                            email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeChip(
                    label = "Clair",
                    icon = Icons.Filled.LightMode,
                    selected = themeMode != "dark",
                    onClick = { onThemeModeChange("light") },
                    modifier = Modifier.weight(1f),
                )
                ThemeModeChip(
                    label = "Sombre",
                    icon = Icons.Filled.DarkMode,
                    selected = themeMode == "dark",
                    onClick = { onThemeModeChange("dark") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
            labelColor = MaterialTheme.colorScheme.onPrimary,
            iconColor = MaterialTheme.colorScheme.onPrimary,
            selectedContainerColor = MaterialTheme.colorScheme.onPrimary,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = null,
    )
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(shape = MaterialTheme.shapes.large) {
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
                    "Code d'invitation — partage-le pour que ton/ta partenaire rejoigne la cave :",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        household.inviteCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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

        VinListRow(
            icon = Icons.Filled.SmartToy,
            title = "Fournisseurs IA",
            subtitle = "Clés API pour la reconnaissance d'étiquette et les accords mets-vin",
            onClick = onOpenAiProviderSettings,
        )

        Card(shape = MaterialTheme.shapes.large) {
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
            Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                "Se déconnecter",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
