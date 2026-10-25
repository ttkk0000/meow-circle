package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    Button(onClick, modifier.heightIn(min = MobileUi.actionHeight), enabled = enabled && !loading) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp); Spacer(Modifier.width(MobileUi.compactGap)) }
        Text(label)
    }
}

@Composable fun MndSecondaryButton(label: String, enabled: Boolean = true, loading: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = MobileUi.actionHeight), enabled = enabled && !loading) {
        if (loading) { CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp); Spacer(Modifier.width(MobileUi.compactGap)) }
        Text(label)
    }
}

@Composable fun MndTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, minLines: Int = 1,
    maxLines: Int = if (minLines == 1) 2 else 10, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false, supportingText: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value, onValueChange, modifier, enabled = enabled, label = { Text(label) },
        minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions, isError = isError, supportingText = supportingText)
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
    else OutlinedTextField(value, onValueChange, modifier, enabled = enabled, label = { Text(label) }, minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions)
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
