package com.ttkk0000.meowcircle.kmpapp.mobile

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.clientui.*
import com.ttkk0000.meowcircle.kmpapp.R

/** 安全拉起外部设置或网页；存在存活宿主 Activity 时沿用当前任务返回栈，避免分屏跳栈。 */
internal fun Context.startExternalActivity(intent: Intent): Boolean = runCatching {
    val host = findActivity()?.takeUnless { it.isFinishing || it.isDestroyed }
    if (host != null) host.startActivity(intent)
    else startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
}.getOrDefault(false)

/** Android仅保留系统能力，手机／平板使用同一公共页面和Store。 */
internal class AndroidClientPlatform(private val context: Context) : ClientUiPlatform {
    override val kind = ClientPlatformKind.Android
    override fun openNotificationSettings(): Boolean = context.startExternalActivity(
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
    override val providers get() = AndroidProviderAuth.providers
    override suspend fun authorizeProvider(provider: String, challenge: ProviderChallenge): ProviderCredential? {
        val activity = context.findActivity()?.takeUnless { it.isFinishing || it.isDestroyed }
            ?: throw MobileApiException(503,"provider Activity unavailable")
        return AndroidProviderAuth.authorize(activity,provider,challenge)?.copy(client = "android",appId = if(provider == "wechat") com.ttkk0000.meowcircle.kmpapp.BuildConfig.WECHAT_APP_ID else com.ttkk0000.meowcircle.kmpapp.BuildConfig.QQ_APP_ID)
    }
    override val policies get() = ClientPolicyLinks(context.getString(R.string.mnd_terms_url), context.getString(R.string.mnd_privacy_url))
    @Composable override fun Window(content: @Composable BoxWithConstraintsScope.() -> Unit) = AndroidAdaptiveWindow(content)
    @Composable override fun BackHandler(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled, onBack)
    @Composable override fun PhotoPicker(remaining: Int, enabled: Boolean, onSelected: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit,
        content: (@Composable (Boolean, () -> Unit) -> Unit)?) =
        com.ttkk0000.meowcircle.kmpapp.mobile.PhotoPickerButton(remaining, enabled, onSelected, onError, content)
    @Composable override fun DateField(store: MobileFeatureStore, state: FeatureState, key: String, label: String, fallback: String, dateOnly: Boolean, optional: Boolean) =
        com.ttkk0000.meowcircle.kmpapp.mobile.DateTimeField(store, state, key, label, fallback, dateOnly, optional)
    @Composable override fun Map(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint, pickable: Boolean, onPick: (LostMapPoint) -> Unit, onSelect: (String) -> Unit) =
        com.ttkk0000.meowcircle.kmpapp.mobile.NativeLostMap(points, selectedId, center, pickable, onPick, onSelect)
    @Composable override fun NotificationPermission(showControls: Boolean, content: @Composable (Boolean) -> Unit) = AndroidNotificationPermission(showControls, content)
    override fun decodeImage(bytes: ByteArray): ImageBitmap? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        }.getOrNull()
    }
    override fun openLink(url: String): Boolean {
        if (url.isBlank() || url.any(Char::isWhitespace)) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) return false
        return context.startExternalActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
