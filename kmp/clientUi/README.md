# M&D 公共页面与组件

> **2026-10-27 现行入口**：[源码与文档基线](../../docs/CURRENT_BASELINE.md) · [产品／交付状态](../../docs/PRODUCT_AND_DELIVERY_MATRIX.md) · [全部文档目录](../../docs/DOCUMENTATION_CATALOG.md) · [Figma文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3)。当前253路径／285操作／139服务端schemas、141Web schemas、87对DTO、45张mobile表／13迁移、28份shared／41份公共UI文件、48个FeaturePage；这些是源码口径。本文带日期的旧数量和“待实施”结论仅适用于当时批次，运行／全量验收未完成。

此模块供 Android 手机／平板、iPhone／iPad 与 Compose Desktop 复用消费者页面。业务、权限、版本、幂等和草稿合同仍只在 `shared/client`；Web 使用自己的 React DOM，按同一设计及 v2 合同实现。

- `ConsumerApp`：五主入口、详情、聊天与当前窗口布局。一个窗口仅有一个 `MobileStore`，改变布局不另建业务状态。
- `ClientTheme`／`ClientMetrics`：Figma V2 默认浅色、Noto Sans SC／Manrope、16 卡片、48 操作、820 正文、144 表情区域。
- `Components`：10 个无 Store 的 primitive。
- `PageComponents`／`FormLayout`／`TaskDialogs`／`LoadStates`：持久任务操作、表单分组、主从列表、选择器、弹层和加载／失败／空／分页。
- `SocialPages`／`CarePages`／`OfferPages`／`AccountPages`：42 个扩展功能入口，保持已有服务器资格、当前版本、范围确认与未知结果恢复逻辑。
- `GuardianPages`：短时邀请、独立本人领取、关系核实后范围确认、撤回和保护设置；不把手机号或领取动作当作监护关系。
- `AgePages`：年龄范围、成年用途告知、通道／原请求状态及返回原任务核对；默认通道未配置不放行。
- `ProviderAuthPages`：官方授权、主动账号选择／创建、用途验证与关联确认，登录与安全设置共用。
- `ClientAssets`：Figma 原 SVG、128 枚 Noto 原图、24 张贴纸与指南图片；业务照片仍从鉴权 API 读取，失败可重试。
- `ClientUiSession`：当前账号的 UI 内存与 transient 流程；密码、验证码和正文不写系统恢复 Bundle。
- `ClientUiPlatform`：窗口、返回、照片选择、日期、地图、通知权限、解码和外部链接端口。Android 保留铰链与系统返回；iOS 按 Scene 窗口；Desktop 按当前窗口收起侧栏。

平台入口分别在 `androidApp/.../AndroidClientPlatform.kt`、`iosUi/.../IosClientPlatform.kt`、`desktopApp/.../DesktopClientPlatform.kt`。Android／iOS 地图仍是原生高德 SDK；Desktop 打开同源 Web 地图，账号凭据不进入链接。

2026-10-24：源码迁移和静态检查已做。没有运行依赖解析、编译、类型检查、测试、设备或浏览器；740 条设计状态的源码归属表不能当成逐状态验收。2026-10-25已补Android／iOS官方微信／QQ桥、Apple与Web回跳和账号关联源码，应用配置按用户要求使用占位符；Desktop官方回跳、真实渠道及逐状态视觉／交互验证仍未完成。详见 `docs/client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md`。
