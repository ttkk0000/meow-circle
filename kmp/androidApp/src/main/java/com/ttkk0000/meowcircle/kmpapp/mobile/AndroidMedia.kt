package com.ttkk0000.meowcircle.kmpapp.mobile

import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.MobileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private sealed interface PhotoState {
    data object Loading : PhotoState
    data object Failed : PhotoState
    data class Ready(val bitmap: ImageBitmap) : PhotoState
}

@Composable
internal fun MobilePhoto(store: MobileStore, id: Long?, account: Long?, description: String, modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.25f), contentScale: ContentScale = ContentScale.Crop) {
    // 切换账号／资源时立即丢弃旧图，避免新请求启动前闪现上一张私密照片。
    key(store, id, account) {
        var attempt by remember { mutableIntStateOf(0) }
        val photo by produceState<PhotoState>(PhotoState.Loading, attempt) {
            value = PhotoState.Loading
            if (id != null) try {
                val bytes = store.media(id)
                val bitmap = withContext(Dispatchers.Default) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                }
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
