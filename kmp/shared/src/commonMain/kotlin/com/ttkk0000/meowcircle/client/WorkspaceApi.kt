package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileEdit(val version: Long, val nickname: String, val bio: String, val theme: String, val background: String,
    @SerialName("avatar_media_id") val avatarMediaId: Long? = null)
@Serializable
data class IntroductionEdit(val version: Long, val bio: String, @SerialName("media_ids") val mediaIds: List<Long>,
    @SerialName("client_request_id") val clientRequestId: String)
@Serializable
data class PublicMember(val id: Long, val username: String = "", val nickname: String = "", val bio: String = "")

suspend fun MobileApi.member(id: Long): PublicMember = get("users/$id")
suspend fun MobileApi.editPet(id: Long, input: PetInput): MobileRecord<PetBody> = durablePatch("pets/$id", input,input.clientRequestId)
suspend fun MobileApi.editEntry(id: Long, input: EntryInput): MobileRecord<EntryBody> = durablePatch("entries/$id", input,input.clientRequestId)
suspend fun MobileApi.editCase(id: Long, input: CaseInput): MobileRecord<CaseBody> = durablePatch("cases/$id", input,input.clientRequestId)
suspend fun MobileApi.editOffer(id: Long, input: OfferInput): MobileRecord<OfferBody> = durablePatch("offers/$id", input,input.clientRequestId)
suspend fun MobileApi.introduction(id: Long, input: IntroductionEdit): MobileRecord<PetBody> = durablePost("pets/$id/introduction", input, input.clientRequestId)
suspend fun MobileApi.proposeContract(id: Long, action: String, input: ContractInput): MobileRecord<AdoptionBody> {
    require(action in listOf("propose", "decline"))
    return durablePost("adoptions/$id/contracts/$action", input, input.clientRequestId)
}
suspend fun MobileApi.group(id: Long): MobileRecord<GroupBody> = get("groups/$id")
suspend fun MobileApi.claims(caseId: Long, cursor: Long = 0): MobilePage<ClaimBody> = get("claims", mapOf("reference_id" to "$caseId", "cursor" to "$cursor"))
suspend fun MobileApi.claimRecord(id: Long): MobileRecord<ClaimBody> = get("claims/$id")
suspend fun MobileApi.leads(caseId: Long, cursor: Long = 0): MobilePage<LeadBody> = get("leads", mapOf("reference_id" to "$caseId", "cursor" to "$cursor"))
suspend fun MobileApi.closeCase(id: Long, input: VersionedAction): MobileRecord<CaseBody> {
    val command = if (input.clientRequestId.isBlank()) input.copy(clientRequestId = newRequestKey()) else input
    return durablePost("cases/$id/close",command,command.clientRequestId)
}
suspend fun MobileApi.careUpdates(id: Long, cursor: Long = 0): MobilePage<CareBody> = get("care_updates", mapOf("reference_id" to "$id", "cursor" to "$cursor"))
suspend fun MobileApi.reviewRecord(id: Long): MobileRecord<ReviewBody> = get("reviews/$id")
suspend fun MobileApi.supplementReview(id: Long, input: VersionedAction): MobileRecord<ReviewBody> {
    val command = if (input.clientRequestId.isBlank()) input.copy(clientRequestId = newRequestKey()) else input
    return durablePost("reviews/$id/supplement",command,command.clientRequestId)
}
suspend fun MobileApi.preferences(): MobilePage<PreferenceBody> = get("preferences")
suspend fun MobileApi.expressionState(): MobilePage<ExpressionBody> = get("expressions")
suspend fun MobileApi.subscriptions(): MobilePage<SubscriptionBody> = get("subscriptions")
suspend fun MobileApi.feedPage(cursor: Long = 0, space: String = "community", keyword: String = "", owner: Long = 0): MobilePage<EntryBody> =
    get("entries", mapOf("cursor" to "$cursor", "space" to space, "q" to keyword, "owner_id" to "$owner"))
suspend fun MobileApi.richComment(id: Long, content: MessageBody, key: String, replyTo: Long): MobileRecord<CommentBody> =
    durablePost("entries/$id/comments", CommentInput(content, key, replyTo), key)
suspend fun MobileApi.richMessage(id: Long, group: Boolean, content: MessageBody, media: List<Long>, key: String): MobileRecord<MessageBody> =
    durablePost("${if (group) "groups" else "conversations"}/$id/messages", SendMessageInput(content, key, media), key)
