package com.ttkk0000.meowcircle.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val followers: Long,
    val following: Long,
)

@Serializable
data class Order(
    val id: Long,
    @SerialName("listing_id") val listingId: Long,
    @SerialName("buyer_id") val buyerId: Long,
    @SerialName("seller_id") val sellerId: Long,
    val status: String,
    @SerialName("amount_cents") val amountCents: Long,
    val currency: String,
    @SerialName("listing_title") val listingTitle: String = "",
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class OrderListResponse(
    val items: List<Order>? = null,
)

@Serializable
data class Message(
    val id: Long,
    @SerialName("sender_id") val senderId: Long,
    @SerialName("recipient_id") val recipientId: Long,
    val content: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ConversationDetails(
    val messages: List<Message>? = null,
    val peer: com.ttkk0000.meowcircle.User,
)

@Serializable
data class AuditLog(
    val id: Long,
    val action: String,
    val actor: String = "",
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AuditLogListResponse(
    val items: List<AuditLog>? = null,
)

@Serializable
data class CreateOrderRequest(
    @SerialName("listing_id") val listingId: Long,
)

@Serializable
data class CreateListingRequest(
    val type: String,
    val title: String,
    val description: String,
    @SerialName("price_cents") val priceCents: Long,
    val currency: String = "USD",
)
