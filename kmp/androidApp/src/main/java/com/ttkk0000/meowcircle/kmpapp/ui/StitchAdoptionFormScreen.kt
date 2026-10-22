package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

@Composable
fun StitchAdoptionFormScreen(
    sdk: MeowCircleSdk,
    petId: Long,
    onBack: () -> Unit,
    onSubmitSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = StitchPalette.OnSurface)
            }
            Text("填写领养申请", style = MaterialTheme.typography.titleLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                label = { Text("申请说明") },
                placeholder = { Text("说明你的居住环境、养宠经验和领养计划。") },
                shape = StitchShape.field,
                colors = adoptionFieldColors(),
            )
            OutlinedTextField(
                value = contactInfo,
                onValueChange = { contactInfo = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("联系方式") },
                placeholder = { Text("微信号或手机号") },
                shape = StitchShape.field,
                colors = adoptionFieldColors(),
            )
            feedback?.let { Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (message.isBlank() || contactInfo.isBlank()) {
                        feedback = "申请说明和联系方式都需要填写。"
                        return@Button
                    }
                    submitting = true
                    feedback = null
                    scope.launch {
                        sdk.applyForAdoption(petId, message.trim(), contactInfo.trim()).fold(
                            onSuccess = { onSubmitSuccess() },
                            onFailure = { feedback = it.message ?: "提交失败，请稍后重试。" },
                        )
                        submitting = false
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.field,
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
            ) {
                Text(if (submitting) "提交中…" else "提交申请", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun adoptionFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StitchPalette.Brand,
        unfocusedBorderColor = StitchPalette.OutlineVariant,
        focusedContainerColor = StitchPalette.Surface,
        unfocusedContainerColor = StitchPalette.Surface,
        cursorColor = StitchPalette.Brand,
    )
