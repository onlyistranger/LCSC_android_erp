package com.example.lcsc_android_erp.domain.model

data class StockLocationCell(
    val id: Long,
    val code: String,
    val displayName: String?,
    val colorHex: String?,
    val sortMode: String,
    val remark: String?,
    val inventoryItemCount: Int,
    val totalQuantity: Int
)
