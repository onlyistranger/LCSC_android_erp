package com.example.lcsc_android_erp.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lcsc_android_erp.LcscApplication
import com.example.lcsc_android_erp.R
import com.example.lcsc_android_erp.core.datastore.UserPreferencesRepository

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier
) {
    val appContainer = (LocalContext.current.applicationContext as LcscApplication).appContainer
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(appContainer)
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        modifier = modifier,
        uiState = uiState.value,
        onLanguageSelected = viewModel::onLanguageSelected,
        onExportInventory = viewModel::exportInventory,
        onImportInventory = viewModel::importInventory,
        onClearInventoryBackupMessage = viewModel::clearInventoryBackupMessage
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLanguageSelected: (String) -> Unit,
    onExportInventory: (android.net.Uri) -> Unit,
    onImportInventory: (android.net.Uri) -> Unit,
    onClearInventoryBackupMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        uri?.let(onExportInventory)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImportInventory)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column {
                SettingsActionRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = when (uiState.selectedLanguageTag) {
                        UserPreferencesRepository.LANGUAGE_EN -> stringResource(R.string.settings_language_english)
                        else -> stringResource(R.string.settings_language_chinese)
                    },
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider()
                SettingsActionRow(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_summary),
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider()
                SettingsActionRow(
                    title = stringResource(R.string.settings_export_inventory),
                    subtitle = stringResource(R.string.settings_export_inventory_summary),
                    onClick = { exportLauncher.launch("lcsc_inventory_backup.xlsx") },
                    enabled = !uiState.isProcessingInventoryBackup
                )
                HorizontalDivider()
                SettingsActionRow(
                    title = stringResource(R.string.settings_import_inventory),
                    subtitle = stringResource(R.string.settings_import_inventory_summary),
                    onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "*/*"
                            )
                        )
                    },
                    enabled = !uiState.isProcessingInventoryBackup
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguageTag = uiState.selectedLanguageTag,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageTag ->
                onLanguageSelected(languageTag)
                showLanguageDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    uiState.inventoryBackupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onClearInventoryBackupMessage,
            title = { Text(text = stringResource(R.string.settings_inventory_backup)) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onClearInventoryBackupMessage) {
                    Text(text = stringResource(R.string.common_confirm))
                }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedLanguageTag: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOptionRow(
                    label = stringResource(R.string.settings_language_chinese),
                    selected = selectedLanguageTag == UserPreferencesRepository.LANGUAGE_ZH,
                    onClick = { onLanguageSelected(UserPreferencesRepository.LANGUAGE_ZH) }
                )
                LanguageOptionRow(
                    label = stringResource(R.string.settings_language_english),
                    selected = selectedLanguageTag == UserPreferencesRepository.LANGUAGE_EN,
                    onClick = { onLanguageSelected(UserPreferencesRepository.LANGUAGE_EN) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_about)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_stack_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_confirm))
            }
        }
    )
}
