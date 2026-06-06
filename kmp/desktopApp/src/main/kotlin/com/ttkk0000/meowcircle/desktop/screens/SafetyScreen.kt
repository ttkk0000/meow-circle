package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.desktop.AuditLog
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import kotlinx.coroutines.launch

@Composable
fun SafetyScreen(
    apiClient: MndApiClient,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()

    var auditLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun refreshLogs() {
        scope.launch {
            loading = true
            errorMsg = null
            apiClient.fetchAuditLogs()
                .onSuccess { auditLogs = it }
                .onFailure { errorMsg = it.message ?: "Failed to load audit logs" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Safety header
        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Safety & Audits Queue",
                    color = colors.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Neutral is a first-class theme, not a default fallback. It stays restrained and data-oriented.",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        // Table
        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Review Queue", fontWeight = FontWeight.Bold, color = colors.onSurface, fontSize = 16.sp)
                    Button(onClick = { refreshLogs() }, shape = MndDesktopShape.pill) {
                        Text("Refresh Logs")
                    }
                }

                Divider(color = colors.outlineVariant)

                // Table Headers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ID", fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(60.dp))
                    Text("EVENT ACTION", fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("STATUS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(100.dp))
                    Text("ACTOR", fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(100.dp))
                }

                Divider(color = colors.outlineVariant)

                if (loading && auditLogs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.brand)
                    }
                } else if (errorMsg != null && auditLogs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMsg.orEmpty(), color = colors.error, fontWeight = FontWeight.Bold)
                    }
                } else if (auditLogs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No audit log events recorded.", color = colors.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(auditLogs) { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${log.id}", fontSize = 13.sp, color = colors.onSurface, modifier = Modifier.width(60.dp))
                                Text(log.action, fontSize = 13.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .padding(end = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("OK", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text(log.actor.ifBlank { "system" }, fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(100.dp))
                            }
                            Divider(color = colors.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
