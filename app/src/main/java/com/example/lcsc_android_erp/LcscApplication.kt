package com.example.lcsc_android_erp

import android.app.Application
import com.example.lcsc_android_erp.core.AppContainer

class LcscApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
