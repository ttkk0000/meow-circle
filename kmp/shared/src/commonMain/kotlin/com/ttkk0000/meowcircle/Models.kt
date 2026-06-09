package com.ttkk0000.meowcircle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope(
    val code: Int,
    val message: String,
    val data: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class User(
    val id: Long,
    val username: String,
    val nickname: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val bio: String = "",
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class Post(
    val id: Long,
    @SerialName("author_id") val authorId: Long,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String> = emptyList(),
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_reply_at") val lastReplyAt: String,
)

@Serializable
data class Media(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val kind: String,
    val mime: String,
    val size: Long,
    val filename: String,
    val url: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class PostFeedItem(
    val post: Post,
    val author: User,
    @SerialName("like_count") val likeCount: Long,
    val liked: Boolean,
    @SerialName("first_media") val firstMedia: Media? = null,
)

@Serializable
data class PostsPage(
    val items: List<PostFeedItem>? = null,
    val total: Long,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val filter: String? = null,
)

@Serializable
data class Listing(
    val id: Long,
    @SerialName("seller_id") val sellerId: Long,
    val type: String,
    val title: String,
    val description: String,
    @SerialName("price_cents") val priceCents: Long,
    val currency: String = "CNY",
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ListingDetailData(
    val listing: Listing,
    val media: List<Media> = emptyList(),
)

@Serializable
data class ListingsPage(
    val items: List<Listing>? = null,
    val total: Long = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
)

@Serializable
data class LoginBody(
    val username: String,
    val password: String,
)

@Serializable
data class LoginData(
    val token: String,
    val user: User,
)

@Serializable
data class HealthData(
    val status: String,
    val store: String,
)

@Serializable
data class Comment(
    val id: Long,
    @SerialName("post_id") val postId: Long,
    @SerialName("author_id") val authorId: Long,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    val author: User? = null,
)

@Serializable
data class PostDetailData(
    val post: Post,
    val media: List<Media> = emptyList(),
    val comments: List<Comment> = emptyList(),
    @SerialName("like_count") val likeCount: Long = 0,
    val liked: Boolean = false,
    val author: User,
    @SerialName("following_author") val followingAuthor: Boolean = false,
)

@Serializable
data class RegisterBody(
    val username: String,
    val password: String,
    val nickname: String = "",
    val phone: String = "",
    @SerialName("sms_code") val smsCode: String = "",
)

@Serializable
data class CreatePostBody(
    val title: String,
    val content: String,
    val category: String = "daily_share",
    val tags: List<String> = emptyList(),
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
)

@Serializable
data class CreateListingBody(
    val type: String = "product",
    val title: String,
    val description: String = "",
    @SerialName("price_cents") val priceCents: Long,
    val currency: String = "CNY",
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
)

@Serializable
data class Conversation(
    val peer: User,
    @SerialName("last_message") val lastMessage: String,
    @SerialName("last_sender_id") val lastSenderId: Long = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ConversationsPayload(
    val items: List<Conversation>? = null,
)

@Serializable
data class Message(
    val id: Long,
    @SerialName("sender_id") val senderId: Long,
    @SerialName("recipient_id") val recipientId: Long,
    val content: String,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ConversationDetailData(
    val peer: User,
    val messages: List<Message> = emptyList(),
)

@Serializable
data class SendMessageBody(
    @SerialName("recipient_id") val recipientId: Long,
    val content: String,
)

@Serializable
data class FollowActionResult(
    val following: Boolean,
    @SerialName("user_id") val userId: Long,
)

@Serializable
data class UpdateMeBody(
    val nickname: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val bio: String = "",
)

@Serializable
data class Order(
    val id: Long,
    @SerialName("buyer_id") val buyerId: Long,
    @SerialName("seller_id") val sellerId: Long,
    @SerialName("listing_id") val listingId: Long,
    @SerialName("listing_title") val listingTitle: String,
    @SerialName("amount_cents") val amountCents: Long,
    val currency: String,
    val status: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_tx_id") val paymentTxId: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("shipped_at") val shippedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class OrdersPayload(
    val items: List<Order>? = null,
    val role: String? = null,
)

@Serializable
data class NotificationItem(
    val id: Long,
    @SerialName("user_id") val userId: Long,
    val kind: String,
    val title: String,
    val body: String = "",
    @SerialName("ref_id") val refId: Long = 0,
    @SerialName("actor_id") val actorId: Long = 0,
    @SerialName("actor_username") val actorUsername: String = "",
    @SerialName("actor_nickname") val actorNickname: String = "",
    @SerialName("actor_avatar_url") val actorAvatarUrl: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class NotificationsPayload(
    val items: List<NotificationItem>? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class CreateOrderBody(
    @SerialName("listing_id") val listingId: Long,
    val note: String = "",
)

@Serializable
data class PayOrderBody(
    val method: String = "mock",
)
