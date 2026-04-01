package com.example.lcsc_android_erp.domain.model

data class StorageLocation(
    val id: Long,
    val code: String,
    val displayName: String?,
    val colorHex: String?,
    val sortMode: String,
    val remark: String?
)
