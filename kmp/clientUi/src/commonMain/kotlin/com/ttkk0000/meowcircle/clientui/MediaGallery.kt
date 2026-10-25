package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.MobileStore

@Composable fun ClientMediaGallery(store: MobileStore,media: List<Long>,account: Long?,description: String) {
    var selected by remember(account,media) { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { media.forEachIndexed { index,id ->
        MobilePhoto(store,id,account,description,Modifier.fillMaxWidth().aspectRatio(1.25f).clickable(onClickLabel = "查看第${index+1}张大图") { selected = index })
    } }
    selected?.takeIf { it in media.indices }?.let { index ->
        var scale by remember(index) { mutableStateOf(1f) }
        var offset by remember(index) { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { zoom,pan,_ -> scale = (scale * zoom).coerceIn(1f,5f);offset = if(scale <= 1f) Offset.Zero else offset + pan }
        MobileDialog({ selected = null }) { Surface(modifier = Modifier.fillMaxSize(),color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) { Text("照片 ${index+1}／${media.size}");TextButton({ selected = null }) { Text("关闭大图") } }
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().transformable(transform)) {
                    MobilePhoto(store,media[index],account,description,Modifier.fillMaxSize().graphicsLayer(scaleX = scale,scaleY = scale,translationX = offset.x,translationY = offset.y),ContentScale.Fit)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) { TextButton({ selected = index-1 },enabled = index>0) { Text("上一张") };TextButton({ scale = 1f;offset = Offset.Zero }) { Text("还原缩放") };TextButton({ selected = index+1 },enabled = index<media.lastIndex) { Text("下一张") } }
            }
        } }
    }
}
