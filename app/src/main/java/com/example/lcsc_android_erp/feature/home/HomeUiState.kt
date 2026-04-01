package com.example.lcsc_android_erp.feature.home

import com.example.lcsc_android_erp.domain.model.DashboardSummary
import com.example.lcsc_android_erp.domain.model.StorageLocation

data class HomeUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val locations: List<StorageLocation> = emptyList(),
    val defaultLocationCode: String? = null
)
