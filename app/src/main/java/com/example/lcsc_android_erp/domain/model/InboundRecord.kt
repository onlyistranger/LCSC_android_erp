package com.example.lcsc_android_erp.domain.model

data class InboundRecord(
    val component: ComponentDetail,
    val quantity: Int,
    val locationCode: String,
    val sourceType: String,
    val rawPayload: String? = null,
    val inboundAt: Long = System.currentTimeMillis()
)
