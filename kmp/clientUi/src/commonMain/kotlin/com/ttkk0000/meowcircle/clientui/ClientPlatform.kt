package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import com.ttkk0000.meowcircle.client.*

enum class ClientPlatformKind { Android, IOS, Desktop }
data class ClientPolicyLinks(val terms: String = "", val privacy: String = "") {
    val ready: Boolean get() = listOf(terms, privacy).all(::isPublicHttps)
}

/** 平台只提供窗口与系统能力；页面、草稿和命令仍由同一Store拥有。 */
interface ClientUiPlatform {
    val kind: ClientPlatformKind
    val policies: ClientPolicyLinks
    val providers: Set<String> get() = emptySet()
    suspend fun authorizeProvider(provider: String, challenge: ProviderChallenge): ProviderCredential? = throw MobileApiException(503, "official provider SDK unavailable")
    @Composable fun Window(content: @Composable BoxWithConstraintsScope.() -> Unit)
    @Composable fun BackHandler(enabled: Boolean, onBack: () -> Unit)
    @Composable fun PhotoPicker(remaining: Int, enabled: Boolean, onSelected: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit)
    @Composable fun DateField(store: MobileFeatureStore, state: FeatureState, key: String, label: String,
        fallback: String, dateOnly: Boolean, optional: Boolean)
    @Composable fun Map(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint, pickable: Boolean,
        onPick: (LostMapPoint) -> Unit, onSelect: (String) -> Unit)
    @Composable fun NotificationPermission(content: @Composable (Boolean) -> Unit)
    fun decodeImage(bytes: ByteArray): ImageBitmap?
    fun openLink(url: String): Boolean
}

val LocalClientPlatform = staticCompositionLocalOf<ClientUiPlatform> { error("ClientUiPlatform is required") }
fun isPublicHttps(value: String): Boolean = Regex("^https://[^/@\\s]+(?:/[^\\s]*)?$").matches(value)

@Composable fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = LocalClientPlatform.current.BackHandler(enabled, onBack)
@Composable fun PlatformWindow(content: @Composable BoxWithConstraintsScope.() -> Unit) = LocalClientPlatform.current.Window(content)
@Composable fun PhotoPickerButton(remaining: Int, enabled: Boolean, onSelected: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit) =
    LocalClientPlatform.current.PhotoPicker(remaining, enabled, onSelected, onError)
@Composable fun PhotoPickerButton(store: MobileStore, remaining: Int, enabled: Boolean) = PhotoPickerButton(remaining, enabled, store::addPhotos, store::pickerFailed)
@Composable fun DateTimeField(f: MobileFeatureStore, s: FeatureState, key: String, label: String, fallback: String = "", dateOnly: Boolean = false, optional: Boolean = false) =
    LocalClientPlatform.current.DateField(f, s, key, label, fallback, dateOnly, optional)
@Composable fun NativeLostMap(points: List<LostMapPoint>, selectedId: String = "", center: LostMapPoint = mapCityCenter(""), pickable: Boolean = false,
    onPick: (LostMapPoint) -> Unit = {}, onSelect: (String) -> Unit = {}) = LocalClientPlatform.current.Map(points, selectedId, center, pickable, onPick, onSelect)
@Composable fun NotificationPermission(content: @Composable (Boolean) -> Unit) = LocalClientPlatform.current.NotificationPermission(content)

expect fun dateTime(value: String): String?
expect fun displayTime(value: String): String
expect fun previousGrapheme(text: String, offset: Int): Int

expect fun futureTime(value: String): Boolean
expect fun validDay(value: String): Boolean
