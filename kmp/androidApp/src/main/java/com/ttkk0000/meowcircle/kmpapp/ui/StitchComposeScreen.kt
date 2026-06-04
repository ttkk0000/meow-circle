package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

/** MOBILE「M&D 发布帖子」：猫猫优先的 Material 3 表单 + 调用 `POST /api/v1/posts`。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchComposeScreen(
    sdk: MeowCircleSdk,
    onClose: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var pickedMediaCount by remember { mutableStateOf(0) }
    var tagsSummary by remember { mutableStateOf("M&D、猫猫、新手…") }
    var locationSummary by remember { mutableStateOf("分享你所在的位置") }
    var circleSummary by remember { mutableStateOf("猫猫新手村") }
    var visibilitySummary by remember { mutableStateOf("公开") }
    var saveToAlbum by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchLoginRef.Background,
        topBar = {
            TopAppBar(
                title = { Text("M&D 发布台", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            err = null
                            scope.launch {
                                busy = true
                                sdk
                                    .createPost(title, content)
                                    .fold(
                                        onSuccess = {
                                            onPosted()
                                            onClose()
                                        },
                                        onFailure = { e ->
                                            err =
                                                (e as? ApiException)?.message
                                                    ?: humanizeClientFailure(e, sdk.baseUrl)
                                        },
                                    )
                                busy = false
                            }
                        },
                        enabled = !busy && title.isNotBlank() && content.isNotBlank(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp,
                                color = StitchLoginRef.PrimaryContainer,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发布", tint = StitchLoginRef.PrimaryContainer)
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = StitchLoginRef.Background,
                        titleContentColor = StitchLoginRef.OnSurface,
                    ),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.container,
                color = StitchLoginRef.SurfaceContainerLow,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "COMPOSE · M&D",
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchLoginRef.PrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "写给猫猫宇宙的一条新动态",
                        style = MaterialTheme.typography.titleMedium,
                        color = StitchLoginRef.OnSurface,
                    )
                    Text(
                        "猫猫是主角，doggie 也可以一起入镜。",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchLoginRef.Outline,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                placeholder = { Text("今天想记录哪一刻？") },
                singleLine = true,
                shape = StitchShape.field,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchLoginRef.PrimaryContainer,
                        unfocusedBorderColor = StitchLoginRef.Outline.copy(alpha = 0.4f),
                        cursorColor = StitchLoginRef.PrimaryContainer,
                    ),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                label = { Text("正文") },
                placeholder = { Text("写下猫猫日常、求助、活动，doggie 也欢迎出镜。") },
                minLines = 8,
                shape = StitchShape.field,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchLoginRef.PrimaryContainer,
                        unfocusedBorderColor = StitchLoginRef.Outline.copy(alpha = 0.4f),
                        cursorColor = StitchLoginRef.PrimaryContainer,
                    ),
            )
            err?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = StitchLoginRef.SurfaceVariant)
            Spacer(Modifier.height(12.dp))
            ComposeMetaRow(
                Icons.Outlined.AddCircleOutline,
                "媒体",
                if (pickedMediaCount == 0) "照片或视频" else "已选择 $pickedMediaCount 个媒体",
            ) { pickedMediaCount = (pickedMediaCount + 1) % 10 }
            ComposeMetaRow(
                Icons.AutoMirrored.Outlined.Label,
                "标签",
                tagsSummary,
            ) {
                tagsSummary =
                    if (tagsSummary.startsWith("M&D")) {
                        "晒猫日常、幼猫成长"
                    } else {
                        "M&D、猫猫、新手…"
                    }
            }
            ComposeMetaRow(
                Icons.Outlined.LocationOn,
                "添加地点",
                locationSummary,
            ) {
                locationSummary =
                    if (locationSummary.startsWith("分享")) {
                        "上海 · 徐汇"
                    } else {
                        "分享你所在的位置"
                    }
            }
            ComposeMetaRow(
                Icons.Outlined.Groups,
                "发布到圈子",
                circleSummary,
            ) {
                circleSummary =
                    if (circleSummary.startsWith("猫猫")) {
                        "M&D 发现圈子"
                    } else {
                        "猫猫新手村"
                    }
            }
            ComposeMetaRow(
                Icons.Outlined.Public,
                "谁可以看",
                visibilitySummary,
            ) {
                visibilitySummary =
                    when (visibilitySummary) {
                        "公开" -> "仅好友"
                        "仅好友" -> "仅自己"
                        else -> "公开"
                    }
            }
            ComposeMetaRow(
                Icons.Outlined.SaveAlt,
                "保存到相册",
                if (saveToAlbum) "已开启" else "未开启",
            ) { saveToAlbum = !saveToAlbum }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(StitchShape.container)
                        .background(StitchLoginRef.SurfaceContainerLow)
                        .clickable(enabled = !busy && title.isNotBlank() && content.isNotBlank()) {}
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = StitchLoginRef.PrimaryContainer)
                Spacer(Modifier.size(8.dp))
                Text("发布到 M&D", style = MaterialTheme.typography.titleMedium, color = StitchLoginRef.OnSurface)
            }
        }
    }
}

@Composable
private fun ComposeMetaRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
        shape = StitchShape.field,
        color = StitchLoginRef.SurfaceContainerLowest,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = StitchLoginRef.PrimaryContainer, modifier = Modifier.size(24.dp))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = StitchLoginRef.OnSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StitchLoginRef.Outline)
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = StitchLoginRef.Outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
