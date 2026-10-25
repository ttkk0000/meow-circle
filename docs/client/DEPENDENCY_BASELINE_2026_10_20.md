# KMP 客户端依赖升级基线

日期：2026-10-20。用户要求 Android 17、最低 API 26，并将本次 KMP 工程的工具链与依赖升级到可用的最高版本。本次选择正式发行版，不主动引入 RC / Beta / Alpha。范围是 `kmp`，包括 shared、Android、iosUi 和共用版本的 Desktop；没有变更 Go、Expo 或旧 Web 的依赖。

通过官方 Maven 仓库实际读取的版本记录见 [JSON 证据](dependency-metadata-2026-10-20.json)。没有解析或下载依赖产物，没有执行 Gradle。

| 项目 | 原版本 | 当前锁定 | 依据 / 边界 |
|---|---|---|---|
| Android compile / target | 35 / 35 | 37 / 37 | [Android 17 配置](https://developer.android.com/about/versions/17/setup-sdk)；shared compile 也为 37，两个 minSdk 均为 26 |
| Kotlin / Compose Compiler | 2.0.21 | 2.4.21 | 官方 Maven 最新正式版；两个插件同版本 |
| AGP | 8.7.2 | 9.3.3 | 采用 KGP 支持的 9.3 分支最新修复版；没有跳到 9.4.1 |
| Gradle 分发配置 | 8.10.2 | 9.7.1 | 采用 KGP 支持的 9.7 分支最新修复版；没有跳到 9.8.2 |
| Compose Multiplatform | 1.7.3 | 1.12.2 | 最新正式框架，Android / iOS 仍各有自己的 UI 模块 |
| Android Compose BOM | 2024.10.01 | 2026.09.00 | 官方 Maven 最新稳定 BOM，统一 runtime / UI / foundation / Android Material3 |
| Ktor 各平台引擎 / Mock | 3.0.3 | 3.6.0 | 最新正式版，保留 OkHttp / Darwin 引擎分工 |
| serialization JSON | 1.7.3 | 1.12.0 | 官方 Maven 最新正式版；未采用搜索缓存中的旧 RC 记录 |
| coroutines core / Android / test | 1.9.0 | 1.11.0 | 最新正式版；Android 显式声明 Main 调度器依赖 |
| multiplatform-settings | 1.2.0 | 1.3.0 | 仍用于历史非秘密偏好，v2 token 不写入明文 Settings |
| Activity Compose | 1.9.3 | 1.13.0 | 最新稳定版，1.14 Alpha 暂不采用 |
| Lifecycle Compose | 2.8.7 | 2.11.0 | 最新稳定版，2.12 Alpha 暂不采用 |
| Coil | 2.7.0 | 3.6.3 | 坐标改 `io.coil-kt.coil3`；旧 UI 的 12 个导入同步改 `coil3.compose`，加入独立网络模块 |
| Android ExifInterface | 无直接依赖 | 1.4.2 | 最新稳定版；选图后处理方向和尺寸，输出服务可接受的 JPEG / PNG |
| CMP Material3 | 跟随旧插件 | 1.9.0 | 最新**稳定**组件；CMP 1.12.2 默认组件为 `1.12.0-alpha03`，因此显式覆盖 |
| Android Material Icons | 跟随旧 BOM | 1.7.8 | 上游冻结的最终版本 |
| CMP Material Icons | 无新 iOS 模块 | 1.7.3 | 上游冻结的最终版本，显式坐标替代已弃用的 Gradle 简写 |

[KMP 官方兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)目前对 2.4.20–2.4.21 列出的精确上界是 Gradle 9.7.0 / AGP 9.3.1 / Xcode 26.4。选择 9.7.1 / 9.3.3 是在同一受支持分支内采用修复版的工程判断，**不是官方已经测试这两个精确补丁组合的声明**。参见 [Gradle 9.7.1](https://docs.gradle.org/9.7.1/release-notes.html)、[AGP 9.3 修复记录](https://developer.android.com/build/releases/agp-9-3-0-release-notes)。更高的 9.8 / 9.4 分支超出已公布的 KGP 范围，未通过压制警告强行使用。

[CMP 兼容说明](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)及 [1.12.2 发行记录](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2)区分框架与 Material3 的版本；稳定 Material3 1.9.0 的对应关系见 [1.9.3 发行记录](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.9.3)。冻结图标版本依据 [官方插件源码](https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/ComposePlugin.kt)及 [Android BOM 映射](https://developer.android.com/develop/ui/compose/bom/bom-mapping)。

## 工程迁移

- Android 应用使用 AGP 内建 Kotlin；shared 使用新的 Android KMP library 插件及 `kotlin.android {}`。移除旧 Kotlin Android 插件和压制兼容警告的配置。
- JVM 字节码与 Desktop toolchain 保持 17，避免把语言目标数字的升高误当成依赖升级；没有修改本机 JDK / Android Studio / Xcode 安装或 JAVA_HOME。iOS 宿主语言模式升级 Swift 6，最低系统仍为 iOS 17；Xcode 26.4 是当前 KMP 表中的参考组合。
- Wrapper 仅修改分发 URL；未执行 wrapper 任务、替换 wrapper JAR、安装 API 37 SDK 或产生锁文件 / 构建产物。尚不能声称已完成依赖解析、二进制兼容验证或供应链校验。

未运行编译，遵循全局不自行编译规则。全部依赖的真实解析、Android API 26 / 37 运行、iPhone / iPad 和 Desktop 构建结果均未验证。

## 2026-10-25 可选推送补充

Android新增可选 `com.google.firebase:firebase-messaging:26.0.1` 主模块，版本及最低API24由[官方发布记录](https://firebase.google.com/support/release-notes/android)核对，与本工程最低API26相容；不引入KTX／Analytics或google-services插件。默认公开应用配置为空、自动初始化关闭，用户保存许可后才初始化。未下载／解析SDK产物、未执行Gradle；这不是依赖解析或二进制兼容性结果。接线、配置与剩余验证见[续作交接](REMAINING_CODE_IMPLEMENTATION_2026_10_25.md)。
