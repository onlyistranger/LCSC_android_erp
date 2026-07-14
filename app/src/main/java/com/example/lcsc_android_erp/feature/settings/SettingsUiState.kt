package com.example.lcsc_android_erp.feature.settings

data class SettingsUiState(
    val selectedLanguageTag: String = "zh",
    val isProcessingInventoryBackup: Boolean = false,
    val inventoryBackupProgress: Float? = null,
    val inventoryBackupMessage: String? = null,
    val content: SettingsContent = SettingsContent(
        title = "",
        languageTitle = "",
        languageChinese = "",
        languageEnglish = "",
        aboutTitle = "",
        aboutSummary = "",
        inventoryBackupTitle = "",
        exportInventoryTitle = "",
        exportInventorySummary = "",
        importInventoryTitle = "",
        importInventorySummary = "",
        aboutBody = "",
        stackBody = ""
    )
)
