package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.kmpapp.R

@Composable
fun ApiBaseUrlConfigDialog(
    currentUrl: String,
    defaultUrl: String,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var customUrl by remember { mutableStateOf(currentUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.api_config_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.api_config_body), style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.api_config_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(stringResource(R.string.api_config_presets), style = MaterialTheme.typography.labelMedium)
                
                val presets = listOf(
                    "http://127.0.0.1:8080" to stringResource(R.string.api_config_preset_mumu),
                    "http://10.0.2.2:8080" to stringResource(R.string.api_config_preset_emulator),
                )
                
                presets.forEach { (url, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { customUrl = url }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = customUrl == url,
                            onClick = { customUrl = url }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { customUrl = defaultUrl }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = customUrl == defaultUrl,
                        onClick = { customUrl = defaultUrl }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.api_config_restore_default), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalUrl = customUrl.trim()
                    if (finalUrl == defaultUrl) {
                        onSave(null)
                    } else {
                        onSave(finalUrl)
                    }
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    )
}
