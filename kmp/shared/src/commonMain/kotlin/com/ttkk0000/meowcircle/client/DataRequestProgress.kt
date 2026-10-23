package com.ttkk0000.meowcircle.client

enum class DataRequestPhase { Processing, Held, Assistance, RetainedCopies, Complete, NeedsReconciliation }

data class DataRequestProgress(
    val phase: DataRequestPhase,
    val message: String,
    val reviewRequired: Boolean,
    val nextReviewAt: String?,
    val isFinal: Boolean = false,
)

/** A database result or a queued attachment is never displayed as full erasure. */
fun DataRequestStatus.progress(): DataRequestProgress {
    val countsValid = progressVersion == 1 && listOf(pending, held, erased, physicalPending, providerPending, restrictedCopies).all { it >= 0 } &&
        physicalPending <= pending && providerPending <= pending - physicalPending
    val externalComplete = externalState in listOf("not_required", "completed")
    val final = countsValid && state == "completed" && pending == 0 && held == 0 &&
        pendingFields.isEmpty() && physicalPending == 0 && providerPending == 0 && restrictedCopies == 0 && externalComplete
    val phase = when {
        !countsValid || state !in listOf("processing", "partially_held", "needs_assistance", "completed") -> DataRequestPhase.NeedsReconciliation
        final -> DataRequestPhase.Complete
        state == "completed" && restrictedCopies > 0 && pending == 0 && held == 0 && pendingFields.isEmpty() && externalComplete -> DataRequestPhase.RetainedCopies
        state == "completed" -> DataRequestPhase.NeedsReconciliation
        state == "needs_assistance" -> DataRequestPhase.Assistance
        held > 0 -> DataRequestPhase.Held
        else -> DataRequestPhase.Processing
    }
    val message = when (phase) {
        DataRequestPhase.Complete -> "本次选定范围已处理完成"
        DataRequestPhase.NeedsReconciliation -> "进度信息待核对，请重新查询原请求"
        DataRequestPhase.Assistance -> "事项协助已受理，尚未执行删除"
        DataRequestPhase.RetainedCopies -> "本次请求已处理，仍有受限副本保留"
        DataRequestPhase.Held -> if (reviewRequired) "部分资料仍在保全，已到复核时间" else "部分资料仍在保全，其余资料分别处理"
        DataRequestPhase.Processing -> when {
            physicalPending > 0 -> "附件清理尚待确认"
            providerPending > 0 -> "账号关联的外部授权仍待处理"
            !externalComplete -> "外部副本处理仍待回执"
            else -> "选定资料正在处理"
        }
    }
    return DataRequestProgress(phase, message, reviewRequired, nextReviewAt, final)
}
