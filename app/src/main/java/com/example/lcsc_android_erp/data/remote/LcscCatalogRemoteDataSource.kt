package com.example.lcsc_android_erp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.jsoup.Jsoup

class LcscCatalogRemoteDataSource(
    private val okHttpClient: OkHttpClient
) {
    fun searchProducts(keyword: String): List<JSONObject> {
        val url = "https://so.szlcsc.com/global.html".toHttpUrl()
            .newBuilder()
            .addQueryParameter("k", keyword)
            .build()
            .toString()
        val root = fetchNextData(url) ?: return emptyList()
        val productList = root
            .optJSONObject("props")
            ?.optJSONObject("pageProps")
            ?.optJSONObject("soData")
            ?.optJSONObject("searchResult")
            ?.optJSONArray("productRecordList")
            ?: return emptyList()

        return buildList {
            for (index in 0 until productList.length()) {
                productList.optJSONObject(index)?.let(::add)
            }
        }
    }

    fun searchMatchedProduct(partNumber: String): JSONObject? {
        val normalizedPartNumber = partNumber.trim().uppercase()
        for (record in searchProducts(partNumber)) {
            val productCode = record
                .optJSONObject("productVO")
                ?.optStringOrNull("productCode")
                ?.trim()
                ?.uppercase()
                ?: continue
            if (productCode == normalizedPartNumber) {
                return record
            }
        }

        return null
    }

    private fun fetchNextData(url: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
            )
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                return null
            }

            val document = Jsoup.parse(html)
            val nextData = document.selectFirst("script#__NEXT_DATA__")?.data()
                ?: return null

            return JSONObject(nextData)
        }
    }
}

internal fun JSONObject.optStringOrNull(name: String): String? {
    val value = optString(name)
    return value.takeIf { it.isNotBlank() && it != "null" }
}

internal fun JSONObject.optIntOrNull(name: String): Int? {
    return if (has(name) && !isNull(name)) optInt(name) else null
}

internal fun JSONObject.optDoubleOrNull(name: String): Double? {
    return if (has(name) && !isNull(name)) optDouble(name) else null
}
