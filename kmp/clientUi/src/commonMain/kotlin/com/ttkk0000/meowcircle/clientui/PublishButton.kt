package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PublishButtonSize { Sidebar, Compact, Shortcut }

/** Figma 836:21 / 838:21 / 838:58; press changes only the face, never the hit area. */
@Composable
fun MndPublishButton(size: PublishButtonSize, enabled: Boolean = true, current: Boolean = false,
    navigation: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val shortcut = size == PublishButtonSize.Shortcut
    val compact = size == PublishButtonSize.Compact
    val faceHeight = if (shortcut) { if (pressed && enabled) 34.dp else 36.dp }
        else if (pressed && enabled) 38.dp else 40.dp
    val background = when {
        !enabled -> ClientPublishDisabled
        pressed || hovered -> ClientPublishPressed
        else -> MaterialTheme.colorScheme.primary
    }
    val foreground = if (enabled) MaterialTheme.colorScheme.onPrimary else ClientFocusColor
    Box(modifier.defaultMinSize(minWidth = if (size == PublishButtonSize.Sidebar) 152.dp else if (compact) 56.dp else 48.dp,
        minHeight = 48.dp).semantics(mergeDescendants = true) {
            contentDescription = "发布"
            if (navigation) selected = current
        }.hoverable(interactions, enabled).clickable(interactionSource = interactions, indication = null,
            enabled = enabled, role = if (navigation) Role.Tab else Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth().heightIn(min = faceHeight).background(background, CircleShape)
            .then(if (focused && enabled) Modifier.border(2.dp, ClientFocusColor, CircleShape) else Modifier)
            .padding(horizontal = if (compact) 4.dp else if (shortcut) 8.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            DesignIcon(if (enabled) "publish-circle-plus" else "publish-circle-plus-disabled", null, Modifier.size(20.dp))
            if (!shortcut) Text("发布", Modifier.weight(1f, fill = false), color = foreground,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = if (compact) 12.sp else 14.sp,
                    lineHeight = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Medium),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
