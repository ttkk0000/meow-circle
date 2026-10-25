package com.ttkk0000.meowcircle.kmpapp.mobile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 单个官方SDK授权并发槽；回调按原state匹配，Activity及凭据不持久化。 */
internal object AndroidProviderAuth {
    private data class Pending(val provider: String, var continuation: CancellableContinuation<ProviderCredential?>?,
        var activity: WeakReference<Activity>? = null, var listener: IUiListener? = null)
    private val lock = Any()
    private val pending = mutableMapOf<String,Pending>()
    val providers: Set<String> get() = buildSet {
        if (BuildConfig.WECHAT_APP_ID != "wx0000000000000000") add("wechat")
        if (BuildConfig.QQ_APP_ID.matches(Regex("[1-9][0-9]{4,19}"))) add("qq")
    }
    suspend fun authorize(activity: Activity, provider: String, challenge: ProviderChallenge): ProviderCredential? = suspendCancellableCoroutine { continuation ->
        if (provider !in providers) { continuation.resumeWithException(MobileApiException(503,"provider AppID placeholder")); return@suspendCancellableCoroutine }
        synchronized(lock) {
            if(pending.isNotEmpty()) { continuation.resumeWithException(MobileApiException(409,"another official authorization pending","provider_pending")); return@suspendCancellableCoroutine }
            pending[challenge.state] = Pending(provider,continuation)
        }
        continuation.invokeOnCancellation {
            val original = synchronized(lock) {
                // QQ回调不带原state。保留原监听及并发槽直到SDK返回，避免旧SSO回跳落入下一次授权。
                pending[challenge.state]?.also {
                    it.continuation = null
                    if(it.provider != "qq" || it.listener == null) pending.remove(challenge.state)
                }
            }
            original?.activity?.get()?.let { it.runOnUiThread { it.finish() } }
        }
        try {
            activity.startActivity(Intent(activity, OfficialProviderActivity::class.java).putExtra("provider",provider).putExtra("state",challenge.state))
        } catch (failure: Exception) { fail(provider,challenge.state) }
    }
    fun attach(provider: String, state: String, activity: Activity): Boolean = synchronized(lock) {
        val operation = pending[state]?.takeIf { it.provider == provider } ?: return@synchronized false
        operation.activity = WeakReference(activity); true
    }
    fun has(provider: String): Boolean = synchronized(lock) { pending.values.any { it.provider == provider } }
    fun listener(state: String): IUiListener? = synchronized(lock) { pending[state]?.listener }
    fun keepListener(state: String, listener: IUiListener) { synchronized(lock) { pending[state]?.listener = listener } }
    fun complete(provider: String, state: String, credential: ProviderCredential?): Boolean {
        val operation = synchronized(lock) {
            pending[state]?.takeIf { it.provider == provider }?.also { pending.remove(state) }
        } ?: return false
        operation.activity?.get()?.finish()
        operation.continuation?.let { if(it.isActive) it.resume(credential) }
        return true
    }
    fun fail(provider: String, state: String) {
        val operation = synchronized(lock) { pending[state]?.takeIf { it.provider == provider }?.also { pending.remove(state) } } ?: return
        operation.activity?.get()?.finish()
        operation.continuation?.let { if(it.isActive) it.resumeWithException(MobileApiException(503,"official provider authorization failed")) }
    }
}

/** 仅在用户同意本次官方授权后启动。SDK回跳以进程中的原state为依据。 */
class OfficialProviderActivity : ComponentActivity() {
    private var provider = ""
    private var state = ""
    private var qqListener: IUiListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = intent.getStringExtra("provider").orEmpty(); state = intent.getStringExtra("state").orEmpty()
        if(!AndroidProviderAuth.attach(provider,state,this)) { finish(); return }
        qqListener = AndroidProviderAuth.listener(state)
        if(savedInstanceState != null) return
        try { when(provider) {
            "wechat" -> {
                val api = WXAPIFactory.createWXAPI(this,BuildConfig.WECHAT_APP_ID,true)
                api.registerApp(BuildConfig.WECHAT_APP_ID)
                if(!api.isWXAppInstalled || !api.sendReq(SendAuth.Req().apply { scope = "snsapi_userinfo"; this.state = this@OfficialProviderActivity.state })) AndroidProviderAuth.fail(provider,state)
            }
            "qq" -> {
                Tencent.setIsPermissionGranted(true,android.os.Build.MODEL)
                val sdk = Tencent.createInstance(BuildConfig.QQ_APP_ID,applicationContext)
                val requestProvider = provider
                val requestState = state
                val listener = object : IUiListener {
                    override fun onComplete(response: Any?) {
                        val code = (response as? JSONObject)?.optString("access_token").orEmpty()
                        // 官方Server-Side模式把授权code放在access_token字段，不能当作普通令牌。
                        if(code.isBlank()) AndroidProviderAuth.fail(requestProvider,requestState) else AndroidProviderAuth.complete(requestProvider,requestState,ProviderCredential(code = code))
                    }
                    override fun onError(error: UiError?) { AndroidProviderAuth.fail(requestProvider,requestState) }
                    override fun onCancel() { AndroidProviderAuth.complete(requestProvider,requestState,null) }
                    override fun onWarning(code: Int) { }
                }
                qqListener = listener
                AndroidProviderAuth.keepListener(state,listener)
                if(sdk == null || sdk.loginServerSide(this,"get_simple_userinfo",listener) < 0) AndroidProviderAuth.fail(provider,state)
            }
            else -> AndroidProviderAuth.fail(provider,state)
        } } catch (failure: Exception) { AndroidProviderAuth.fail(provider,state) }
    }
    @Deprecated("官方SDK仍通过Activity结果转发")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        qqListener?.let { Tencent.onActivityResultData(requestCode,resultCode,data,it) }
    }
    override fun onDestroy() { qqListener = null; super.onDestroy() }
}
