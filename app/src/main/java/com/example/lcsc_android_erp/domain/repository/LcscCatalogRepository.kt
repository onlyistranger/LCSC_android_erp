package com.example.lcsc_android_erp.domain.repository

import com.example.lcsc_android_erp.domain.model.ComponentDetail

interface LcscCatalogRepository {
    suspend fun lookupByPartNumber(partNumber: String): ComponentDetail?
    suspend fun searchByKeyword(keyword: String): List<ComponentDetail>
}
