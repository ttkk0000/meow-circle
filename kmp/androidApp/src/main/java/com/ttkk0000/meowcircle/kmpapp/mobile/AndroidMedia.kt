package com.ttkk0000.meowcircle.kmpapp.mobile

import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ttkk0000.meowcircle.client.MobileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MobilePhoto(store: MobileStore, id: Long?, account: Long?, description: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, id, account) {
        value = null
        if (id != null) try {
            val bytes = store.media(id)
            value = withContext(Dispatchers.Default) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { value = null }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, description, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else Text(if (id == null) "暂无照片" else "照片暂不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
