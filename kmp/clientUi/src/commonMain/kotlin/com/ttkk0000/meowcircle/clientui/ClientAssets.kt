package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.EmojiItem
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.svg.SvgDecoder
import com.ttkk0000.meowcircle.clientui.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val LocalDesignImageLoader = staticCompositionLocalOf<ImageLoader> { error("ClientAssets is required") }
private object AssetBytes {
    private val lock = Mutex()
    private val values = mutableMapOf<String, ByteArray>()
    suspend fun get(path: String): ByteArray = lock.withLock { values[path] ?: Res.readBytes(path).also { values[path] = it } }
}

@Composable fun ClientAssets(content: @Composable () -> Unit) {
    val context = LocalPlatformContext.current
    val loader = remember(context) { ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    CompositionLocalProvider(LocalDesignImageLoader provides loader, content = content)
}

@Composable private fun bundledBytes(path: String): ByteArray? {
    val bytes by produceState<ByteArray?>(null, path) { value = runCatching { AssetBytes.get(path) }.getOrNull() }
    return bytes
}

/** 仅使用Figma下载原SVG，根尺寸24×24；没有重绘或替换图案。 */
@Composable fun DesignIcon(name: String, description: String?, modifier: Modifier = Modifier) {
    val bytes = bundledBytes("files/figma/$name.svg")
    Box(modifier.size(24.dp)) {
        if (bytes != null) AsyncImage(bytes, description, imageLoader = LocalDesignImageLoader.current,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Fit)
    }
}

@Composable fun BundledGuideImage(path: String, description: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bytes = bundledBytes(path)
    if (bytes == null) Box(modifier) { Text("配图正在载入") }
    else AsyncImage(bytes, description, imageLoader = LocalDesignImageLoader.current, modifier = modifier, contentScale = contentScale)
}

@Composable fun StickerPhoto(id: String, description: String, modifier: Modifier = Modifier.size(MobileUi.stickerSize)) {
    val index = id.removePrefix("sticker-").toIntOrNull()?.minus(1)
    val bytes = bundledBytes("files/mnd_cat_stickers.png")
    val platform = LocalClientPlatform.current
    val atlas by produceState<ImageBitmap?>(null, bytes, platform) {
        value = bytes?.let { withContext(Dispatchers.Default) { platform.decodeImage(it) } }
    }
    val image = atlas
    if (index == null || index !in 0..23 || image == null) Box(modifier) { Text("贴纸正在载入") }
    else {
        val width = image.width / 6; val height = image.height / 4
        val painter = remember(image, index) { BitmapPainter(image, IntOffset(index % 6 * width, index / 6 * height), IntSize(width, height)) }
        Image(painter, description, modifier, contentScale = ContentScale.Fit)
    }
}

/** Noto原SVG保持正方形；已发送的单颗表情占144区域，图案72，不随正文扩宽。 */
@Composable fun EmojiPhoto(item: EmojiItem, modifier: Modifier = Modifier.size(32.dp)) {
    val filename = Regex("^/api/v2/expression-assets/v1/(emoji_u[a-f0-9_]+\\.svg)$").matchEntire(item.assetUrl)?.groupValues?.get(1)
    val bytes = filename?.let { bundledBytes("files/emoji/$it") }
    Box(modifier, contentAlignment = Alignment.Center) {
        if(bytes != null) AsyncImage(bytes,item.label,imageLoader = LocalDesignImageLoader.current,modifier = Modifier.matchParentSize(),contentScale = ContentScale.Fit)
        else Text(item.label)
    }
}

@Composable fun RichContentText(text: String, catalog: List<EmojiItem>) {
    val single = catalog.firstOrNull { it.unicode == text.trim() }
    if(single == null) Text(text)
    else Box(Modifier.size(MobileUi.stickerSize),contentAlignment = Alignment.Center) { EmojiPhoto(single,Modifier.size(72.dp)) }
}
