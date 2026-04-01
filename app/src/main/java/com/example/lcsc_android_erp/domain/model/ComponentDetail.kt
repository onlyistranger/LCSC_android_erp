package com.example.lcsc_android_erp.domain.model

data class ComponentDetail(
    val partNumber: String,
    val mpn: String?,
    val name: String?,
    val brand: String?,
    val packageName: String?,
    val category: String?,
    val description: String?,
    val stockQuantity: Int?,
    val price: Double?,
    val productUrl: String?,
    val datasheetUrl: String?,
    val imageUrl: String?,
    val specifications: Map<String, String>
)
