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
        Box(Modifier.fillMaxSize().absolutePadding(area.left,area.top,area.right,area.bottom).safeDrawingPadding().imePadding().padding(16.dp),contentAlignment = Alignment.Center) { content() }
    }
}
/** 登录等独立任务在手机占用当前安全区域，平板保持可读的居中正文宽度。 */
@Composable internal fun MobileTaskDialog(onDismissRequest: () -> Unit,content: @Composable ColumnScope.() -> Unit) {
    val area = LocalDialogRegion.current
    Dialog(onDismissRequest,properties = DialogProperties(usePlatformDefaultWidth = false,decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().absolutePadding(area.left,area.top,area.right,area.bottom).safeDrawingPadding().imePadding(),contentAlignment = Alignment.Center) {
            val compact = maxWidth < 600.dp
            Surface(color = MaterialTheme.colorScheme.background,shape = if(compact) androidx.compose.ui.graphics.RectangleShape else MaterialTheme.shapes.large,
                modifier = if(compact) Modifier.fillMaxSize() else Modifier.padding(24.dp).widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight(0.94f)) { TaskColumn(content = content) }
        }
    }
}
@Composable internal fun MobileAlertDialog(onDismissRequest: () -> Unit,title: @Composable () -> Unit,text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,dismissButton: @Composable () -> Unit) {
    MobileDialog(onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large,modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge,title)
                text()
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) { dismissButton(); Spacer(Modifier.width(8.dp)); confirmButton() }
            }
        }
    }
}
