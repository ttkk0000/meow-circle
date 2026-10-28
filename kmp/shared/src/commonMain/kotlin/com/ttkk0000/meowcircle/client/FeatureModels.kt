package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendBody(@SerialName("requester_id") val requesterId: Long, @SerialName("recipient_id") val recipientId: Long,
    val reason: String = "", @SerialName("accepted_at") val acceptedAt: String? = null, @SerialName("blocked_by") val blockedBy: Long = 0)
@Serializable
data class FriendInput(@SerialName("user_id") val userId: Long, @SerialName("client_request_id") val clientRequestId: String,
    val version: Long = 0, val reason: String = "")

@Serializable
data class Fee(val name: String, @SerialName("amount_cents") val amountCents: Long, val required: Boolean,
    @SerialName("paid_by") val paidBy: String, @SerialName("when") val dueWhen: String)
@Serializable
data class OfferBody(@SerialName("pet_id") val petId: Long, @SerialName("pet_version") val petVersion: Long,
    val nature: String, @SerialName("publisher_type") val publisherType: String, val relationship: String,
    @SerialName("authority_summary") val authoritySummary: String, val custodian: String,
    @SerialName("custodian_relationship") val custodianRelationship: String, val origin: String, val city: String, val area: String,
    @SerialName("venue_type") val venueType: String, val reception: String,
    @SerialName("reception_relationship") val receptionRelationship: String, val payee: String,
    @SerialName("responsible_party") val responsibleParty: String, @SerialName("known_health") val knownHealth: String,
    val aftercare: String, @SerialName("price_cents") val priceCents: Long, val currency: String = "CNY", val fees: List<Fee> = emptyList(),
    @SerialName("total_cents") val totalCents: Long, @SerialName("deposit_terms") val depositTerms: String,
    @SerialName("valid_until") val validUntil: String, @SerialName("private_address") val privateAddress: String = "")
@Serializable
data class OfferInput(val offer: OfferBody, @SerialName("client_request_id") val clientRequestId: String,
    val version: Long = 0, @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
data class DisclosureSnapshot(@SerialName("offer_id") val offerId: Long, @SerialName("offer_version") val offerVersion: Long, val disclosure: OfferBody)
@Serializable
data class AppointmentTerms(@SerialName("starts_at") val startsAt: String, val place: String, val reception: String,
    @SerialName("pet_name") val petName: String, @SerialName("amount_cents") val amountCents: Long,
    @SerialName("visit_fee_cents") val visitFeeCents: Long, @SerialName("required_fees") val requiredFees: String,
    val payee: String, val snapshot: DisclosureSnapshot? = null, val city: String = "", val species: String = "")
@Serializable
data class AppointmentVersion(val number: Long, val terms: AppointmentTerms, @SerialName("proposed_by") val proposedBy: Long,
    val confirmations: List<Long>, val status: String, @SerialName("created_at") val createdAt: String,
    @SerialName("safety_hold_version") val safetyHoldVersion: Long = 0, @SerialName("resolution_note") val resolutionNote: String = "",
    @SerialName("safety_checks") val safetyChecks: Map<String,String> = emptyMap())
@Serializable
data class AppointmentBody(@SerialName("context_kind") val contextKind: String, @SerialName("context_id") val contextId: Long,
    @SerialName("peer_id") val peerId: Long, @SerialName("effective_version") val effectiveVersion: Long,
    @SerialName("pending_version") val pendingVersion: Long, val versions: List<AppointmentVersion>, val reason: String = "",
    @SerialName("safety_hold_version") val safetyHoldVersion: Long = 0, @SerialName("safety_reason") val safetyReason: String = "") {
    val effectiveTerms: AppointmentTerms? get() = versions.firstOrNull { it.number == effectiveVersion && it.status == "effective" }?.terms
    val pendingTerms: AppointmentTerms? get() = versions.firstOrNull { it.number == pendingVersion && it.status == "pending" }?.terms
}
@Serializable
data class AppointmentInput(@SerialName("context_kind") val contextKind: String, @SerialName("context_id") val contextId: Long,
    @SerialName("peer_id") val peerId: Long, val terms: AppointmentTerms, @SerialName("client_request_id") val clientRequestId: String)
@Serializable
data class AppointmentAction(val version: Long, @SerialName("client_request_id") val clientRequestId: String,
    val terms: AppointmentTerms? = null, @SerialName("restore_version") val restoreVersion: Long = 0, val note: String = "")
@Serializable
data class ContractTerms(val city: String, val species: String, val nature: String, val responsibilities: String,
    @SerialName("health_disclosure") val healthDisclosure: String, @SerialName("return_terms") val returnTerms: String,
    @SerialName("amount_cents") val amountCents: Long, val fees: List<Fee> = emptyList(), val payee: String,
    @SerialName("trial_until") val trialUntil: String? = null, @SerialName("offer_id") val offerId: Long = 0,
    @SerialName("offer_version") val offerVersion: Long = 0)
@Serializable
data class ContractRevision(val number: Long, val terms: ContractTerms, val hash: String, @SerialName("proposed_by") val proposedBy: Long,
    @SerialName("accepted_by") val acceptedBy: List<Long>, val status: String, @SerialName("created_at") val createdAt: String)
@Serializable
data class ContractInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String,
    val terms: ContractTerms? = null, val revision: Long = 0, @SerialName("reauth_token") val reauthToken: String = "")
@Serializable
data class ReturnTerms(@SerialName("starts_at") val startsAt: String, val place: String, val reception: String,
    @SerialName("care_until") val careUntil: String, val costs: String, val safety: String)
