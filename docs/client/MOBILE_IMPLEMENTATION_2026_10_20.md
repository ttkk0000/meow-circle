# Android / iOS 手机和平板实施记录

更新：2026-10-20。用户已从“先写文档”转为授权 Android / iOS 客户端代码实现；构建、测试、安装和设备运行仍未授权。

工作树：`J:\GolandProjects\kitty-circle`，`main` / `ebfc469`。保留既有在途服务端、旧 Android、旧 SwiftUI 和桌面代码。

## 本次顺序

1. 服务端静态核对，见[接入前核对](../server/CLIENT_READINESS_2026_10_20.md)。结论为主业务源码可接入、生产未验收。
2. C01–C06：独立 v2 DTO / Transport / 安全存储端口 / 会话轮换 / 任务导航与 Store，不复用 v1 token。
3. A01 / I01：Android Compose 与独立 iosUi Compose 模块，Swift 只做宿主和 Keychain 适配。
4. A04 / I04：按窗口尺寸分别选择手机、平板竖屏 / 窄分屏、平板横屏布局，保留共享任务与草稿。
5. 先接真实游客浏览、已有账号密码登录、登录返回、申请编辑与草稿；对外交流等待本轮发现的 B04 认证合同差距修复。逐项记录覆盖，不把所有设计状态预先标完成。

## 视觉实施决定

本次用户要求按已完成的手机设计实施。新移动入口采用 Figma 手机 V2 默认浅色及 `CROSS_PLATFORM_ADAPTIVE_CONTRACT.md`；这条范围明确的决定优先于旧 Stitch 四主题默认行为。`.stitch` 作为历史镜像原样保留，不冒称远程已同步。旧主题与背景偏好继续保留，当前页面无主题切换或系统暗色。

