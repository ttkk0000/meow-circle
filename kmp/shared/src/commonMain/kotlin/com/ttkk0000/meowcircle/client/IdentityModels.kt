package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderCredential(val code: String = "", @SerialName("identity_token") val identityToken: String = "",
    @SerialName("access_token") val accessToken: String = "")
@Serializable
data class ProviderStart(val provider: String, val resume: String = "", val scope: PurposeScope? = null)
@Serializable
data class ProviderChallenge(val state: String, val nonce: String, @SerialName("expires_at") val expiresAt: String)
@Serializable
data class ProviderFinish(val state: String, val credential: ProviderCredential, val cancelled: Boolean = false)
@Serializable
data class ProviderResult(val login: LoginSession? = null, @SerialName("resolution_token") val resolutionToken: String = "",
    val resume: String = "", @SerialName("needs_account_choice") val needsAccountChoice: Boolean = false,
    @SerialName("reauth_token") val reauthToken: String = "")
@Serializable
data class ProviderResolve(val token: String, val create: Boolean = false, @SerialName("accept_terms") val acceptTerms: Boolean = false,
    @SerialName("reauth_token") val reauthToken: String = "", val device: String = "")
@Serializable
data class LinkedIdentity(val id: Long, val provider: String)
@Serializable
data class PhoneLoginInput(val phone: String, val code: String, val device: String = "")
@Serializable
data class PhoneRecoveryInput(val phone: String, val code: String, val password: String)
@Serializable
data class IdentityMethods(val phone: String = "", @SerialName("password_set") val passwordSet: Boolean,
    val linked: List<LinkedIdentity> = emptyList(), @SerialName("available_providers") val availableProviders: List<String> = emptyList(),
    @SerialName("sms_available") val smsAvailable: Boolean = false)
@Serializable
data class VerificationSummary(val id: Long, val check: String, val state: String, val summary: String,
    @SerialName("target_version") val targetVersion: Long, @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("valid_until") val validUntil: String? = null)
