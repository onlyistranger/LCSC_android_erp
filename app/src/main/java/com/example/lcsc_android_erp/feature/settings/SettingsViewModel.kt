package com.example.lcsc_android_erp.feature.settings

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.lcsc_android_erp.core.AppContainer
import com.example.lcsc_android_erp.core.datastore.UserPreferencesRepository
import com.example.lcsc_android_erp.core.i18n.AppLanguageManager
import com.example.lcsc_android_erp.R
import com.example.lcsc_android_erp.data.repository.InventoryBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val inventoryBackupManager: InventoryBackupManager,
    private val appContext: Context
) : ViewModel() {
    private val screenState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferences,
        screenState
    ) { preferences, state ->
        state.copy(
            selectedLanguageTag = preferences.appLanguageTag,
            content = SettingsContentLoader.load(appContext, preferences.appLanguageTag)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onLanguageSelected(languageTag: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(languageTag)
            AppLanguageManager.applyLanguage(appContext, languageTag)
        }
    }

    fun clearInventoryBackupMessage() {
        screenState.update { it.copy(inventoryBackupMessage = null) }
    }

    fun exportInventory(uri: Uri) {
        viewModelScope.launch {
            screenState.update {
                it.copy(
                    isProcessingInventoryBackup = true,
                    inventoryBackupProgress = 0f,
                    inventoryBackupMessage = null
                )
            }
            val error = inventoryBackupManager.exportToUri(uri) { processed, total ->
                screenState.update { state ->
                    state.copy(
                        inventoryBackupProgress = if (total > 0) {
                            processed.toFloat() / total.toFloat()
                        } else {
                            null
                        }
                    )
                }
            }
            screenState.update {
                it.copy(
                    isProcessingInventoryBackup = false,
                    inventoryBackupProgress = null,
                    inventoryBackupMessage = error ?: appContext.getString(R.string.settings_export_inventory_success)
                )
            }
        }
    }

    fun importInventory(uri: Uri) {
        viewModelScope.launch {
            screenState.update {
                it.copy(
                    isProcessingInventoryBackup = true,
                    inventoryBackupProgress = 0f,
                    inventoryBackupMessage = null
                )
            }
            // Give Compose a frame to draw the progress bar before POI starts parsing the file.
            delay(100)
            val error = inventoryBackupManager.importFromUri(uri) { processed, total ->
                screenState.update { state ->
                    state.copy(
                        inventoryBackupProgress = if (total > 0) {
                            processed.toFloat() / total.toFloat()
                        } else {
                            null
                        }
                    )
                }
            }
            screenState.update {
                it.copy(
                    isProcessingInventoryBackup = false,
                    inventoryBackupProgress = null,
                    inventoryBackupMessage = error ?: appContext.getString(R.string.settings_import_inventory_success)
                )
            }
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    userPreferencesRepository = appContainer.userPreferencesRepository,
                    inventoryBackupManager = appContainer.inventoryBackupManager,
                    appContext = appContainer.appContext
                )
            }
        }
    }
}
