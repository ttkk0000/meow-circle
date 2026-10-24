package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileUser(val id: Long, val username: String, val nickname: String, val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "")

@Serializable
data class MobileProfile(val user: MobileUser, val version: Long, val theme: String = "honey", val background: String = "picnic")

@Serializable
data class LoginSession(val user: MobileUser, val token: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: String, val created: Boolean = false)

@Serializable
data class LoginCapabilities(val sms: Boolean = false, val password: Boolean = false, val providers: List<String> = emptyList(),
    val email: Boolean = false, val registration: Boolean = false, @SerialName("interaction_contract") val interactionContract: Int = 0,
    @SerialName("scoped_reauth") val scopedReauth: Boolean = false)

@Serializable
data class MobileRecord<T>(val id: Long, @SerialName("owner_id") val ownerId: Long, val version: Long, val state: String,
    val data: T, @SerialName("reference_id") val referenceId: Long = 0,
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("created_at") val createdAt: String = "", @SerialName("updated_at") val updatedAt: String = "")

@Serializable
data class MobilePage<T>(val items: List<MobileRecord<T>> = emptyList(), @SerialName("next_cursor") val nextCursor: Long = 0)

@Serializable
data class Audience(val mode: String = "public", @SerialName("user_ids") val userIds: List<Long> = emptyList())

@Serializable
data class PetBody(val name: String, val species: String, val breed: String = "", val birthday: String = "",
    val sex: String = "", val bio: String = "", val health: String = "", val missing: Boolean = false,
    val audience: Audience = Audience(), @SerialName("adoption_available") val adoptionAvailable: Boolean = false, val city: String = "",
    @SerialName("adoption_cycle") val adoptionCycle: Long = 0, @SerialName("placement_state") val placementState: String = "",
    @SerialName("adoption_opened") val adoptionOpened: Boolean = false)


@Serializable
data class EntryBody(val text: String, val space: String = "community", val title: String = "", val category: String = "daily_share",
    val tags: List<String> = emptyList(), @SerialName("pet_id") val petId: Long = 0, val audience: Audience = Audience(),
    @SerialName("content_origin") val contentOrigin: String = "", val city: String = "")

@Serializable
data class CommentBody(val text: String = "", @SerialName("sticker_id") val stickerId: String = "",
    @SerialName("reply_to") val replyTo: Long = 0)

@Serializable
data class EntryMetrics(@SerialName("entry_id") val entryId: Long, val likes: Long, val comments: Long,
    @SerialName("viewer_liked") val viewerLiked: Boolean)

@Serializable
data class AdoptionBody(@SerialName("pet_id") val petId: Long, @SerialName("applicant_id") val applicantId: Long,
    @SerialName("publisher_id") val publisherId: Long, val message: String, val supplement: String = "", val note: String = "",
    @SerialName("trial_terms") val trialTerms: String = "", @SerialName("trial_until") val trialUntil: String? = null,
    @SerialName("trial_confirmations") val trialConfirmations: List<Long> = emptyList(),
    @SerialName("handover_confirmations") val handoverConfirmations: List<Long> = emptyList(),
    @SerialName("completed_at") val completedAt: String? = null,
    val contracts: List<ContractRevision>? = null, @SerialName("effective_contract") val effectiveContract: Long = 0,
    @SerialName("return_plans") val returnPlans: List<ReturnPlan>? = null, @SerialName("effective_return") val effectiveReturn: Long = 0,
    @SerialName("adoption_cycle") val adoptionCycle: Long = 0)

@Serializable
data class ConversationBody(@SerialName("requester_id") val requesterId: Long, @SerialName("recipient_id") val recipientId: Long)

@Serializable
data class GroupBody(val name: String, val notice: String = "", val members: List<Long> = emptyList(), val muted: List<Long> = emptyList(),
    @SerialName("pending_transfer_to") val pendingTransferTo: Long = 0, @SerialName("transfer_expires_at") val transferExpiresAt: String? = null, val discoverable: Boolean = false, val city: String = "", val summary: String = "")

@Serializable
data class MessageBody(val text: String = "", @SerialName("sticker_id") val stickerId: String = "")

