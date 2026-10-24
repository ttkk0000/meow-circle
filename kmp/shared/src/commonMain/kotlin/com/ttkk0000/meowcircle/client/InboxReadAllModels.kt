package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InboxSnapshotInput(@SerialName("client_request_id") val clientRequestId: String)

@Serializable
data class InboxReadAllInput(@SerialName("snapshot_id") val snapshotId: Long, val version: Long,
    @SerialName("client_request_id") val clientRequestId: String)

@Serializable
data class InboxReadSnapshot(@SerialName("snapshot_id") val snapshotId: Long, val version: Long,
    val count: Int, @SerialName("muted_count") val mutedCount: Int, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class InboxReadAllResult(@SerialName("snapshot_id") val snapshotId: Long,
    @SerialName("applied_count") val appliedCount: Int, @SerialName("skipped_count") val skippedCount: Int)

data class InboxReadAllState(val snapshot: InboxReadSnapshot? = null, val preparing: Boolean = false,
    val submitting: Boolean = false, val error: String? = null, val resultUnknown: Boolean = false,
    val prepareRequestId: String = "", val confirmRequestId: String = "", val visible: Boolean = false,
    val recoveredConfirmation: InboxReadAllInput? = null) {
    val canRetry: Boolean get() = !preparing && !submitting && (error != null || resultUnknown)
    fun hidden(): InboxReadAllState = if (preparing || submitting) this else copy(visible = false)
}

internal data class InboxReadAllRecovery(val requestId: String, val confirmation: InboxReadAllInput?)

/** Keep the current reminder policy; muted counts are exposed separately for
 * confirmation, rather than silently changing an unconfirmed product policy. */
val InboxSummary.reminderTotal: Int get() = direct.values.sum() + groups.values.sum() +
    notifications.filterKeys { it !in listOf("direct_messages", "groups") }.values.sum()
