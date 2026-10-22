@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ttkk0000.meowcircle.iosui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ttkk0000.meowcircle.client.MobileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal fun MobilePhoto(store: MobileStore, id: Long?, account: Long?, description: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, id, account) {
        value = null
        if (id != null) try {
            val bytes = store.media(id)
            value = withContext(Dispatchers.Default) {
                val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
                val source = UIImage(data = data)
                val dimensions = source.size.useContents { width to height }
                check(dimensions.first > 0 && dimensions.second > 0)
                val scale = minOf(1.0, 1200.0 / maxOf(dimensions.first, dimensions.second))
                val width = dimensions.first * scale; val height = dimensions.second * scale
                UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 1.0)
                val small = try {
                    source.drawInRect(CGRectMake(0.0, 0.0, width, height))
                    UIGraphicsGetImageFromCurrentImageContext()
                } finally { UIGraphicsEndImageContext() }
                val encoded = small?.let { UIImageJPEGRepresentation(it, 0.9) } ?: error("Invalid image")
                val thumbnail = ByteArray(encoded.length.toInt())
                thumbnail.usePinned { memcpy(it.addressOf(0), encoded.bytes, encoded.length) }
                SkiaImage.makeFromEncoded(thumbnail).toComposeImageBitmap()
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { value = null }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, description, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else Text(if (id == null) "暂无照片" else "照片暂不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
