package com.ttkk0000.meowcircle.kmpapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors from Stitch MCP 项目「Kitty Circle Social」**仅移动端** `designTheme.namedColors`
 *（与 `StitchMcpMobileRef` 中工程 id 一致；勿用同仓库 `web/_stitch_ref` 桌面 HTML 当色板真值）。
 */
object StitchPalette {
    /** `background` / `surface-bright` */
    val Canvas = Color(0xFFFFF8F7)
    val Surface = Color(0xFFFFFFFF)
    /** `surface-container-low` */
    val SurfaceLow = Color(0xFFFFF0F1)
    val SurfaceContainer = Color(0xFFFDE9EB)
    val SurfaceContainerHigh = Color(0xFFF7E3E6)
    val OnSurface = Color(0xFF23191B)
    val OnSurfaceVariant = Color(0xFF544245)
    /** `primary` — main actions, nav selection */
    val Brand = Color(0xFF964456)
    /** `primary_container` — gradients, chips, soft fills */
    val BrandLight = Color(0xFFFF99AC)
    val BrandEnd = Color(0xFFFFB2BE)
    val BrandMuted = Color(0x1A964456)
    /** `on_primary_container` — text on pink surfaces */
    val PrimaryDark = Color(0xFF7A2D40)
    val Secondary = Color(0xFF785741)
    val SecondaryContainer = Color(0xFFFDD1B4)
    val Outline = Color(0xFF867275)
    val OutlineVariant = Color(0xFFD9C1C3)
    val Error = Color(0xFFBA1A1A)
    /** Accent star / highlights */
    val Gold = Color(0xFFE8BEA2)
    val GoldWeak = Color(0x33E8BEA2)
    val HeaderBorder = Color(0x33964456)
    val BorderHairline = Color(0x28964456)
    val Stone500 = Color(0xFF867275)
}

/**
 * 登录屏色值：与 Stitch MCP 移动端「登录页面」一致（`StitchMcpMobileScreens.LOGIN`），
 * 勿用「登录页面 (桌面版)」导出稿。
 */
object StitchLoginRef {
    val Background = Color(0xFFFBF9F4)
    val PrimaryContainer = Color(0xFFFF5A77)
    val Primary = Color(0xFFB52044)
    val InversePrimary = Color(0xFFFFB2B9)
    val OnSurface = Color(0xFF1B1C19)
    val Outline = Color(0xFF8D7072)
    val SurfaceContainerLow = Color(0xFFF5F3EE)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFE4E2DD)
    val SecondaryContainer = Color(0xFFFDAF18)
    val OnPrimaryButton = Color(0xFFFFFFFF)
    val WeChat = Color(0xFF07C160)
    val QqBlue = Color(0xFF12B7F5)
}