@Serializable
data class ReturnPlan(val number: Long, val terms: ReturnTerms, @SerialName("proposed_by") val proposedBy: Long,
    val confirmations: List<Long>, val handover: List<Long>, val state: String)
@Serializable
data class ReturnInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String, val revision: Long = 0, val terms: ReturnTerms? = null)
@Serializable
data class CaseInput(val case: CaseBody, @SerialName("client_request_id") val clientRequestId: String, val version: Long = 0,
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
data class ClaimBody(@SerialName("pet_name") val petName: String, val evidence: String,
    @SerialName("publisher_confirmed") val publisherConfirmed: Boolean = false,
    @SerialName("claimant_confirmed") val claimantConfirmed: Boolean = false, val note: String = "")
@Serializable
data class ClaimInput(@SerialName("pet_name") val petName: String, val evidence: String,
    @SerialName("client_request_id") val clientRequestId: String, @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
data class VersionedAction(val version: Long, val note: String = "", @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("client_request_id") val clientRequestId: String = "")
@Serializable
data class LeadBody(val text: String, val assessment: String = "", @SerialName("reviewed_by") val reviewedBy: Long = 0,
    @SerialName("reviewed_at") val reviewedAt: String? = null)
@Serializable
data class LeadAssessmentInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String, val assessment: String)
@Serializable
data class LeadContactInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String)
@Serializable
data class LeadInput(val text: String, @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable data class LeadSubmissionInput(val version: Long, val text: String, @SerialName("media_ids") val mediaIds: List<Long> = emptyList(),
    @SerialName("client_request_id") val clientRequestId: String)
@Serializable
data class CareBody(val text: String, @SerialName("needs_help") val needsHelp: Boolean)
@Serializable
data class CareContext(@SerialName("adoption_id") val adoptionId: Long, val state: String,
    @SerialName("can_post") val canPost: Boolean, val reason: String)
@Serializable
data class CareInput(val text: String, @SerialName("needs_help") val needsHelp: Boolean,
    @SerialName("client_request_id") val clientRequestId: String, @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
data class GroupInput(val name: String, @SerialName("client_request_id") val clientRequestId: String, val discoverable: Boolean = false, val city: String = "", val summary: String = "")
@Serializable
data class GroupAction(val version: Long, @SerialName("client_request_id") val clientRequestId: String,
    val notice: String = "", @SerialName("user_id") val userId: Long = 0, val discoverable: Boolean = false, val city: String = "", val summary: String = "")
@Serializable
data class InvitationBody(val scope: String, @SerialName("target_id") val targetId: Long,
    @SerialName("recipient_id") val recipientId: Long, @SerialName("expires_at") val expiresAt: String, val note: String = "")
@Serializable
data class InviteInput(val scope: String, @SerialName("target_id") val targetId: Long,
    @SerialName("user_id") val userId: Long, @SerialName("client_request_id") val clientRequestId: String)
@Serializable
data class PreferenceBody(@SerialName("direct_messages") val directMessages: Boolean = true, val groups: Boolean = true,
    val applications: Boolean = true, val interactions: Boolean = true, val frequency: String = "instant",
    @SerialName("push_permission") val pushPermission: Boolean = false, val content: ContentSubscriptions? = null)
@Serializable
data class ExpressionBody(val favorites: List<String> = emptyList(), val recent: List<String> = emptyList())
@Serializable
data class SubscriptionBody(val city: String, val latitude: Double, val longitude: Double,
    @SerialName("radius_km") val radiusKm: Double, val frequency: String, val enabled: Boolean,
    @SerialName("location_precision") val locationPrecision: String = "approximate", val species: String = "",
    @SerialName("coordinate_system") val coordinateSystem: String = "")
@Serializable
data class SubscriptionInput(val subscription: SubscriptionBody, val version: Long = 0)
@Serializable
data class SettingsInput(val version: Long, val preference: PreferenceBody? = null, val expressions: ExpressionBody? = null)
@Serializable
data class ReviewBody(val type: String, @SerialName("target_kind") val targetKind: String = "",
    @SerialName("target_id") val targetId: Long = 0, val reason: String = "", val check: String = "", val summary: String = "",
    @SerialName("valid_until") val validUntil: String? = null)
@Serializable
data class ReviewInput(val type: String, val reason: String, val check: String = "", @SerialName("client_request_id") val clientRequestId: String = "",
    @SerialName("target_kind") val targetKind: String = "", @SerialName("target_id") val targetId: Long = 0,
    @SerialName("appeal_of") val appealOf: Long = 0, @SerialName("media_ids") val mediaIds: List<Long> = emptyList())
@Serializable
data class FeedbackInput(@SerialName("appointment_id") val appointmentId: Long, @SerialName("appointment_version") val version: Long,
    val items: Map<String,String>, val note: String = "")

@Serializable
data class ContentSubscriptions(@SerialName("followed_users") val followedUsers: List<Long> = emptyList(), @SerialName("watched_cases") val watchedCases: List<Long> = emptyList())
@Serializable
data class ContentSubscriptionInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String, @SerialName("followed_users") val followedUsers: List<Long>, @SerialName("watched_cases") val watchedCases: List<Long>)
@Serializable
data class GroupDirectoryBody(val name: String, val city: String = "", val summary: String = "", @SerialName("member_count") val memberCount: Int = 0, val member: Boolean = false)
@Serializable
data class GroupJoinInput(@SerialName("client_request_id") val clientRequestId: String, val note: String = "")
