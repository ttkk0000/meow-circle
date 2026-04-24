# iOS（SwiftUI）使用 `shared` KMP 模块

`kmp/shared` 已声明 `iosArm64` / `iosSimulatorArm64`，并产出名为 **`shared`** 的 **静态 Framework**。

## 要求

- **macOS + Xcode**（Kotlin/Native 的 iOS 产物需在 Apple 环境链接）。
- 仓库根目录的 Go 后端可访问（模拟器用 `http://127.0.0.1:8080`，真机用你电脑的局域网 IP）。

## 生成 Framework 供 Xcode 引用

在 `kmp/` 目录：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

Xcode 侧用 **「Add Package / Add Framework」** 指向 Gradle 生成的产物目录，或使用 Android Studio / Fleet 的 KMP 向导生成 `iosApp` 工程后，将 `MeowCircleSdk(baseUrl:)` 注入 SwiftUI（通过 `@StateObject` 包装一层 `ObservableObject`，在 `Task` 里调用 `async` Kotlin suspend 方法需使用 **Skie** 或手写 `CallbackFlow` / `suspend` 桥接；最小做法是先在 Kotlin 的 `iosMain` 增加薄封装导出给 Swift）。

当前仓库**未**提交完整 Xcode 工程，避免在 Windows 上维护二进制工程文件；你在 Mac 上新建 **SwiftUI App**，按 [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html) 官方流程链入 `:shared` 即可。

## API 基址

与 Android 的 `BuildConfig.API_BASE_URL` 一致：真机请传 **电脑局域网 IP + 8080**，不要用 `10.0.2.2`（仅 Android 模拟器指向宿主机）。
