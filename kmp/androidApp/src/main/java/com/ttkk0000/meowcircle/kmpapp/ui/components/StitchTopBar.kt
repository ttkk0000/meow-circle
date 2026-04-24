package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl

/** 顶部栏：磨砂浅色底 + 品牌标题，与 Stitch 移动中文版一致。 */
@Composable
fun StitchTopBar(
    apiBase: String,
    user: User?,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Kitty Circle",
    subtitle: String = "喵友圈 · 萌友社区",
) {
    val avatarUrl = resolveMediaUrl(apiBase, user?.avatarUrl?.takeIf { it.isNotBlank() })
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RectangleShape,
                    ambientColor = StitchShadows.headerAmbientColor,
                    spotColor = StitchShadows.headerAmbientColor,
                )
                .background(StitchPalette.SurfaceContainerHigh.copy(alpha = 0.88f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAvatarPress, modifier = Modifier.size(52.dp)) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, StitchPalette.BorderHairline, CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = StitchShadows.avatarGlowColor,
                                    spotColor = StitchShadows.avatarGlowColor,
                                )
                                .clip(CircleShape)
                                .background(StitchPalette.Brand)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.Stone500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onNotifyPress) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "通知",
                    tint = StitchPalette.Brand,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = StitchPalette.HeaderBorder,
        )
    }
}
