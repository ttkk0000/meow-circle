package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette

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
        shape = RoundedCornerShape(999.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StitchPalette.Brand.copy(alpha = 0.45f),
                unfocusedBorderColor = StitchPalette.BorderHairline,
                focusedContainerColor = StitchPalette.SecondaryContainer.copy(alpha = 0.35f),
                unfocusedContainerColor = StitchPalette.SecondaryContainer.copy(alpha = 0.28f),
                cursorColor = StitchPalette.Brand,
                focusedTextColor = StitchPalette.OnSurface,
                unfocusedTextColor = StitchPalette.OnSurface,
            ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
