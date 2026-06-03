package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape

@Composable
fun StitchSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = StitchPalette.Outline) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = StitchPalette.Outline,
            )
        },
        singleLine = true,
        shape = StitchShape.field,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StitchPalette.Brand.copy(alpha = 0.45f),
                unfocusedBorderColor = StitchPalette.BorderHairline,
                focusedContainerColor = StitchPalette.Surface,
                unfocusedContainerColor = StitchPalette.SurfaceLow,
                cursorColor = StitchPalette.Brand,
                focusedTextColor = StitchPalette.OnSurface,
                unfocusedTextColor = StitchPalette.OnSurface,
            ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
