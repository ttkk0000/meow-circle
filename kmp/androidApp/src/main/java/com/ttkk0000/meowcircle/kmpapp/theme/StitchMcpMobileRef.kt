package com.ttkk0000.meowcircle.kmpapp.theme

/**
 * Stitch **MCP** 源：项目「Kitty Circle Social」`deviceType = MOBILE`（不要用同项目里的桌面屏当真值）。
 *
 * 对齐方式：以 MCP `list_projects` / `get_project` 返回的 `designTheme.namedColors` 与 `typography` 为准；
 * 具体界面 HTML 用 MCP `list_screens` 筛 `deviceType == MOBILE` 后 `get_screen`。
 *
 * Project id（`list_screens` / `get_screen` 的 `projectId`）：
 */
const val STITCH_MCP_KITTY_CIRCLE_PROJECT_ID = "472020832926366758"

/** 移动端屏（`list_screens` 中 `deviceType: MOBILE`）的 `screenId` 片段，便于 `get_screen`。 */
object StitchMcpMobileScreens {
    const val HOME_ZH = "24394474a8fb461691446c023767549f"
    const val LOGIN = "11540204191034377071"
    const val REGISTER = "62e4e647722245a88e07d5801560ada1"
    const val POST_DETAIL_ZH = "56969a3c2b064bd1976cf418ec4d32e2"
    const val COMPOSE_POST_ZH = "300abed7f6aa4aef9e4a02b95e093059"
    const val DISCOVER_ZH = "c4356b5a317846ae8d58ffa3ec723154"
    const val PROFILE_ZH = "072b7dd565914b8e835a6260878aa0ee"
    const val MESSAGES_ZH = "855b3e0ba0194e9aaa07f2b0237c075f"
    const val SPLASH = "ea5847d8c0fd4dc7b14e62893aad56ee"
    const val LOADING = "550bdee1020e465c9cc49aaf46de7ec8"
}
