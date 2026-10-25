package com.ttkk0000.meowcircle.clientui

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

data class DialogRegion(val left: Dp = 0.dp,val top: Dp = 0.dp,val right: Dp = 0.dp,val bottom: Dp = 0.dp)
val LocalDialogRegion = staticCompositionLocalOf { DialogRegion() }

/** 弹层与任务使用同一块无铰链区域，真实键盘占用后仍可滚动到确认／返回。 */
@Composable fun MobileDialog(onDismissRequest: () -> Unit,content: @Composable () -> Unit) {
    val area = LocalDialogRegion.current
    Dialog(onDismissRequest,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().absolutePadding(area.left,area.top,area.right,area.bottom).safeDrawingPadding().imePadding().padding(MobileUi.pagePadding),contentAlignment = Alignment.Center) { content() }
    }
}
/** 登录等独立任务在手机占用当前安全区域，平板保持可读的居中正文宽度。 */
@Composable fun MobileTaskDialog(onDismissRequest: () -> Unit,title: String? = null,content: @Composable ColumnScope.() -> Unit) {
    val area = LocalDialogRegion.current
    Dialog(onDismissRequest,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().absolutePadding(area.left,area.top,area.right,area.bottom).safeDrawingPadding().imePadding(),contentAlignment = Alignment.Center) {
            val compact = maxWidth < 600.dp
            Surface(color = MaterialTheme.colorScheme.background,shape = if(compact) androidx.compose.ui.graphics.RectangleShape else MaterialTheme.shapes.large,
                modifier = if(compact) Modifier.fillMaxSize() else Modifier.padding(24.dp).widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight(0.94f)) {
                Column {
                    if(title != null) Surface(color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(4.dp),verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onDismissRequest,Modifier.size(48.dp)) { DesignIcon("chevron-left","返回") }
                            Text(title,Modifier.weight(1f),style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    TaskColumn(content = content)
                }
            }
        }
    }
}
@Composable fun MobileAlertDialog(onDismissRequest: () -> Unit,title: @Composable () -> Unit,text: @Composable () -> Unit,
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
