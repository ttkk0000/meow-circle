package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ReadingFact(val label: String,val text: String)
@Serializable data class ReadingDocument(val key: String,val type: String,val title: String,val heading: String = "",val revision: String,val status: String,
    @SerialName("effective_from") val effectiveFrom: String = "",@SerialName("effective_until") val effectiveUntil: String = "",val notice: String = "",
    val facts: List<ReadingFact> = emptyList(),val paragraphs: List<String> = emptyList(),val changes: List<String> = emptyList(),
    @SerialName("cover_asset") val coverAsset: String = "",val sha256: String = "")
@Serializable data class ReadingCatalog(@SerialName("operator_name") val operatorName: String = "",@SerialName("operator_address") val operatorAddress: String = "",
    @SerialName("operator_contact") val operatorContact: String = "",val documents: List<ReadingDocument> = emptyList())

suspend fun MobileApi.readingCatalog(): ReadingCatalog = get("reading",authenticated = false)
suspend fun MobileApi.readingDocument(key: String): ReadingDocument {
    require(Regex("^[a-z][a-z0-9-]{0,63}$").matches(key))
    return get("reading/$key",authenticated = false)
}

/** 目录顺序由发布物维护，优先展示发布文本；始终保留其真实状态标识。 */
fun ReadingCatalog.documentFor(type: String): ReadingDocument? =
    documents.firstOrNull { it.type == type && it.status == "published" }
        ?: documents.firstOrNull { it.type == type && it.status != "archived" }
        ?: documents.firstOrNull { it.type == type }
