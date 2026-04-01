package com.example.lcsc_android_erp.core.i18n

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

object AppLanguageManager {
    fun applyLanguage(context: Context, languageTag: String) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales = LocaleList.forLanguageTags(languageTag)
    }
}
