package com.ttkk0000.meowcircle.clientui

import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.client.SelectedPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** 预览仅在当前账号的内存中解码，不将照片字节复制到系统恢复状态。 */
@Composable fun LocalPhotoPreview(key: String, owner: Long?, description: String, modifier: Modifier, read: suspend (String) -> SelectedPhoto?) {
    val platform = LocalClientPlatform.current
    val reader by rememberUpdatedState(read)
    val preview by produceState<ImageBitmap?>(null, key, owner) {
        try {
            val photo = reader(key)
            value = photo?.let { withContext(Dispatchers.Default) { platform.decodeImage(it.bytes) } }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            value = null
        }
    }
    val bitmap = preview
    if (bitmap != null) Image(bitmap, description, modifier.clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
    else Box(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("本机照片预览", style = MaterialTheme.typography.bodySmall) }
}

private sealed interface PhotoState {
    data object Loading : PhotoState
    data object Failed : PhotoState
    data class Ready(val bitmap: ImageBitmap) : PhotoState
}

@Composable
fun MobilePhoto(store: MobileStore, id: Long?, account: Long?, description: String, modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.25f), contentScale: ContentScale = ContentScale.Crop) {
    val platform = LocalClientPlatform.current
    // 切换账号／资源时立即丢弃旧图，避免新请求启动前闪现上一张私密照片。
    key(store, id, account) {
        var attempt by remember { mutableIntStateOf(0) }
        val photo by produceState<PhotoState>(PhotoState.Loading, attempt) {
            value = PhotoState.Loading
            if (id != null) try {
                val bytes = store.media(id)
                val bitmap = withContext(Dispatchers.Default) { platform.decodeImage(bytes) }
                currentCoroutineContext().ensureActive()
                value = bitmap?.let { PhotoState.Ready(it) } ?: PhotoState.Failed
            } catch (e: CancellationException) { throw e } catch (_: Exception) { value = PhotoState.Failed }
        }
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            when {
                id == null -> Text("暂无照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                photo is PhotoState.Ready -> Image((photo as PhotoState.Ready).bitmap, description,
                    Modifier.matchParentSize(), contentScale = contentScale)
                photo == PhotoState.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else -> Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("照片加载失败", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { attempt++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("重试图片") }
                }
            }
        }
    }
}
