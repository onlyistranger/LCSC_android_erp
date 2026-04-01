package com.example.lcsc_android_erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lcsc_android_erp.core.i18n.AppLanguageManager
import com.example.lcsc_android_erp.ui.LcscApp
import com.example.lcsc_android_erp.ui.theme.LCSC_android_erpTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val appContainer = (application as LcscApplication).appContainer
        val languageTag = runBlocking {
            appContainer.userPreferencesRepository.preferences.first().appLanguageTag
        }
        AppLanguageManager.applyLanguage(this, languageTag)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            LCSC_android_erpTheme {
                LcscApp()
            }
        }
    }
}
