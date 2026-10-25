package com.ttkk0000.meowcircle.client

import kotlinx.serialization.SerializationException

/** 只描述读取失败；写入的结果未知／原请求核对仍走原命令状态机。 */
enum class ClientReadFailure { Connection, SignIn, Forbidden, Unavailable, Busy, Service }

class MobileConnectionException(cause: Throwable) : Exception("连接暂时中断", cause)

fun Throwable.readFailure(): ClientReadFailure = when (this) {
    is MobileConnectionException -> ClientReadFailure.Connection
    is MobileApiException -> when (status) {
        401 -> ClientReadFailure.SignIn
        403 -> ClientReadFailure.Forbidden
        404, 410 -> ClientReadFailure.Unavailable
        429 -> ClientReadFailure.Busy
        else -> ClientReadFailure.Service
    }
    is SerializationException, is MobileStorageException, is LocalDraftException -> ClientReadFailure.Service
    else -> ClientReadFailure.Service
}

@OptIn(kotlin.time.ExperimentalTime::class)
fun clientReadTimestamp(): String = kotlin.time.Clock.System.now().toString()

data class ClientStateCopy(val title: String, val detail: String)

fun ClientReadFailure.copyText(): ClientStateCopy = when (this) {
    ClientReadFailure.Connection -> ClientStateCopy("网络没跟上，内容先留着", "已有草稿和阅读位置会保留；当前显示的内容来自上次成功读取，不代表最新状态。")
    ClientReadFailure.SignIn -> ClientStateCopy("登录后继续查看", "登录状态已变化，受保护内容已收起；重新登录后再核对原任务，不会自动重发操作。")
    ClientReadFailure.Forbidden -> ClientStateCopy("当前账号暂不能查看", "访问范围或使用资格可能已变化，受保护内容已收起。你仍可返回或使用安全与隐私求助。")
    ClientReadFailure.Unavailable -> ClientStateCopy("这条信息暂不可继续", "可能已结束、撤回或受限；不把旧预约、旧地址和旧费用继续当有效信息。")
    ClientReadFailure.Busy -> ClientStateCopy("稍等一下再试", "操作较频繁。当前输入与原请求编号保留，请稍后重新读取。")
    ClientReadFailure.Service -> ClientStateCopy("暂时无法读取", "服务暂不可用。当前输入保留；重新读取不会重新提交原操作。")
}
