@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ttkk0000.meowcircle.iosui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.interop.LocalUIViewController
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.client.SelectedPhoto
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.math.min

@Composable
internal actual fun PhotoPickerButton(store: MobileStore, remaining: Int, enabled: Boolean) {
    val controller = LocalUIViewController.current
    val scope = rememberCoroutineScope()
    var preparing by remember { mutableStateOf(false) }
    val delegate = remember(store) {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                if (results.isEmpty()) return
                scope.launch {
                    preparing = true
                    try {
                        // awaitAll preserves the user's selection order despite asynchronous provider callbacks.
                        val photos = results.mapIndexed { index, result -> async(Dispatchers.Default) {
                            val data = suspendCancellableCoroutine<NSData?> { continuation ->
                                val progress = result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                                    if (continuation.isActive) continuation.resume(data)
                                }
                                continuation.invokeOnCancellation { progress.cancel() }
                            } ?: return@async null
                            if (data.length > 30uL * 1024uL * 1024uL) return@async null
                            val original = UIImage(data = data)
                            val dimensions = original.size.useContents { width to height }
                            if (dimensions.first <= 0 || dimensions.second <= 0) return@async null
                            val scale = min(1.0, 2000.0 / maxOf(dimensions.first, dimensions.second))
                            val width = dimensions.first * scale; val height = dimensions.second * scale
                            UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 1.0)
                            val normalized = try {
                                original.drawInRect(CGRectMake(0.0, 0.0, width, height))
                                UIGraphicsGetImageFromCurrentImageContext()
                            } finally { UIGraphicsEndImageContext() }
                            val jpeg = normalized?.let { UIImageJPEGRepresentation(it, 0.88) } ?: return@async null
                            if (jpeg.length > 10uL * 1024uL * 1024uL) return@async null
                            val bytes = ByteArray(jpeg.length.toInt())
                            bytes.usePinned { memcpy(it.addressOf(0), jpeg.bytes, jpeg.length) }
                            SelectedPhoto("照片 ${index + 1}", "image/jpeg", bytes)
                        } }.awaitAll()
                        store.addPhotos(photos.filterNotNull())
                        if (photos.any { it == null }) store.pickerFailed("部分照片无法读取，请重新选择")
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { store.pickerFailed("照片暂时无法读取，请重新选择") }
                    finally { preparing = false }
                }
            }
        }
    }
    OutlinedButton(onClick = {
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = remaining.toLong()
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
        controller.presentViewController(picker, animated = true, completion = null)
    }, enabled = enabled && remaining > 0 && !preparing) {
        Text(if (preparing) "正在准备照片" else "添加照片（还可选 $remaining 张）")
    }
}
