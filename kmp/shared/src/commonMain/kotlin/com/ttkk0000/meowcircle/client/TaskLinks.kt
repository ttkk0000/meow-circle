package com.ttkk0000.meowcircle.client

data class TaskLinkTarget(val kind: String, val id: Long) {
    val isPublic: Boolean get() = kind in publicTaskLinkKinds
}
data class PendingClientTaskLink(val target: TaskLinkTarget, val owner: Long)
val publicTaskLinkKinds = setOf("pets", "entries", "cases", "offers")
val taskLinkKinds = publicTaskLinkKinds + setOf("adoptions", "appointments", "claims", "reviews", "groups", "conversations", "invitations", "leads")

/** 完整匹配原始链接，不解码路径，不接受查询、凭据、片段、写动作或非规范编号。 */
fun parseTaskLink(raw: String, trustedBaseUrl: String): TaskLinkTarget? {
    if (raw.length > 2048) return null
    val custom = Regex("^mnd://([a-z]+)/([1-9][0-9]{0,18})$").matchEntire(raw)
    val parts = if (custom != null) custom.groupValues.drop(1) else {
        val link = Regex("^https://([^/?#\\s]+)/share/([a-z]+)/([1-9][0-9]{0,18})$").matchEntire(raw) ?: return null
        val base = Regex("^https://([^/?#\\s]+)(?:/[^?#\\s]*)?$").matchEntire(trustedBaseUrl) ?: return null
        val origin = httpsAuthority(link.groupValues[1]) ?: return null
        if (origin != httpsAuthority(base.groupValues[1])) return null
        if (link.groupValues[2] !in publicTaskLinkKinds) return null
        link.groupValues.drop(2)
    }
    val kind = parts[0]
    val id = parts[1].toLongOrNull()?.takeIf { it > 0 && it.toString() == parts[1] } ?: return null
    return if (kind in taskLinkKinds) TaskLinkTarget(kind, id) else null
}
private fun httpsAuthority(raw: String): Pair<String, Int>? {
    val match = Regex("^([A-Za-z0-9.-]+|\\[[0-9A-Fa-f:]+\\])(?::([1-9][0-9]{0,4}))?$").matchEntire(raw) ?: return null
    val port = match.groupValues[2].ifBlank { "443" }.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
    return match.groupValues[1].lowercase() to port
}

fun taskLinkDestination(target: TaskLinkTarget): MobileDestination? = when (target.kind) {
    "pets" -> MobileDestination.Pet(target.id)
    "entries" -> MobileDestination.Entry(target.id)
    "cases" -> MobileDestination.Case(target.id)
    "adoptions" -> MobileDestination.Application(target.id)
    "groups" -> MobileDestination.Chat(target.id, true)
    "conversations" -> MobileDestination.Chat(target.id)
    else -> null
}
fun taskLinkFeature(target: TaskLinkTarget): FeatureRoute? = when (target.kind) {
    "offers" -> FeatureRoute(FeaturePage.Offer, target.id)
    "appointments" -> FeatureRoute(FeaturePage.Appointment, target.id)
    "claims" -> FeatureRoute(FeaturePage.Claim, target.id)
    "reviews" -> FeatureRoute(FeaturePage.Review, target.id)
    "invitations" -> FeatureRoute(FeaturePage.Invitations, target.id)
    "leads" -> FeatureRoute(FeaturePage.Lead, target.id)
    else -> null
}
