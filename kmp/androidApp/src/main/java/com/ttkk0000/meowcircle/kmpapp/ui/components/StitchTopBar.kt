package com.ttkk0000.meowcircle.kmpapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.InterFontFamily
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl

enum class StitchTopBarLeading {
    Menu,
    Paw,
    Avatar,
}

enum class StitchTopBarTrailing {
    Bell,
    Settings,
}

/** Mobile app header matching the Stitch phone frames. */
@Composable
fun StitchTopBar(
    apiBase: String,
    user: User?,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "M&D",
    leading: StitchTopBarLeading = StitchTopBarLeading.Paw,
    trailing: StitchTopBarTrailing = StitchTopBarTrailing.Bell,
) {
    val avatarUrl = resolveMediaUrl(apiBase, user?.avatarUrl?.takeIf { it.isNotBlank() })
    val leadingDescription =
        when (leading) {
            StitchTopBarLeading.Menu -> stringResource(R.string.common_menu)
            StitchTopBarLeading.Paw -> stringResource(R.string.app_name)
            StitchTopBarLeading.Avatar -> user?.nickname?.ifBlank { user.username } ?: stringResource(R.string.nav_profile)
        }
    val trailingDescription =
        when (trailing) {
            StitchTopBarTrailing.Bell -> stringResource(R.string.common_notifications)
            StitchTopBarTrailing.Settings -> stringResource(R.string.common_settings)
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(StitchPalette.Surface)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAvatarPress, modifier = Modifier.size(44.dp)) {
                when (leading) {
                    StitchTopBarLeading.Menu ->
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = leadingDescription,
                            tint = StitchPalette.Brand,
                            modifier = Modifier.size(30.dp),
                        )
                    StitchTopBarLeading.Paw ->
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = leadingDescription,
                            tint = StitchPalette.Brand,
                            modifier = Modifier.size(30.dp),
                        )
                    StitchTopBarLeading.Avatar ->
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = leadingDescription,
                                modifier =
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, StitchPalette.Surface, CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(StitchPalette.BrandMuted)
                                        .border(1.dp, StitchPalette.BorderHairline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Pets,
                                    contentDescription = null,
                                    tint = StitchPalette.Brand,
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                        }
                }
            }
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = StitchPalette.Brand,
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(
                onClick = onNotifyPress,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
            ) {
                Icon(
                    imageVector = if (trailing == StitchTopBarTrailing.Settings) Icons.Outlined.Settings else Icons.Outlined.NotificationsNone,
                    contentDescription = trailingDescription,
                    tint = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = StitchPalette.HeaderBorder)
}
