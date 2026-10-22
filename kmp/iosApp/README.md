# M&D iPhone / iPad Compose 宿主

2026-10-20 已新增 `../iosUi`，新 XcodeGen 目标仅编译 `ComposeApp` 的 Swift 薄宿主 / Keychain 适配，链接 `MndMobileUI.framework` 并复用 shared/client 业务状态。界面由 iosUi 自己的 Compose 源码绘制，按 Scene 宽度适配 iPhone、iPad 横竖屏与分屏。最低 iOS 17，Swift 6，当前只用默认浅色。

旧 `MndIOS` SwiftUI / Store / 主题源码原样保留，已不列入新目标的 sources；下文是迁移前历史说明。当前只有第一批界面与共享接线，完整功能尚未完成，B04 认证合同及平台构建仍未验证。见[实施记录](../../docs/client/MOBILE_IMPLEMENTATION_2026_10_20.md)和[依赖基线](../../docs/client/DEPENDENCY_BASELINE_2026_10_20.md)。没有运行 XcodeGen / Gradle / Xcode 或设备。

2026-10-20用户决定：Android / iOS共享KMP业务逻辑，iOS新主界面规划在独立`iosUi`模块中使用Compose Multiplatform，iPad单独适配，当前仅使用设计默认主题，多主题预留。见[客户端实施方案](../../docs/client/IMPLEMENTATION_PLAN.md)与[任务I01–I04](../../docs/client/TASKS.md)。目前未新增该模块或替换页面；以下描述现有SwiftUI代码事实，不能作为继续扩充新SwiftUI业务UI的目标。旧代码保留，替代验证后再清理。

`iosApp` 是原生 SwiftUI 壳，继续复用 `../shared` 中的 KMP 网络、会话和领域模型。
UI 状态、导航和主题由 SwiftUI 原生管理，避免把 Compose UI 或 Android 生命周期概念带到 iOS。

## 架构边界

- `shared/commonMain`: `MeowCircleSdk`、模型、会话、Ktor 请求。
- `shared/iosMain/IosSdkBridge.kt`: 把 Kotlin `suspend + Result` 变成可取消的 Swift 回调。
- `MndIOS/Architecture`: Swift 单一 State/Action/Effect 与可复用加载状态。
- `MndIOS/Navigation`: 每个 Tab 独立 `NavigationStack`。
- `MndIOS/DesignSystem`: 当前 Stitch V2 的 Honey / Mint / Night / Neutral 语义令牌。
- `MndIOS/Features`: SwiftUI 原生页面，只通过注入的 Store/Bridge 获取数据。

## 在 macOS 生成工程

要求：JDK 17、Xcode 16+、XcodeGen。

```bash
cd kmp/iosApp
xcodegen generate
open MndIOS.xcodeproj
```

Xcode 的预构建脚本会调用：

```bash
cd kmp
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

模拟器默认后端地址为 `http://127.0.0.1:8080`。真机请在 Xcode 的 Build Settings 中把
`MND_API_BASE_URL` 改为开发机局域网地址，例如 `http://192.168.1.10:8080`。

## 最低系统

iOS 17。该基线允许使用 Observation 的 `@Observable`、`@Environment(Type.self)` 和现代
SwiftUI 状态所有权；如需降到 iOS 16，应把根 Store 改为 `ObservableObject/@StateObject`。
