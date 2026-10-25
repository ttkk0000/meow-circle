package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** 宽表单按可读宽度排两列，窄窗／大字体保持一列；不改变字段的状态 owner。 */
@Composable fun FormPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val currentFirst by rememberUpdatedState(first)
    val currentSecond by rememberUpdatedState(second)
    val firstContent = remember { movableContentOf { currentFirst() } }
    val secondContent = remember { movableContentOf { currentSecond() } }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / scale >= MobileUi.formPairWidth) Row(horizontalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) {
            Column(Modifier.weight(1f)) { firstContent() }; Column(Modifier.weight(1f)) { secondContent() }
        } else Column(verticalArrangement = Arrangement.spacedBy(MobileUi.contentGap)) { firstContent(); secondContent() }
    }
}
