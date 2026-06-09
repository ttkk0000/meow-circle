package com.ttkk0000.meowcircle.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "M&D Desktop",
    ) {
        DesktopApp()
    }
}

@Composable
@Preview
fun DesktopApp() {
    var themeMode by remember { mutableStateOf(MndDesktopThemeMode.Honey) }
    MndDesktopTheme(themeMode) {
        MndDesktopShell(themeMode = themeMode, onThemeChange = { themeMode = it })
    }
}

@Composable
private fun MndDesktopShell(
    themeMode: MndDesktopThemeMode,
    onThemeChange: (MndDesktopThemeMode) -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.canvas,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DesktopRail(themeMode = themeMode, onThemeChange = onThemeChange)
            MainFeedColumn()
            RightWorkspaceColumn()
        }
    }
}

@Composable
private fun DesktopRail(
    themeMode: MndDesktopThemeMode,
    onThemeChange: (MndDesktopThemeMode) -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    Card(
        modifier = Modifier.width(248.dp).fillMaxHeight(),
        shape = MndDesktopShape.panel,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("M&D", color = colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("meow & doggie", color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                text = "Stitch desktop reference",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
            RailItem("Feed", "cat-first stories", selected = true)
            RailItem("Market", "services and goods")
            RailItem("Messages", "orders and chat")
            RailItem("Review", "safety queue")
            Spacer(Modifier.weight(1f))
            Text("Theme", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            MndDesktopThemeMode.entries.forEach { mode ->
                ThemeChoice(
                    mode = mode,
                    selected = themeMode == mode,
                    onClick = { onThemeChange(mode) },
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    title: String,
    subtitle: String,
    selected: Boolean = false,
) {
    val colors = LocalDesktopStitchColors.current
    val background = if (selected) colors.brandMuted else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MndDesktopShape.card)
            .background(background)
            .border(1.dp, if (selected) colors.brand else colors.outlineVariant, MndDesktopShape.card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandDot(12.dp, if (selected) colors.brand else colors.outline)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ThemeChoice(
    mode: MndDesktopThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val modeColors = colorsFor(mode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MndDesktopShape.card)
            .clickable(onClick = onClick)
            .background(if (selected) colors.surfaceLow else Color.Transparent)
            .border(1.dp, if (selected) colors.brand else colors.outlineVariant, MndDesktopShape.card)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandDot(14.dp, modeColors.brand)
        Spacer(Modifier.width(10.dp))
        Text(mode.label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.MainFeedColumn() {
    val colors = LocalDesktopStitchColors.current
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "M&D desktop client",
                    color = colors.onSurface,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Cat-first community, doggie-friendly market, dense desktop controls.",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryAction("New post")
                    SecondaryAction("Open market")
                    SecondaryAction("Review queue")
                }
            }
        }
        FeedCard(
            author = "Mika & Toast",
            title = "Morning window patrol",
            body = "Toast claimed the sunny desk corner. The desktop layout keeps feed reading calm while still leaving room for orders and messages.",
            tags = listOf("cat-first", "honey", "desktop"),
        )
        FeedCard(
            author = "Nori Studio",
            title = "Carrier training checklist",
            body = "Reusable cards stay at 8dp radius. Theme colors never mix across Honey, Mint, Night, and Neutral.",
            tags = listOf("care", "guide", "neutral-ready"),
        )
        FeedCard(
            author = "Doggie branch",
            title = "Weekend walking service",
            body = "Doggie content remains a companion branch for services, marketplace items, and local activities.",
            tags = listOf("doggie", "market", "mint"),
        )
    }
}

@Composable
private fun RightWorkspaceColumn() {
    val colors = LocalDesktopStitchColors.current
    Column(
        modifier = Modifier.widthIn(min = 320.dp, max = 360.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Panel("Stitch refs") {
            RefRow("Project", STITCH_MCP_MND_PROJECT_ID)
            RefRow("Feed", StitchDesktopScreens.SOCIAL_FEED)
            RefRow("Market desktop", StitchDesktopScreens.MARKET_DESKTOP)
            RefRow("Neutral theme", StitchDesktopThemeScreens.NEUTRAL)
        }
        Panel("Orders") {
            StatusRow("Paid", "2", colors.brand)
            StatusRow("Shipping", "5", colors.gold)
            StatusRow("Review", "1", colors.error)
        }
        Panel("Safety") {
            Text(
                "Neutral is a first-class theme for dense tools and true admin/moderation screens.",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun FeedCard(
    author: String,
    title: String,
    body: String,
    tags: List<String>,
) {
    val colors = LocalDesktopStitchColors.current
    Card(
        shape = MndDesktopShape.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(38.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(author, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("M&D circle", color = colors.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Text(title, color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(body, color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { Chip(it) }
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalDesktopStitchColors.current
    Card(
        shape = MndDesktopShape.panel,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun RefRow(label: String, value: String) {
    val colors = LocalDesktopStitchColors.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = colors.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    val colors = LocalDesktopStitchColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandDot(10.dp, color)
            Spacer(Modifier.width(8.dp))
            Text(label, color = colors.onSurfaceVariant, fontSize = 13.sp)
        }
        Text(value, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Chip(label: String) {
    val colors = LocalDesktopStitchColors.current
    Text(
        text = label,
        modifier = Modifier
            .clip(MndDesktopShape.pill)
            .background(colors.brandMuted)
            .border(1.dp, colors.outlineVariant, MndDesktopShape.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = colors.onSurface,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PrimaryAction(label: String) {
    val colors = LocalDesktopStitchColors.current
    Button(
        onClick = {},
        shape = MndDesktopShape.pill,
        colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = Color.White),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryAction(label: String) {
    val colors = LocalDesktopStitchColors.current
    TextButton(onClick = {}, shape = MndDesktopShape.pill) {
        Text(label, color = colors.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BrandMark(size: Dp) {
    val colors = LocalDesktopStitchColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.brand),
        contentAlignment = Alignment.Center,
    ) {
        Text("M&D", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BrandDot(size: Dp, color: Color) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
