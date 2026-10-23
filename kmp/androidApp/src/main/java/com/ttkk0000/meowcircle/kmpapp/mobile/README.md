# Android 手机／平板 UI

现行入口使用批准的 Figma V2 默认浅色，Android手机／平板同一个 Store，苹果 UI 在独立目录。

- 主题：`MobileTheme.kt`；尺寸：`MobileUi.kt`。
- 纯视觉／输入组件：`MobileComponents.kt`；业务表单适配：`AndroidFeatureContent.kt`。
- 窗口／铰链：`AndroidAdaptiveWindow.kt`；框架和五主入口：`AndroidMobileApp.kt`。
- 日期、人员、宠物、区域、图片、通用读取状态分别由 `AndroidDateFields`、`AndroidPeopleFields`、`AndroidPetFields`、`AndroidRegionFields`、`AndroidMedia`、`AndroidLoadStates` 复用。
- `MobileViewModel` 保存业务 owner 与 `AndroidUiSessionState`；不要按横竖屏新建 Store，不把密码或私密正文放进系统 Bundle。

完整[组件目录与维护规范](../../../../../../../../../../docs/client/UI_COMPONENTS.md)、[横竖屏状态交接](../../../../../../../../../../docs/client/PAD_STATE_AND_UI_2026_10_23.md)。项目根的禁止编译／运行规则仍适用，文档不是运行授权。
