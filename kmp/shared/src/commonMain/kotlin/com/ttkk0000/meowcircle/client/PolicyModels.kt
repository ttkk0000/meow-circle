package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

// Uniqueness key only; security capabilities always come from the server CSPRNG.
fun newRequestKey(): String = Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

@Serializable
data class Eligibility(val interaction: Boolean = false, val adult: Boolean = false,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("age_band") val ageBand: String = "unknown", val reasons: List<String> = emptyList(),
    @SerialName("contract_version") val contractVersion: Int = 0)

@Serializable
data class PurposeScope(val purpose: String, @SerialName("resource_type") val resourceType: String,
    @SerialName("resource_id") val resourceId: Long, @SerialName("resource_version") val resourceVersion: Long,
    @SerialName("scope_hash") val scopeHash: String, val role: String)
@Serializable
data class ReauthInput(val scope: PurposeScope, val password: String = "", val code: String = "")
@Serializable
data class PurposeGrant(@SerialName("reauth_token") val reauthToken: String)
@Serializable
data class OTPReceipt(val id: Long, @SerialName("expires_at") val expiresAt: String,
    @SerialName("retry_after_seconds") val retryAfterSeconds: Int)
@Serializable
data class PhoneOTPInput(val phone: String, val purpose: String)
@Serializable
data class EmailOTPInput(val email: String, val purpose: String)
@Serializable
data class EmailAuthInput(val email: String, val code: String, val username: String = "", val password: String = "",
    val device: String = "", @SerialName("accept_terms") val acceptTerms: Boolean = false)
@Serializable
data class AccountInput(val version: Long, @SerialName("client_request_id") val clientRequestId: String,
    val phone: String = "", val email: String = "", val password: String = "", val code: String = "",
    @SerialName("identity_id") val identityId: Long = 0, @SerialName("reauth_token") val reauthToken: String = "")
@Serializable
data class OperationReceipt(@SerialName("client_request_id") val clientRequestId: String, val command: String,
    @SerialName("object_id") val objectId: Long, @SerialName("result_kind") val resultKind: String,
    @SerialName("result_id") val resultId: Long, @SerialName("result_version") val resultVersion: Long, val state: String)
@Serializable
data class PublicationDeclaration(@SerialName("manifest_hash") val manifestHash: String = "", val origin: String = "unknown", val confirmed: Boolean = false)
@Serializable
data class PublicationPreview(@SerialName("manifest_hash") val manifestHash: String, val origin: String,
    @SerialName("requires_confirmation") val requiresConfirmation: Boolean)
@Serializable
data class GuardianBody(@SerialName("child_id") val childId: Long, val scopes: List<String>, @SerialName("valid_until") val validUntil: String,
    @SerialName("policy_version") val policyVersion: String)
@Serializable
data class GuardianDecision(val version: Long, val consent: Boolean, @SerialName("client_request_id") val clientRequestId: String,
    @SerialName("reauth_token") val reauthToken: String = "")
@Serializable
data class DataSelection(val kind: String, val id: Long)
@Serializable
data class DataRequestInput(val type: String, @SerialName("client_request_id") val clientRequestId: String,
    @SerialName("query_token") val queryToken: String, val fields: List<String> = emptyList(), val selections: List<DataSelection> = emptyList(),
    val note: String = "", @SerialName("reauth_token") val reauthToken: String = "")
@Serializable
data class DataRequestStatus(val id: Long, val version: Long, val type: String, val state: String, val pending: Int, val held: Int,
    @SerialName("external_state") val externalState: String, val erased: Int = 0,
    @SerialName("physical_pending") val physicalPending: Int = 0, @SerialName("provider_pending") val providerPending: Int = 0,
    @SerialName("pending_fields") val pendingFields: List<String> = emptyList(),
    @SerialName("next_review_at") val nextReviewAt: String? = null, @SerialName("review_required") val reviewRequired: Boolean = false,
    @SerialName("progress_version") val progressVersion: Int = 0, @SerialName("restricted_copies") val restrictedCopies: Int = 0)
@Serializable
data class DataItem(val kind: String, val id: Long, val state: String)
@Serializable
data class DataHold(val kind: String, val id: Long, val fields: List<String>, val basis: String, val responsible: String,
    @SerialName("review_at") val reviewAt: String, val released: Boolean)
@Serializable
data class DataRequestBody(val type: String, val fields: List<String>, val items: List<DataItem>, val holds: List<DataHold>,
    val note: String = "", @SerialName("external_state") val externalState: String,
    @SerialName("erased_fields") val erasedFields: List<String> = emptyList(), @SerialName("progress_version") val progressVersion: Int = 0)
@Serializable
data class SafetyAccess(val token: String, @SerialName("expires_at") val expiresAt: String)
@Serializable
data class RightsInput(val type: String, @SerialName("client_request_id") val clientRequestId: String,
    val reason: String, @SerialName("target_kind") val targetKind: String = "", @SerialName("target_id") val targetId: Long = 0,
    @SerialName("related_case_id") val relatedCaseId: Long = 0, val version: Long = 0,
    @SerialName("right_type") val rightType: String = "", @SerialName("legal_name") val legalName: String = "",
    val statement: String = "", @SerialName("evidence_description") val evidenceDescription: String = "",
    @SerialName("media_ids") val mediaIds: List<Long> = emptyList(), @SerialName("reauth_token") val reauthToken: String = "", val confirmed: Boolean = false)
@Serializable
data class PushDeviceInput(val platform: String, val token: String, val permission: String, val enabled: Boolean, val version: Long = 0)
@Serializable
data class PushDeviceReceipt(val id: Long, val version: Long, val state: String)
@Serializable
data class RightsReceipt(val id: Long, val version: Long, val state: String, val type: String, @SerialName("review_due") val reviewDue: String,
    @SerialName("evidence_state") val evidenceState: String = "unknown")

@Serializable
internal data class DurableCommand(val owner: Long, val id: String, val path: String, val body: String,
    val phase: String = "prepared", val receipt: OperationReceipt? = null, val replayable: Boolean = true,
    val method: String = "POST", val receiptHeader: Boolean = false, val uncertainReplay: Boolean = false)
@Serializable
internal data class ClientVault(val schema: Int = 1, val session: LoginSession? = null, val commands: List<DurableCommand> = emptyList(),
    val limitedAccess: List<RestrictedAccess> = emptyList())
@Serializable
data class RestrictedAccess(val type: String, val clientRequestId: String, val token: String, val id: Long = 0)

data class CommandRecovery(val clientRequestId: String, val command: String, val phase: String, val receipt: OperationReceipt? = null,
    val replayable: Boolean = true)
class OperationResultUnknown(val clientRequestId: String) : Exception("Command outcome requires reconciliation")

@Serializable
data class PushDeviceLookup(val platform: String, val token: String)
