# Android 手机／平板原生宿主

2026-10-24消费者页面已迁移到独立 `kmp/clientUi` 公共库，手机、平板、iOS和Desktop复用页面组件及已有共享业务Store。当前默认Figma浅色，不改旧Stitch镜像。

- `AndroidMobileApp`：薄入口，注入 `AndroidClientPlatform`，不创建第二个业务Store。
- `MobileViewModel`／`AndroidMviBase`：保留Gemini MVI聚合、生命周期、深链队列与恢复；UI会话和待查看深链值使用公共类型别名。
- `AndroidAdaptiveWindow`：真实系统栏／IME／铰链；公共正文按当前可用区域调整主从与侧栏。
- `AndroidPhotoPicker`／`AndroidImageOrigin`：真实照片与来源元数据；`AndroidDateFields`：系统日期／时间确认；`AndroidNotificationPermission`：真实系统权限。
- `AndroidSecretStorage`／`AndroidComposeDraftFiles`：Keystore及私有草稿；`MndApplication`：进程API刷新和跨Activity草稿协调。
- `AndroidNativeMap`：原生高德MapView；地图列表与CaseMapPicker在公共库，仍不新增业务Store。

页面、主题、尺寸、任务骨架与组件见 `kmp/clientUi/README.md` 和 `docs/client/UI_COMPONENTS.md`。禁止构建／测试／启动规则仍适用，当前只有源码／只读检查证据。
