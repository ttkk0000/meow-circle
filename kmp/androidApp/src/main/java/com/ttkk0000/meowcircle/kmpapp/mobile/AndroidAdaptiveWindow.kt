package com.ttkk0000.meowcircle.kmpapp.mobile

import com.ttkk0000.meowcircle.clientui.*

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

private tailrec fun Context.activity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

/** 分隔铰链的两侧不跨越排版；先将任务放在较大的完整区域，再按该区域宽高选择栏数。 */
@Composable internal fun AndroidAdaptiveWindow(content: @Composable BoxWithConstraintsScope.() -> Unit) {
    val context = LocalContext.current
    val activity = context.activity()
    val folds = remember(activity) {
        if (activity == null) flowOf(emptyList()) else WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { info ->
            info.displayFeatures.filterIsInstance<FoldingFeature>().filter { it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL }
        }.catch { emit(emptyList()) }
    }
    val features by folds.collectAsStateWithLifecycle(emptyList())
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        var left = 0.dp; var right = 0.dp; var top = 0.dp; var bottom = 0.dp
        features.forEach { fold -> with(density) {
            if (fold.orientation == FoldingFeature.Orientation.VERTICAL) {
                val start = fold.bounds.left.toDp().coerceIn(0.dp,maxWidth); val end = fold.bounds.right.toDp().coerceIn(0.dp,maxWidth)
                if (start >= left && end <= maxWidth - right) {
                    if (start - left >= maxWidth - right - end) right = maxWidth - start else left = end
                }
            } else {
                val start = fold.bounds.top.toDp().coerceIn(0.dp,maxHeight); val end = fold.bounds.bottom.toDp().coerceIn(0.dp,maxHeight)
                if (start >= top && end <= maxHeight - bottom) {
                    if (start - top >= maxHeight - bottom - end) bottom = maxHeight - start else top = end
                }
            }
        } }
        CompositionLocalProvider(LocalDialogRegion provides DialogRegion(left,top,right,bottom)) {
            BoxWithConstraints(Modifier.fillMaxSize().absolutePadding(left,top,right,bottom),content = content)
        }
    }
}
