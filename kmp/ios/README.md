# iOS（SwiftUI）使用 `shared` KMP 模块

`kmp/shared` 已声明 `iosArm64` / `iosSimulatorArm64`，并产出名为 **`shared`** 的 **静态 Framework**。

当前仓库尚未提交完整 Xcode 工程；iOS 的 UI 目标是复刻 Stitch V2 的 M&D 移动端：五个主区（Feed / Market / Messages / Orders / Profile）、全局 Honey/Mint/Night/Neutral 单主题、Profile background（Picnic / Desk / Arcade / Garden），以及与 Android/Expo 相同的 cat-first copy。

## 要求

- **macOS + Xcode**（Kotlin/Native 的 iOS 产物需在 Apple 环境链接）。
- 仓库根目录的 Go 后端可访问（模拟器用 `http://127.0.0.1:8080`，真机用你电脑的局域网 IP）。

## 生成 Framework 供 Xcode 引用

在 `kmp/` 目录：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

仓库根目录也提供了同一个入口：

```bash
make package-kmp-ios-framework
```

Xcode 侧用 **「Add Package / Add Framework」** 指向 Gradle 生成的产物目录，或使用 Android Studio / Fleet 的 KMP 向导生成 `iosApp` 工程后，将 `MeowCircleSdk(baseUrl:)` 注入 SwiftUI（通过 `@StateObject` 包装一层 `ObservableObject`，在 `Task` 里调用 `async` Kotlin suspend 方法需使用 **Skie** 或手写 `CallbackFlow` / `suspend` 桥接；最小做法是先在 Kotlin 的 `iosMain` 增加薄封装导出给 Swift）。

当前仓库**未**提交完整 Xcode 工程，避免在 Windows 上维护二进制工程文件；你在 Mac 上新建 **SwiftUI App**，按 [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html) 官方流程链入 `:shared` 即可。

## Stitch UI shell 对齐清单

- `TabView`: Feed, Market, Messages, Orders, Profile。
- `ThemeStore`: 持久化一个全局主题，只允许 Honey / Mint / Night / Neutral；不要给 Market/Feed 写固定颜色。
- `ProfileBackgroundStore`: 持久化 Picnic / Desk / Arcade / Garden。
- `MndPalette`: 字段名对齐 KMP/Expo token（canvas, surface, onSurface, brand, accentSoft, border, error）。
- `Feed`: 调 `MeowCircleSdk.feedPosts(filter:)`。
- `Market`: 调 `listings()`，详情调 `listingDetail(listingId:)`，购买调 `createOrder(listingId:)`。
- `Messages`: 调 `conversations()` 和 `fetchConversationDetails(peerId:)`。
- `Orders`: 调 `fetchOrders(role:)`、`orderDetail(orderId:)` 以及 pay/cancel/ship/complete/refund。
- `Badges`: 登录后调 `actionCounts()`，Messages 显示 unread messages + notifications，Orders 显示 order actions。
- `Compose`: 第一版可先支持 `createPost` / `createListing`；媒体上传桥接后再接图片/视频。

## API 基址

与 Android 的 `BuildConfig.API_BASE_URL` 一致：真机请传 **电脑局域网 IP + 8080**，不要用 `10.0.2.2`（仅 Android 模拟器指向宿主机）。
