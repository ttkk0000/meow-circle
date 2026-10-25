package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class DevicePushStatus(val state: String = "unavailable", val message: String = "当前设备通道尚未配置")
private data class DeviceChannel(val token: String = "", val permission: String = "unknown", val available: Boolean = false)

/** 应用级单一协调器：系统权限、已保存偏好、账号与Token共同决定绑定，不保存Token明文到普通文件。 */
class DevicePushCoordinator internal constructor(private val api: MobileApi, private val platform: String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = MutableStateFlow(DeviceChannel())
    private val refresh = MutableStateFlow(0L)
    private val mutableStatus = MutableStateFlow(DevicePushStatus())
    val status = mutableStatus.asStateFlow()
    private val mutableConsent = MutableStateFlow(false)
    val consent = mutableConsent.asStateFlow()
    private val mutableBadge = MutableStateFlow(0)
    val badge = mutableBadge.asStateFlow()
    private var owner: Long? = null

    init {
        if (platform in setOf("android", "ios")) scope.launch {
            combine(api.user.map { it?.id }.distinctUntilChanged(), channel, refresh) { user, device, _ -> user to device }
                .collectLatest { (user, device) ->
                    if (owner != user) { owner = user; mutableConsent.value = false; mutableBadge.value = 0 }
                    if (user == null) { mutableStatus.value = DevicePushStatus("signed_out", "登录后才会绑定设备通道"); return@collectLatest }
                    for (attempt in 0..2) {
                        try {
                            withTimeout(20_000L) {
                                val preferences = api.preferences().items.firstOrNull()?.data
                                val consent = preferences?.pushPermission == true && preferences.frequency != "off"
                                mutableConsent.value = consent
                                if (!consent || device.permission == "denied") {
                                    // 重启后可能没有旧Token，仍按当前会话原子撤销服务端绑定。
                                    api.post<JsonElement,JsonObject>("push-devices/disable", buildJsonObject {})
                                    mutableStatus.value = DevicePushStatus("disabled", "设备推送已关闭")
                                    return@withTimeout
                                }
                                if (!device.available) { mutableStatus.value = DevicePushStatus(); return@withTimeout }
                                if (device.token.isBlank()) {
                                    mutableStatus.value = DevicePushStatus("waiting", if (consent) "正在等待设备通道注册" else "未启用设备推送")
                                    return@withTimeout
                                }
                                val enabled = consent && device.permission == "granted"
                                val current: PushDeviceReceipt = api.post("push-devices/lookup", PushDeviceLookup(platform, device.token))
                                if (!enabled && current.id == 0L) { mutableStatus.value = DevicePushStatus("disabled", "设备推送已关闭"); return@withTimeout }
                                val receipt = api.registerPushDevice(PushDeviceInput(platform, device.token, device.permission, enabled, current.version))
                                mutableStatus.value = DevicePushStatus(receipt.state, if (receipt.state == "enabled") "设备通道已登记，送达仍由系统与供应商决定" else "设备推送已关闭")
                            }
                            break
                        } catch (_: TimeoutCancellationException) {
                            mutableStatus.value = DevicePushStatus("failed", "设备登记等待超时；下次会先核对原绑定")
                            if (attempt < 2) delay((attempt + 1) * 2_000L)
                        } catch (failure: CancellationException) { throw failure } catch (failure: Exception) {
                            mutableStatus.value = DevicePushStatus("failed", failure.mobileMessage())
                            if (failure is MobileApiException && failure.status in setOf(401, 403)) { mutableConsent.value = false; break }
                            if (attempt < 2) delay((attempt + 1) * 2_000L)
                        }
                    }
                }
        }
    }

    fun updateChannel(token: String, permission: String, available: Boolean) {
        require(permission in setOf("unknown", "granted", "denied"))
        require(token.length <= 4096)
        channel.value = DeviceChannel(token, permission, available)
    }
    fun refresh() {
        refresh.update { it + 1 }
        scope.launch { try {
            val user = api.user.value?.id
            if (user != null) { val summary = api.inbox(); if (api.user.value?.id == user) updateBadge(summary.globalTotal) }
        } catch (failure: CancellationException) { throw failure } catch (_: Exception) { /* 未读失败不假清零。 */ } }
    }
    fun channelFailed() { mutableStatus.value = DevicePushStatus("failed", "系统设备通道暂不可用，请稍后重试；站内消息仍可查看") }
    fun preferencesSaved(preference: PreferenceBody) {
        if (!preference.pushPermission || preference.frequency == "off") mutableConsent.value = false
        refresh()
    }
    fun updateBadge(total: Int?) { if (total != null && total >= 0) mutableBadge.value = total.coerceAtMost(9999) }
    fun close() { scope.cancel() }
}
