package com.example.lcsc_android_erp.core.database.model

data class ExistingStockLocationProjection(
    val locationCode: String,
    val locationDisplayName: String?,
    val quantity: Int
)
