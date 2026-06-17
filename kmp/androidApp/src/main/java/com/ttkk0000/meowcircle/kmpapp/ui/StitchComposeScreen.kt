package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.ui.components.dashedBorder
import kotlinx.coroutines.launch

@Composable
fun StitchComposeScreen(
    sdk: MeowCircleSdk,
    onClose: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var categoryKey by remember { mutableStateOf("daily_share") }
    var visibilityKey by remember { mutableStateOf("public") }
    var hasLocation by remember { mutableStateOf(false) }
    var pickedMediaCount by remember { mutableStateOf(0) }
    var mediaIdsText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val defaultPostTitle = stringResource(R.string.compose_default_title)
    val visibilityValue = stringResource(if (visibilityKey == "public") R.string.compose_public else R.string.compose_friends)
    val locationValue = stringResource(if (hasLocation) R.string.compose_location_shanghai else R.string.compose_add_location)

    fun publish() {
        val body = content.trim()
        if (body.isBlank()) return
        val title = body.lineSequence().firstOrNull()?.take(42)?.ifBlank { defaultPostTitle } ?: defaultPostTitle
        val mediaIds = parseMediaIds(mediaIdsText)
        err = null
        busy = true
        scope.launch {
            sdk.createPost(title = title, content = body, category = categoryKey, mediaIds = mediaIds).fold(
                onSuccess = {
                    onPosted()
                    onClose()
                },
                onFailure = { e ->
                    err = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                },
            )
            busy = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchPalette.Canvas,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose, enabled = !busy, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close), tint = StitchPalette.PrimaryDark)
                    }
                    Text(
                        stringResource(R.string.compose_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = StitchPalette.PrimaryDark,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = ::publish,
                        enabled = !busy && content.isNotBlank(),
                        shape = StitchShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                        modifier = Modifier.height(42.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text(stringResource(R.string.compose_post), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = StitchPalette.BorderHairline)
            }
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = StitchShape.field,
                color = StitchPalette.Surface,
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
                        Text("P", color = StitchPalette.Brand, fontWeight = FontWeight.Black)
                    }
                    Text(
                        "Peach & Latte",
                        style = MaterialTheme.typography.titleMedium,
                        color = StitchPalette.OnSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    )
                    Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.compose_change_identity), tint = StitchPalette.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.weight(1f))
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(178.dp),
                placeholder = { Text(stringResource(R.string.compose_placeholder)) },
                minLines = 6,
                shape = StitchShape.cardFeed,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = StitchPalette.Canvas,
                        unfocusedContainerColor = StitchPalette.Canvas,
                        cursorColor = StitchPalette.Brand,
                    ),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(86.dp)
                                .let {
                                    if (index < pickedMediaCount) {
                                        it.clip(StitchShape.field)
                                          .background(StitchPalette.SurfaceLow)
                                          .border(1.dp, StitchPalette.BorderHairline, StitchShape.field)
                                    } else {
                                        it.background(StitchPalette.BrandMuted.copy(alpha = 0.5f), StitchShape.field)
                                          .dashedBorder(StitchPalette.Brand.copy(alpha = 0.4f), 1.5.dp, 12.dp)
                                    }
                                }
                                .clickable { pickedMediaCount = (pickedMediaCount + 1).coerceAtMost(2) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(R.string.feed_add_photo), tint = StitchPalette.Brand)
                    }
                }
            }

            ComposeSettingRow(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.compose_visibility),
                value = visibilityValue,
                onClick = { visibilityKey = if (visibilityKey == "public") "friends" else "public" },
            )
            HorizontalDivider(color = StitchPalette.BorderHairline)
            ComposeSettingRow(
                icon = Icons.Outlined.LocationOn,
                title = stringResource(R.string.compose_add_location),
                value = locationValue,
                onClick = { hasLocation = !hasLocation },
            )

            err?.let {
                Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(190.dp))
            Text(
                stringResource(R.string.compose_draft_saved),
                style = MaterialTheme.typography.bodySmall,
                color = StitchPalette.OnSurfaceVariant,
                fontStyle = FontStyle.Italic,
            )
            Text(stringResource(R.string.compose_suggested_tags), style = MaterialTheme.typography.titleSmall, color = StitchPalette.OnSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    R.string.compose_tag_cat to "daily_share",
                    R.string.compose_tag_daily to "daily_share",
                    R.string.compose_tag_cozy to "activity",
                ).forEach { (labelRes, category) ->
                    val tag = stringResource(labelRes)
                    val active = categoryKey == category
                    Surface(
                        shape = StitchShape.pill,
                        color = if (active) StitchPalette.BrandMuted else StitchPalette.Surface,
                        border = BorderStroke(1.dp, StitchPalette.Brand.copy(alpha = if (active) 0.6f else 0.3f)),
                        modifier = Modifier.clickable {
                            categoryKey = category
                        },
                    ) {
                        Text(
                            tag,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = StitchPalette.Brand.copy(alpha = if (active) 1.0f else 0.8f),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(StitchShape.field)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(24.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = StitchPalette.OnSurface, modifier = Modifier.padding(start = 14.dp).weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = StitchPalette.OnSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

private fun parseMediaIds(raw: String): List<Long> =
    raw
        .split(Regex("[,\\s]+"))
        .mapNotNull { it.trim().toLongOrNull() }
        .distinct()
