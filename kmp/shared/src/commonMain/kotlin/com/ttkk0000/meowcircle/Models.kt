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
