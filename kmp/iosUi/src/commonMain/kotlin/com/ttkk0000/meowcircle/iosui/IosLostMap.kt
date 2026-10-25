package com.ttkk0000.meowcircle.iosui

import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.LostMapPoint

@Composable internal expect fun IosNativeLostMap(points: List<LostMapPoint>, selectedId: String, center: LostMapPoint,
    pickable: Boolean = false, onPick: (LostMapPoint) -> Unit = {}, onSelect: (String) -> Unit = {})
