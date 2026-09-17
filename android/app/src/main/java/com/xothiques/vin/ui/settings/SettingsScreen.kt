package com.xothiques.vin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.xothiques.vin.data.remote.dto.HouseholdSummaryDto
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
    val householdsState by viewModel.householdsState.collectAsState()
    val switchHouseholdState by viewModel.switchHouseholdState.collectAsState()
    val createHouseholdState by viewModel.createHouseholdState.collectAsState()
    val joinHouseholdState by viewModel.joinHouseholdState.collectAsState()
    var showAddHouseholdChooser by remember { mutableStateOf(false) }
    var showCreateHouseholdDialog by remember { mutableStateOf(false) }
    var showJoinHouseholdDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(createHouseholdState) {
        if (createHouseholdState is UiState.Success) {
            viewModel.resetCreateHouseholdState()
            showCreateHouseholdDialog = false
        }
    }
    LaunchedEffect(joinHouseholdState) {
        if (joinHouseholdState is UiState.Success) {
            viewModel.resetJoinHouseholdState()
            showJoinHouseholdDialog = false
        }
    }
    LaunchedEffect(switchHouseholdState) {
        if (switchHouseholdState is UiState.Success) {
            viewModel.resetSwitchHouseholdState()
        }
    }

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
                        households = (householdsState as? UiState.Success)?.data.orEmpty(),
                        switchHouseholdState = switchHouseholdState,
                        regenerateState = regenerateState,
                        exportState = exportState,
                        onSwitchHousehold = viewModel::switchHousehold,
                        onAddHousehold = { showAddHouseholdChooser = true },
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

        if (showAddHouseholdChooser) {
            AddHouseholdChooserDialog(
                onDismiss = { showAddHouseholdChooser = false },
                onCreateNew = {
                    showAddHouseholdChooser = false
                    showCreateHouseholdDialog = true
                },
                onJoinExisting = {
                    showAddHouseholdChooser = false
                    showJoinHouseholdDialog = true
                },
            )
        }

        if (showCreateHouseholdDialog) {
            CreateHouseholdDialog(
                submitState = createHouseholdState,
                onDismiss = {
                    showCreateHouseholdDialog = false
                    viewModel.resetCreateHouseholdState()
                },
                onConfirm = viewModel::createHousehold,
            )
        }

        if (showJoinHouseholdDialog) {
            JoinHouseholdDialog(
                submitState = joinHouseholdState,
                onDismiss = {
                    showJoinHouseholdDialog = false
                    viewModel.resetJoinHouseholdState()
                },
                onConfirm = viewModel::joinHousehold,
            )
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
    households: List<HouseholdSummaryDto>,
    switchHouseholdState: UiState<Unit>?,
    regenerateState: UiState<Unit>?,
    exportState: UiState<File>?,
    onSwitchHousehold: (String) -> Unit,
    onAddHousehold: () -> Unit,
    onRegenerateInviteCode: () -> Unit,
    onOpenAiProviderSettings: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onSignOut: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Foyer", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (switchHouseholdState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onAddHousehold) {
                            Icon(Icons.Filled.Add, contentDescription = "Ajouter ou rejoindre un foyer")
                        }
                    }
                }

                // Every foyer this account belongs to -- tap one to switch,
                // e.g. "Maison" vs "Appartement". Falls back to just the
                // active household's name if the list hasn't loaded yet.
                if (households.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        households.forEach { summary ->
                            HouseholdRow(
                                summary = summary,
                                active = summary.id == household.id,
                                enabled = switchHouseholdState !is UiState.Loading,
                                onClick = { if (summary.id != household.id) onSwitchHousehold(summary.id) },
                            )
                        }
                    }
                } else {
                    Text(household.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                if (switchHouseholdState is UiState.Error) {
                    Text(switchHouseholdState.message, color = MaterialTheme.colorScheme.error)
                }

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

        AboutSection()
    }
}

/** Version de l'app installée -- utile pour vérifier qu'un rebuild a bien
 *  été installé, ou pour le signaler en cas de bug. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val (versionName, versionCode) = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            (info.versionName ?: "?") to code
        } catch (e: Exception) {
            "?" to 0L
        }
    }

    Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("À propos", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Vin",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Version $versionName (build $versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One row of the foyer switcher -- tap a non-active one to switch into it. */
@Composable
private fun HouseholdRow(
    summary: HouseholdSummaryDto,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !active, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.Home,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                summary.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                if (summary.role == "owner") "Propriétaire" else "Membre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (active) {
            Icon(Icons.Filled.Check, contentDescription = "Foyer actif", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** First step of adding a foyer -- choose between creating a brand new one
 *  or joining an existing one (someone else's) with its invite code. */
@Composable
private fun AddHouseholdChooserDialog(
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onJoinExisting: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un foyer") },
        text = {
            Text(
                "Crée un nouveau foyer (par exemple \"Appartement\") pour y gérer une cave séparée, ou rejoins un foyer existant avec le code d'invitation de quelqu'un d'autre.",
            )
        },
        confirmButton = {
            TextButton(onClick = onCreateNew) { Text("Créer un nouveau foyer") }
        },
        dismissButton = {
            TextButton(onClick = onJoinExisting) { Text("Rejoindre avec un code") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateHouseholdDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau foyer") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    placeholder = { Text("Appartement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Tu deviens propriétaire de ce foyer, avec son propre code d'invitation, ses propres membres et sa propre cave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (submitState is UiState.Error) {
                    Text(
                        submitState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = submitState !is UiState.Loading,
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinHouseholdDialog(
    submitState: UiState<Unit>?,
    onDismiss: () -> Unit,
    onConfirm: (inviteCode: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejoindre un foyer") },
        text = {
            Column {
                Text(
                    "Avec le code d'invitation que quelqu'un d'autre t'a partagé -- utilise ce même compte pour basculer entre tes foyers.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code d'invitation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (submitState is UiState.Error) {
                    Text(
                        submitState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (code.isNotBlank()) onConfirm(code.trim()) },
                enabled = submitState !is UiState.Loading,
            ) { Text("Rejoindre") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
