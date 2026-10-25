@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ttkk0000.meowcircle.iosui

import com.ttkk0000.meowcircle.client.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class IosProviderResult(val kind: String, val state: String, val code: String = "", val identityToken: String = "")
interface IosProviderFactory {
    fun configured(provider: String): Boolean
    fun start(provider: String, state: String, nonce: String, controller: UIViewController, onResult: (IosProviderResult) -> Unit)
    fun cancel(state: String)
}
object IosProviderPlatform {
    var factory: IosProviderFactory? = null
}

internal suspend fun authorizeIosProvider(provider: String, challenge: ProviderChallenge, controller: UIViewController): ProviderCredential? =
    suspendCancellableCoroutine { continuation ->
        val factory = IosProviderPlatform.factory
        if(factory?.configured(provider) != true) {
            continuation.resumeWithException(MobileApiException(503,"official provider AppID placeholder"))
            return@suspendCancellableCoroutine
        }
        continuation.invokeOnCancellation { dispatch_async(dispatch_get_main_queue()) { factory.cancel(challenge.state) } }
        factory.start(provider,challenge.state,challenge.nonce,controller) { result ->
            if(continuation.isActive && result.state == challenge.state) when(result.kind) {
                "success" -> if(result.code.isNotBlank() || result.identityToken.isNotBlank()) continuation.resume(ProviderCredential(code = result.code,identityToken = result.identityToken)) else continuation.resumeWithException(MobileApiException(503,"empty official provider credential"))
                "cancelled" -> continuation.resume(null)
                "pending" -> continuation.resumeWithException(MobileApiException(409,"previous official authorization pending","provider_pending"))
                else -> continuation.resumeWithException(MobileApiException(503,"official provider authorization failed"))
            }
        }
    }
