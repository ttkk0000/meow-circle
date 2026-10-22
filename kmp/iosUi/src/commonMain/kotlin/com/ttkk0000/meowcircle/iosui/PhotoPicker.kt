package com.ttkk0000.meowcircle.iosui

import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.MobileStore

@Composable
internal expect fun PhotoPickerButton(store: MobileStore, remaining: Int, enabled: Boolean)
