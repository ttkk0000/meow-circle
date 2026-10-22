package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ttkk0000.meowcircle.client.MobileStore
import com.ttkk0000.meowcircle.client.SelectedPhoto
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

@Composable
internal fun PhotoPickerButton(store: MobileStore, remaining: Int, enabled: Boolean) {
    PhotoPickerButton(remaining, enabled, store::addPhotos, store::pickerFailed)
}

@Composable
internal fun PhotoPickerButton(remaining: Int, enabled: Boolean, onPhotos: (List<SelectedPhoto>) -> Unit, onError: (String) -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var preparing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            preparing = true
            try {
                val photos = withContext(Dispatchers.IO) {
                    uris.take(remaining).mapIndexed { index, uri ->
                        val mime = resolver.getType(uri)
                        require(mime in listOf("image/jpeg", "image/png", "image/heic", "image/heif", "image/webp"))
                        val bytes = resolver.openInputStream(uri)?.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = input.read(buffer)
                            while (count != -1) {
                                require(output.size() + count <= 30 * 1024 * 1024)
                                output.write(buffer, 0, count)
                                count = input.read(buffer)
                            }
                            output.toByteArray()
                        } ?: error("No image data")
                        val origin = AndroidImageOrigin.read(bytes,mime)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 1
                            while (bounds.outWidth / inSampleSize > 2000 || bounds.outHeight / inSampleSize > 2000) inSampleSize *= 2
                        }
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Unsupported image")
                        val orientation = bytes.inputStream().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                        val matrix = Matrix().apply {
                            when (orientation) {
                                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                            }
                        }
                        val normalized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        val output = ByteArrayOutputStream()
                        val png = mime == "image/png"
                        try { check(normalized.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 90, output)) }
                        finally { if (normalized !== bitmap) normalized.recycle(); bitmap.recycle() }
                        val normalizedBytes = AndroidImageOrigin.write(output.toByteArray(),png,origin)
                        require(normalizedBytes.size <= 10 * 1024 * 1024)
                        SelectedPhoto("照片 ${index + 1}", if (png) "image/png" else "image/jpeg", normalizedBytes)
                    }
                }
                onPhotos(photos)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { onError("请选择可读取且不超过 10 MiB 的 JPEG 或 PNG 照片") }
            finally { preparing = false }
        }
    }
    OutlinedButton(onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = enabled && remaining > 0 && !preparing) {
        Text(if (preparing) "正在准备照片" else "添加照片（还可选 $remaining 张）")
    }
}
