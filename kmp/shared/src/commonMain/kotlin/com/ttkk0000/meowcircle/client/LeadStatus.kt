package com.ttkk0000.meowcircle.client

enum class LeadAssessment { Pending, Helpful, NotMatch, Unknown }
fun leadAssessment(value: String): LeadAssessment = when(value) {
    "", "pending" -> LeadAssessment.Pending
    "helpful" -> LeadAssessment.Helpful
    "not_match" -> LeadAssessment.NotMatch
    else -> LeadAssessment.Unknown
}
fun leadAssessmentLabel(value: String): String = when(leadAssessment(value)) {
    LeadAssessment.Pending -> "待核实"
    LeadAssessment.Helpful -> "有帮助"
    LeadAssessment.NotMatch -> "不匹配"
    LeadAssessment.Unknown -> "处理状态待核对"
}
fun leadAssessmentSummary(value: String): String = when(leadAssessment(value)) {
    LeadAssessment.Pending -> "不公开精确位置，不改变目击者的原始描述。"
    LeadAssessment.Helpful -> "仍不等于找回；不改写对方原话，不自动公开其位置。"
    LeadAssessment.NotMatch -> "不是对目击者作违规认定，后续仍可继续核实。"
    LeadAssessment.Unknown -> "尚未读到支持的处理状态，请重新读取；原线索保持不变。"
}
fun leadReviewMinimumVersion(kind: String): Long? = if(kind.startsWith("reviewed-"))
    kind.removePrefix("reviewed-").toLongOrNull()?.takeIf { it > 0 } else null
fun leadSubmissionMinimumVersion(kind: String): Long? = if(kind.startsWith("submitted-"))
    kind.removePrefix("submitted-").toLongOrNull()?.takeIf { it > 0 } else null
fun leadSubmissionRecorded(lead: MobileRecord<LeadBody>?, actor: Long?, minimum: Long): Boolean =
    minimum > 0 && lead != null && actor != null && lead.ownerId == actor && lead.version >= minimum && lead.state == "submitted"
fun leadAssessmentRecorded(lead: MobileRecord<LeadBody>?, parent: MobileRecord<CaseBody>?, minimumVersion: Long): Boolean =
    lead != null && parent != null && lead.version >= minimumVersion && lead.referenceId == parent.id &&
        leadAssessment(lead.data.assessment) in listOf(LeadAssessment.Helpful,LeadAssessment.NotMatch) &&
        lead.data.reviewedBy == parent.ownerId && !lead.data.reviewedAt.isNullOrBlank()
/** 只预测入口，提交仍由服务端实时校验资格、双方关系、版本和求助状态。 */
fun canContactLead(lead: MobileRecord<LeadBody>, parent: MobileRecord<CaseBody>, actor: Long?): Boolean =
    actor != null && actor > 0 && lead.referenceId == parent.id && parent.state in listOf("active","matched") &&
        lead.state !in listOf("deleted","removed") && lead.ownerId != parent.ownerId && actor in listOf(lead.ownerId,parent.ownerId)
fun canAssessLead(lead: MobileRecord<LeadBody>, parent: MobileRecord<CaseBody>, actor: Long?): Boolean =
    actor != null && actor > 0 && actor == parent.ownerId && lead.referenceId == parent.id &&
        parent.state in listOf("active","matched") && lead.state !in listOf("deleted","removed") && leadAssessment(lead.data.assessment) != LeadAssessment.Unknown