@Serializable
data class CaseBody(val type: String, val title: String, val species: String, val description: String, val city: String, val area: String,
    val latitude: Double, val longitude: Double, @SerialName("occurred_at") val occurredAt: String,
    @SerialName("pet_id") val petId: Long = 0, @SerialName("closure_reason") val closureReason: String = "",
    @SerialName("private_location") val privateLocation: String = "", @SerialName("matched_claim_id") val matchedClaimId: Long = 0,
    @SerialName("location_precision") val locationPrecision: String = "approximate")

@Serializable
data class NotificationBody(val category: String, val text: String, val read: Boolean,
    @SerialName("target_kind") val targetKind: String, @SerialName("target_id") val targetId: Long,
    @SerialName("push_state") val pushState: String = "suppressed")

@Serializable
data class InboxSummary(val direct: Map<String, Int> = emptyMap(), val groups: Map<String, Int> = emptyMap(),
    @SerialName("global_total") val globalTotal: Int? = null,
    @SerialName("muted_groups") val mutedGroups: Map<String, Int> = emptyMap(),
    val notifications: Map<String, Int> = emptyMap(), @SerialName("message_requests") val messageRequests: Int = 0,
    val pending: Map<String,Int> = emptyMap(),
    @SerialName("direct_first_unread") val directFirstUnread: Map<String,Long> = emptyMap(),
    @SerialName("group_first_unread") val groupFirstUnread: Map<String,Long> = emptyMap())

@Serializable
data class EmojiItem(val id: String, @SerialName("text") val unicode: String = "", val label: String = "",
    @SerialName("asset_url") val assetUrl: String = "")

@Serializable
data class StickerItem(val id: String, val label: String = "", val column: Int = 0, val row: Int = 0)

@Serializable
data class ExpressionCatalog(val emoji: List<EmojiItem> = emptyList(), val stickers: List<StickerItem> = emptyList())

@Serializable
data class MediaBody(val scope: String, val mime: String, val size: Long, val width: Int, val height: Int,
    @SerialName("known_ai") val knownAI: Boolean = false)

@Serializable
internal data class PasswordLoginInput(val username: String = "", val phone: String = "", val email: String = "", val password: String, val device: String)
@Serializable
internal data class RefreshInput(@SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String, val device: String)
@Serializable
internal data class LogoutInput(val all: Boolean = false)
@Serializable
data class AdoptionInput(@SerialName("pet_id") val petId: Long, val message: String, @SerialName("client_request_id") val clientRequestId: String = "",
    @SerialName("adoption_cycle") val adoptionCycle: Long = 0)
@Serializable
data class AdoptionActionInput(val version: Long, val note: String = "", @SerialName("trial_until") val trialUntil: String? = null,
    @SerialName("client_request_id") val clientRequestId: String = "")
@Serializable
data class EntryInput(val entry: EntryBody, val version: Long = 0, @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("client_request_id") val clientRequestId: String = "", val declaration: PublicationDeclaration = PublicationDeclaration())
@Serializable
data class PetInput(val pet: PetBody, val version: Long = 0, @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("client_request_id") val clientRequestId: String = "")
@Serializable
internal data class ReactionInput(val liked: Boolean)
@Serializable
internal data class CommentInput(val content: MessageBody, @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("reply_to") val replyTo: Long = 0)
@Serializable
internal data class SendMessageInput(val content: MessageBody, @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
internal data class ReadInput(@SerialName("through_message_id") val throughMessageId: Long)
@Serializable
internal data class StateInput(val version: Long, val note: String = "", @SerialName("client_request_id") val clientRequestId: String = "")
@Serializable
internal data class ConversationInput(@SerialName("user_id") val userId: Long, @SerialName("client_request_id") val clientRequestId: String = "")

@Serializable
data class DraftBody(@SerialName("context_kind") val contextKind: String, val text: String,
    val audience: Audience = Audience(), val cursor: Int = 0,
    @SerialName("context_id") val contextId: Long = 0, @SerialName("pet_id") val petId: Long = 0)
@Serializable
internal data class ComposeDraftContent(val schema: Int = 1, val title: String, val text: String, val entry: EntryBody? = null)
@Serializable
internal data class DraftInput(val version: Long, val draft: DraftBody, val key: String = "community-compose-v1",
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList())

data class SelectedPhoto(val name: String, val mime: String, val bytes: ByteArray)
data class PhotoAttachment(val key: String, val name: String, val mediaId: Long? = null,
    val uploading: Boolean = false, val error: String? = null, val mime: String = "image/jpeg")
