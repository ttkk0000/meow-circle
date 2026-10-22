# iOS 入口

当前实际入口已是 `../iosUi` Compose UI + `../iosApp/ComposeApp` 薄宿主 / Keychain + shared/client。第一批源码已写，完整功能未完成、未构建；见[实施记录](../../docs/client/MOBILE_IMPLEMENTATION_2026_10_20.md)。下文保留旧 SwiftUI 迁移历史。

当前规划（2026-10-20）已改为独立Compose iOS UI＋KMP共享逻辑，iPad分别适配；详见[客户端文档](../../docs/client/README.md)。下面是现有SwiftUI壳的历史工程入口，尚未迁移完成。当前只实现设计默认主题，旧多主题保留参考。

完整的 SwiftUI + KMP 基础架构已经迁到 [`../iosApp`](../iosApp/README.md)。

保留本目录作为旧文档入口，避免已有链接失效。新的 iOS 壳包含：

- XcodeGen 工程描述；
- `IosSdkBridge` 的可取消回调桥接；
- Swift 单一 State/Action/Effect；
- 每个 Tab 独立的 `NavigationStack`；
- 与 Android/Web 一致的 Honey、Mint、Night、Neutral 主题令牌；
- Feed、Market、Messages、Orders、Profile 五个基础页面。

## 旧 shared framework 打包入口

本地分支保留根目录 `make package-kmp-ios-framework`，只针对旧 `shared` framework，不能代替当前 `iosUi`／`ComposeApp` 的完整打包。此处仅记录入口，本次未运行构建；当前实施与验收仍以文首交接为准。
