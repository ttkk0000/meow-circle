package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal data class DialogRegion(val left: Dp = 0.dp,val top: Dp = 0.dp,val right: Dp = 0.dp,val bottom: Dp = 0.dp)
internal val LocalDialogRegion = staticCompositionLocalOf { DialogRegion() }

/** 弹层与任务使用同一块无铰链区域，真实键盘占用后仍可滚动到确认／返回。 */
@Composable internal fun MobileDialog(onDismissRequest: () -> Unit,content: @Composable () -> Unit) {
    val area = LocalDialogRegion.current
    Dialog(onDismissRequest,properties = DialogProperties(usePlatformDefaultWidth = false,decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().absolutePadding(area.left,area.top,area.right,area.bottom).safeDrawingPadding().imePadding().padding(MobileUi.pagePadding),contentAlignment = Alignment.Center) { content() }
    }
}
@Composable internal fun MobileAlertDialog(onDismissRequest: () -> Unit,title: @Composable () -> Unit,text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,dismissButton: @Composable () -> Unit) {
    MobileDialog(onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large,modifier = Modifier.widthIn(max = MobileUi.dialogWidth).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(MobileUi.dialogPadding),verticalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge,title)
                text()
                // 纵向排列使窄分屏和大字下的完整动作标签仍可换行。
                Column(Modifier.fillMaxWidth(),horizontalAlignment = Alignment.End) { confirmButton(); dismissButton() }
            }
        }
    }
}
