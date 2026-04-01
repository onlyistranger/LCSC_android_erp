package com.example.lcsc_android_erp.feature.settings

data class SettingsUiState(
    val selectedLanguageTag: String = "zh",
    val isProcessingInventoryBackup: Boolean = false,
    val inventoryBackupMessage: String? = null
)
