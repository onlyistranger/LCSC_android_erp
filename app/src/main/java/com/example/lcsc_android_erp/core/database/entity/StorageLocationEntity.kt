package com.example.lcsc_android_erp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lcsc_android_erp.domain.model.StorageLocationSortMode

@Entity(
    tableName = "storage_location",
    indices = [Index(value = ["code"], unique = true)]
)
data class StorageLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val displayName: String? = null,
    val colorHex: String? = null,
    val sortMode: String = StorageLocationSortMode.NONE,
    val remark: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