用户中途明确 Android 面向 **Android 17，最低 API 26**。已按[官方 SDK 配置](https://developer.android.com/about/versions/17/setup-sdk)设应用 `compileSdk = 37`、`targetSdk = 37`，共享库 `compileSdk = 37`，两者 `minSdk = 26`。

用户随后要求全部升级到最高可用版本。最终版本、官方仓库实际查询结果、稳定 Material3 / 冻结图标例外和 KGP 精确补丁兼容边界均见[依赖升级基线](DEPENDENCY_BASELINE_2026_10_20.md)。当前 AGP 9.3.3 / Gradle 9.7.1 / Kotlin 2.4.21 / CMP 1.12.2，其他直接依赖也已升级；整体组合未构建验证。

Android 应用使用 AGP 内建 Kotlin，移除旧 `kotlinAndroid` 插件；shared 改 `com.android.kotlin.multiplatform.library` 与 `kotlin.android {}`，不通过压制兼容警告或长期关闭新 DSL 绕过迁移（[官方 KMP 插件](https://developer.android.com/kotlin/multiplatform/plugin)、[内建 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)）。应用源文件仍在 `src/main/java`；shared Android 源原本已在 `src/androidMain/kotlin`。

采用官方 [ComposeUIViewController 集成方式](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html)，Xcode 源描述配置高刷新率键及 iPhone / iPad 方向。此为源码接线方案，macOS / Xcode 实际兼容性仍待构建验证；没有运行 Wrapper、安装 API 37 SDK 或下载构建依赖。

## 已写源码与边界

| 范围 | 当前源码 | 状态 |
|---|---|---|
| C01 / C02 / C08 | Gradle 新 Android KMP 插件、独立 iosUi、Compose iOS framework / Swift 薄宿主；两端默认浅色，旧主题保留 | 基础源码已写，字体素材与完整视觉还原未完成 |
| C03 / C04 / C05 | `shared/.../client/Models.kt`、`MobileApi.kt`：具体响应类型、Long ID、v2 包装、401 串行轮换、取消透传、账号变化检查；Android Keystore / Swift Keychain；Application / IosClient 持有同进程刷新协调器，各窗口独立 Store | 部分功能合同已写，邮箱 / OAuth / B04 状态尚缺，不能称全部合同冻结 |
| C06 / F00 | `MobileStore.kt`：唯一共享状态、目的地 / 返回、游客浏览、登录返回、页游标、过期结果与账号状态清理 | 第一批状态已写；进程恢复、深链与完整导航栈尚缺 |
| F02 / F04 / F07 / F09 | 社区 / 领养 / 我的宠物 / 走失列表与详情、评论读取、申请列表与双方角色操作视图、申请编辑 | 读取 / UI 源码已接；对外提交受 B04 阻塞，其他闭环仍未覆盖 |
| F05 / F06 / F12 | 私信 / 群会话与消息读取、真实未读数、显式已读、站内通知列表与部分目的地 | 群管理与完整通知配置尚缺，系统推送受 B03 阻塞 |
| C07 / F10 | Android Photo Picker / iOS PHPicker、方向 / 尺寸处理、最多 9 张、共享 2 并发上传队列、同键重试 / 删除 / 排序、版本化云端草稿 | 源码已写；未上传文件仍仅在进程内，应用被终止后的本地文件恢复尚缺；公开发布受 B04 阻塞 |
| F11 | 读取服务的 128 个 Unicode 表情、平台光标插入、连续编辑和显式发送 | 贴纸预览 / 发送、收藏 / 最近及全部输入场景尚缺 |
| A01 / A02 / A04 | `androidApp/.../mobile` 独立 Compose UI、ViewModel 持有共享 Store、按实际宽度切换底栏 / 侧栏 / 上下文栏、Insets 与系统返回 | 第一批手机 / 平板结构已写，不代表平台验收 |
| I01 / I02 / I04 | `iosUi` 独立 Compose UI；`iosApp/ComposeApp` 只含宿主与 Keychain；iPad 旋转 / 窄分屏按 Scene 宽度布局 | 第一批结构已写；旧 `MndIOS` 原样保留、未列入新目标源码；手势 / 外接键盘等尚未验收 |

## 最新认证决定与阻塞

读取到同日新增的“同一账号三标识 / 普通登录免强制短信 / 对外交流前验证当前手机号”规则后，移除了新 UI 中的旧手机号验证码自动建号入口。暂可使用服务真实支持的已有账号名或手机号与密码；邮箱不会被送进旧 username 字段冒充支持。

`requireInteractionContract()` 明确阻止新客户端的对外发布 / 评论 / 私信 / 点赞 / 领养提交 / 关系决策；读取、编辑与私有草稿仍可进行。它是 **B04 缺失期间的集成阻塞**，不是服务端权限实现。不得把它改为恒 true 或加本地“已验证”开关抵扣服务端开发。需要服务端先交付真实资格响应及写关卡，再由共享逻辑接入，保留任务与输入并让用户再次提交。

本轮用户另询问“绑定微信是否可获得发帖资格”。已解释：微信绑定不等于姓名 / 身份证实名认证；“已验证手机号或服务端确认的微信绑定满足其一”可以作为产品准入方案。此时尚无进一步明确确认，不静默覆盖认证规划中的既定规则，也不把微信登录成功直接当成服务器认可的发帖资格。

## 尚未实现的主要项

F01 新注册、邮箱、真实第三方 SDK、敏感账号操作与验证流程；F03 好友与圈子受众；F04 宠物编辑 / 角色协作；F06 群管理 / 群主转让；F07 完整试养 / 交接后近况；F08 预约版本工作区；F09 发布 / 线索 / 认领 / 双方交接；F11 贴纸完整功能；F12 配置与推送；F13 核实 / 举报 / 申诉；F14 出售披露 / 到访反馈。两端完整状态覆盖、正式字体资源、无障碍、系统链接及设备性能均未验收。Web / Desktop 业务迁移未在本轮展开。

## 静态检查与运行边界

- 已审阅并执行只读脚本 `scripts/check_mobile_static.py`：32 表、167 操作、50 schema 与表情素材引用通过。
- 已编写并执行只读脚本 `scripts/check_kmp_client_static.py`：19 个新增 Kotlin / Swift 文件的 UTF-8 / 基础定界符、catalog 引用、API 26 / 37、iOS Compose 接线、请求字段与服务合同检查通过。定界符检查不是语言解析或编译。
- 已编写 4 个共享网络回归用例源码：并发 401 单次轮换、409 不重放确认、取消不清会话、int64 / 隐私字段投影。**未执行这些测试。**
- `git diff --check` 对本轮 KMP / 交接差异无空白错误。未安装依赖、未运行 Wrapper / Gradle / Go / XcodeGen / 服务 / 模拟器 / 设备，未提交或推送。

未运行编译，遵循全局不自行编译规则。本次是第一批源码与升级交付，**Android / iOS 全功能尚未完成，B04 与设备运行验收也未解决**。
