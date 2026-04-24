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
    val items: List<PostFeedItem>,
    val total: Long,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val filter: String? = null,
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
data class Conversation(
    val peer: User,
    @SerialName("last_message") val lastMessage: String,
    @SerialName("last_sender_id") val lastSenderId: Long = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ConversationsPayload(
    val items: List<Conversation>,
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
