package com.ttkk0000.meowcircle.clientui

import androidx.compose.runtime.Composable

@Composable internal fun AuthConflictTask(busy: Boolean, hasAccount: Boolean, onReturn: () -> Unit, onOther: () -> Unit, onRecovery: () -> Unit) {
    MobileTaskDialog(onReturn, title = "这个登录方式已有关联", navigationEnabled = !busy) {
        MndScreenTitle("先确认你要使用的账号")
        TaskNotice("不会覆盖任何资料", "这个方式已关联另一个M&D账号。请使用原方式登录，或通过账号恢复获得帮助。", warning = true)
        MndSecondaryButton(if (hasAccount) "返回当前账号" else "返回刚才的页面", !busy, onClick = onReturn)
        MndQuietButton("账号恢复帮助", !busy, onClick = onRecovery)
        FeaturePrimary("用其他方式登录", !busy, onOther)
    }
}

@Composable internal fun AuthLastMethodTask(busy: Boolean, onReturn: () -> Unit, onAdd: () -> Unit) {
    MobileTaskDialog(onReturn, title = "先保留一种登录方式", navigationEnabled = !busy) {
        MndScreenTitle("避免无法再登录")
        TaskNotice("这是最后一种可用方式", "先关联并验证手机号或另一种方式，再解除当前关联。", warning = true)
        MndQuietButton("返回账号设置", !busy, onClick = onReturn)
        FeaturePrimary("添加其他方式", !busy, onAdd)
    }
}
