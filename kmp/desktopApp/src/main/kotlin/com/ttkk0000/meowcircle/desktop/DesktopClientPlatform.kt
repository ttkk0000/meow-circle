package com.ttkk0000.meowcircle.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*
import com.ttkk0000.meowcircle.clientui.*
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.net.URI
import java.nio.file.Files
import java.time.*
import java.util.Date
import javax.imageio.ImageIO
import javax.swing.*
import org.jetbrains.skia.Image as SkiaImage

internal class DesktopClientPlatform(private val owner: Frame, private val endpoint: DesktopEndpoint) : ClientUiPlatform {
    override val kind = ClientPlatformKind.Desktop
    override val policies = ClientPolicyLinks(endpoint.terms, endpoint.privacy)
    private val providerAuth = DesktopProviderAuth(endpoint)
    override val providers get() = providerAuth.providers
    override suspend fun authorizeProvider(provider: String, challenge: ProviderChallenge): ProviderCredential? = providerAuth.authorize(provider, challenge)
    private val backActions = mutableStateListOf<() -> Unit>()
    @Composable override fun Window(content: @Composable BoxWithConstraintsScope.() -> Unit) =
        BoxWithConstraints(Modifier.fillMaxSize().onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyUp && it.key == Key.Escape && backActions.isNotEmpty()) {
                backActions.last().invoke(); true
            } else false
        }, content = content)
    @Composable override fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
        val latest by rememberUpdatedState(onBack)
        val callback = remember { { latest() } }
        DisposableEffect(enabled) { if (enabled) backActions.add(callback); onDispose { backActions.remove(callback) } }
    }
    @Composable override fun PhotoPicker(remaining: Int, enabled: Boolean, onSelected: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit) {
        val scope = rememberCoroutineScope()
        val selected by rememberUpdatedState(onSelected)
        val failed by rememberUpdatedState(onError)
        var opened by remember { mutableStateOf(false) }
        var preparing by remember { mutableStateOf(false) }
        OutlinedButton({ opened = true }, enabled = enabled && remaining > 0 && !opened && !preparing) {
            Text(if (preparing) "正在读取照片" else "添加照片（还可选 $remaining 张）")
        }
        if (opened) DisposableEffect(Unit) {
            val picker = FileDialog(owner, "选择JPEG或PNG照片", FileDialog.LOAD).apply { isMultipleMode = true }
            SwingUtilities.invokeLater {
                picker.isVisible = true
                val files = picker.files.toList()
                opened = false
                if (files.isNotEmpty()) scope.launch {
                    preparing = true
                    try {
                        val photos = withContext(Dispatchers.IO) {
                            files.take(remaining).mapNotNull(::readDesktopPhoto)
                        }
                        selected(photos)
                        if (photos.size != files.size) failed("部分照片无法读取或超出数量／大小限制，请重新选择JPEG或PNG")
                    } catch (e: CancellationException) { throw e } catch (failure: Exception) { failed("照片暂时无法读取，请重新选择") } finally { preparing = false }
                }
            }
            onDispose { picker.dispose() }
        }
    }

    @Composable override fun DateField(store: MobileFeatureStore, state: FeatureState, key: String, label: String,
        fallback: String, dateOnly: Boolean, optional: Boolean) {
        val value = store.field(key, fallback)
        var opened by rememberSessionUiState("$key.dateOpen") { false }
        val enabled = !state.busy && !state.loading && state.confirmation == null && !state.unconfirmedResult
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge)
            OutlinedButton({ opened = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (value.isBlank()) "选择${if (dateOnly) "日期" else "日期与时间"}" else if (dateOnly) value else displayTime(value))
            }
            if (!dateOnly) Text("按本机时区 ${ZoneId.systemDefault().id} 选择。", style = MaterialTheme.typography.bodySmall)
            if (optional && value.isNotBlank()) TextButton({ store.edit(key, "") }, enabled = enabled) { Text("清除日期") }
        }
        if (opened && enabled) DisposableEffect(state.route, key) {
            val zone = ZoneId.systemDefault()
            val initial = runCatching {
                if (dateOnly) LocalDate.parse(value).atStartOfDay(zone).toInstant() else OffsetDateTime.parse(value).toInstant()
            }.getOrElse { Instant.now() }
            val spinner = JSpinner(SpinnerDateModel(Date.from(initial), null, if (dateOnly) Date() else null, java.util.Calendar.MINUTE))
            spinner.editor = JSpinner.DateEditor(spinner, if (dateOnly) "yyyy-MM-dd" else "yyyy-MM-dd HH:mm")
            val dialog = JDialog(owner, label, true)
            dialog.layout = BorderLayout(12, 12)
            dialog.add(spinner, BorderLayout.CENTER)
            val buttons = JPanel()
            buttons.add(JButton("返回编辑").apply { addActionListener { opened = false; dialog.dispose() } })
            buttons.add(JButton("确认选择").apply { addActionListener {
                try {
                    spinner.commitEdit()
                    val chosen = (spinner.value as Date).toInstant().atZone(zone)
                    store.edit(key, if (dateOnly) chosen.toLocalDate().toString() else chosen.toOffsetDateTime().toString())
                    opened = false; dialog.dispose()
                } catch (failure: Exception) { JOptionPane.showMessageDialog(dialog, "请填写有效日期与时间") }
            } })
            dialog.add(buttons, BorderLayout.SOUTH)
            dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            dialog.addWindowListener(object : java.awt.event.WindowAdapter() {
                override fun windowClosed(event: java.awt.event.WindowEvent) { opened = false }
            })
            SwingUtilities.invokeLater { dialog.pack(); dialog.setLocationRelativeTo(owner); dialog.isVisible = true }
            onDispose { dialog.dispose() }
        }
    }

    @Composable override fun Map(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint, pickable: Boolean,
        onPick: (LostMapPoint) -> Unit, onSelect: (String) -> Unit) {
        var error by remember { mutableStateOf<String?>(null) }
        var latitude by remember { mutableStateOf("") }
        var longitude by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("高德Web地图", style = MaterialTheme.typography.titleMedium)
            Text("浏览器打开公开地图；桌面账号凭据不会放入链接。仅绘制已载入的公里级公开粗点。")
            OutlinedButton({ if (!browse("${endpoint.web}/app/map")) error = "无法打开浏览器，请检查系统默认浏览器" }) { Text("打开高德地图") }
            if (pickable) {
                Text("在Web地图选点后，将GCJ-02坐标填入下方；确认前不会写入草稿。")
                FormPair({ OutlinedTextField(latitude, { latitude = it }, label = { Text("GCJ-02纬度") }, singleLine = true) },
                    { OutlinedTextField(longitude, { longitude = it }, label = { Text("GCJ-02经度") }, singleLine = true) })
                TextButton({
                    val lat = latitude.toDoubleOrNull(); val lng = longitude.toDoubleOrNull()
                    val point = if (lat != null && lng != null) coarseMapPoint(lat, lng) else null
                    if (point == null) error = "请填写有效的GCJ-02坐标" else { error = null; onPick(point) }
                }) { Text("使用此大致区域") }
            }
            points.forEach { point -> TextButton({ onSelect(point.id) }) { Text(if (point.id == selectedId) "已选中 · ${point.title}" else point.title) } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    @Composable override fun NotificationPermission(content: @Composable (Boolean) -> Unit) {
        Text("桌面当前通过站内收件箱接收消息。系统推送通道尚未开放。")
        content(false)
    }
    override fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    private fun browse(url: String): Boolean = runCatching {
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        Desktop.getDesktop().browse(URI(url))
    }.isSuccess
    override fun openLink(url: String): Boolean = isPublicHttps(url) && browse(url)
}

/** 仅探测编码和尺寸，上传保留用户选择的原始字节。 */
private fun readDesktopPhoto(file: java.io.File): SelectedPhoto? = runCatching {
    check(Files.size(file.toPath()) in 1..10L * 1024 * 1024)
    val bytes = Files.readAllBytes(file.toPath())
    val png = bytes.size >= 8 && bytes.take(8) == listOf(137, 80, 78, 71, 13, 10, 26, 10).map(Int::toByte)
    val jpeg = bytes.size >= 3 && bytes[0] == 255.toByte() && bytes[1] == 216.toByte() && bytes[2] == 255.toByte()
    check(png || jpeg)
    ImageIO.createImageInputStream(bytes.inputStream()).use { input ->
        val readers = ImageIO.getImageReaders(input)
        check(readers.hasNext())
        val reader = readers.next()
        try {
            reader.input = input
            check(reader.getWidth(0).toLong() * reader.getHeight(0) in 1..40_000_000L)
        } finally {
            reader.dispose()
        }
    }
    SelectedPhoto(file.name.take(200), if (png) "image/png" else "image/jpeg", bytes)
}.getOrNull()
