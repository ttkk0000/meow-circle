# M&D iPhone／iPad Compose宿主

当前目标由 `ComposeApp` 薄Swift宿主、`iosUi` 系统桥、`clientUi` 公共页面和 `shared/client` 业务组成。旧 `MndIOS` SwiftUI文件保留参考，不参与当前sources。最低iOS17，默认浅色，手机／平板按Scene窗口适配。

当前打包、签名、Simulator／真机安装和CI入口统一见[交付手册](../../docs/client/DELIVERY_GUIDE_2026_10_25.md)。

- `python3 scripts/mobile.py ios prepare`：XcodeGen后安装Pods，得到 `MndIOS.xcworkspace`；有锁文件时使用deployment模式。
- `ios simulator`：arm64 Simulator构件；`ios archive`：本人Team与证书签名的Archive；`ios export`：依据私有ExportOptions导出IPA。
- `ios install/run --device ...`：安装或启动已有构件；真机加 `--physical` 并指定可安装的签名 `.app`。
- Xcode预构建脚本调用 `:iosUi:embedAndSignAppleFrameworkForXcode`，链接 `MndMobileUI.framework`。该任务需要Xcode提供的SDK／configuration环境，不能用旧shared目标代替。
- API、原生地图、官方授权AppID及Universal Link均从本次构建环境提供；Secret不进App。默认占位不产生真实授权。实际启用的Apple／Associated Domains能力在Archive时生成对应entitlements。

本轮未生成Xcode工程、解析Pods、编译、签名或安装，也未伪造Podfile.lock。真实SDK解析／ABI、iPhone／iPad布局和运行仍待获准环境验证。地图与页面源码边界见[全端进展](../../docs/client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md)。
