package com.example.lcsc_android_erp.feature.inventory

import com.example.lcsc_android_erp.domain.model.ComponentDetail
import com.example.lcsc_android_erp.domain.model.ExistingStockLocation

data class InventoryScanLookupResult(
    val component: ComponentDetail? = null,
    val quantity: Int = 0,
    val rawPayload: String? = null,
    val errorMessage: String? = null,
    val existingStockLocations: List<ExistingStockLocation> = emptyList()
)
