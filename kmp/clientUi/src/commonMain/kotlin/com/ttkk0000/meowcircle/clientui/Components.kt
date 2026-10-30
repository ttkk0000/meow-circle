package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.error
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** UI primitives have no Store, API, navigation or automatic submit side effect. */
@Composable fun MndForm(modifier: Modifier = Modifier, gap: Dp = MobileUi.formGap, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberTaskScrollState()
    Column(modifier.fillMaxSize().verticalScroll(scroll).padding(MobileUi.pagePadding),
        verticalArrangement = Arrangement.spacedBy(gap), content = content)
}

@Composable fun MndSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(MobileUi.pagePadding), verticalArrangement = Arrangement.spacedBy(MobileUi.contentGap)) {
            if(title.isNotBlank()) Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable fun MndPrimaryButton(label: String, enabled: Boolean = true, loading: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    MndActionButton(label, enabled, loading, modifier, secondary = false, onClick = onClick)
}

@Composable fun MndSecondaryButton(label: String, enabled: Boolean = true, loading: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    MndActionButton(label, enabled, loading, modifier, secondary = true, onClick = onClick)
}

@Composable private fun MndActionButton(label: String, enabled: Boolean, loading: Boolean,
    modifier: Modifier, secondary: Boolean, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val active = enabled && !loading
    val shape = RoundedCornerShape(24.dp)
    val colors = MaterialTheme.colorScheme
    Button(onClick, modifier.heightIn(min = MobileUi.actionHeight)
        .then(if (focused && active) Modifier.border(2.dp, ClientFocusColor, shape) else Modifier),
        enabled = active, shape = shape, interactionSource = interactions,
        border = if (secondary) BorderStroke(1.dp, if (active) ClientButtonBorder else ClientFocusColor) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) colors.primaryContainer else if (pressed || hovered) ClientPublishPressed else colors.primary,
            contentColor = if (secondary) colors.onPrimaryContainer else colors.onPrimary,
            disabledContainerColor = ClientPublishDisabled, disabledContentColor = ClientFocusColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp); Spacer(Modifier.width(MobileUi.compactGap)) }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable fun MndTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, minLines: Int = 1,
    maxLines: Int = if (minLines == 1) 2 else 10, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false, supportingText: (@Composable () -> Unit)? = null) {
    MndOutlinedField(value, onValueChange, modifier.semantics { contentDescription = label }, enabled = enabled, label = { Text(label) },
        minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions, isError = isError, supportingText = supportingText)
}

/** Figma Field: persistent label, 16dp control shape and current semantic states. */
@Composable fun MndOutlinedField(value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(), label: @Composable () -> Unit,
    enabled: Boolean = true, singleLine: Boolean = false, minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false, supportingText: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    BasicTextField(value, onValueChange,
        modifier.semantics { if (isError) error("请核对此字段") }, enabled = enabled,
        singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) MaterialTheme.colorScheme.onSurface else ClientFocusColor),
        keyboardOptions = keyboardOptions, visualTransformation = visualTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary), interactionSource = interactions,
        decorationBox = { inner -> FieldDecoration(label, enabled, focused, isError, supportingText, trailingIcon, inner) })
}

@Composable private fun FieldDecoration(label: @Composable () -> Unit, enabled: Boolean,
    focused: Boolean, isError: Boolean, supportingText: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?, inner: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = if (isError) scheme.error else if (focused && enabled) ClientFocusColor else scheme.outline
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProvideTextStyle(MaterialTheme.typography.labelMedium) { label() }
        Row(Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight)
            .background(if (enabled) scheme.surface else ClientPublishDisabled, MaterialTheme.shapes.medium)
            .border(if (focused && enabled) 2.dp else 1.dp, borderColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(vertical = 12.dp)) { inner() }
            if (trailingIcon != null) Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) { trailingIcon() }
        }
        if (supportingText != null) CompositionLocalProvider(LocalContentColor provides if (isError) scheme.error else scheme.onSurfaceVariant) {
            ProvideTextStyle(MaterialTheme.typography.bodySmall) { supportingText() }
        }
    }
}

@Composable fun MndRichTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, label: String,
    modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, minLines: Int = 1, maxLines: Int = 10,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, compact: Boolean = false) {
    if (compact) BasicTextField(value,onValueChange,modifier.semantics { contentDescription = label },enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        minLines = minLines,maxLines = maxLines,keyboardOptions = keyboardOptions,cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner -> Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.medium)
            .heightIn(min = MobileUi.actionHeight).padding(12.dp),contentAlignment = Alignment.CenterStart) {
            if(value.text.isEmpty()) Text(label,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
            inner()
        } })
    else {
        val interactions = remember { MutableInteractionSource() }
        val focused by interactions.collectIsFocusedAsState()
        BasicTextField(value, onValueChange, modifier.semantics { contentDescription = label }, enabled = enabled,
            minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions,
            interactionSource = interactions, cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) MaterialTheme.colorScheme.onSurface else ClientFocusColor),
            decorationBox = { inner -> FieldDecoration({ Text(label) }, enabled, focused, false, null, null, inner) })
    }
}

@Composable fun MndScreenTitle(text: String) {
    Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable fun MndActionCard(title: String, detail: String, onClick: () -> Unit) {
    Card(onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(MobileUi.pagePadding), verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MndChoiceGroup(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    Text(label, fontWeight = FontWeight.Medium)
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
        options.forEach { (id, text) ->
            FilterChip(value == id, { onValueChange(id) }, label = { Text(text) }, enabled = enabled,
                modifier = Modifier.heightIn(min = MobileUi.actionHeight))
        }
    }
}

@Composable fun MndCheckLine(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = MobileUi.actionHeight).toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChecked),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(label, Modifier.weight(1f))
    }
}
