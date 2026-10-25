# 官方登录与账号关联源码（2026-10-25）

本页记录 `AI-20261024-ALL-FIGMA` 的授权阶段。用户已确认应用信息**先用占位符**；没有取得、写入或上传真实密钥。源码和静态核对完成不等于 SDK 已解析、编译、签名或联调通过。尚未宣称全部 Figma 状态完成。

## 入口与共享职责

- `shared/client/ProviderAuthStore.kt`：官方授权、服务端核验、账号选择、独立创建确认、原账号验证、用途验证、关联确认、取消／不可用／过期／冲突／结果待核对。短时挑战、resolution 和用途 grant 仅驻留私有内存，不进 UI State、草稿、日志或保险库。
- `clientUi/ProviderAuthPages.kt`：Android、iOS 和 Desktop 共用任务页面；登录与安全设置复用 `ProviderEntry`，固定底部动作复用 `TaskPrimary`／`TaskSecondary`，公共任务支持固定工具栏。
- Android：`AndroidProviderAuth.kt`／`wxapi/WXEntryActivity.kt` 使用官方 SDK。授权 Activity 非导出，微信约定回跳导出；按原挑战匹配。QQ Server-Side 回调将 code 放在 SDK 的 `access_token` 字段，交服务端时仍作为 code。取消后保留旧 QQ 原监听和并发槽直到原 SDK 返回，防止旧回跳串到新授权；若官方界面始终不返回，当前进程不重新发起 QQ，可用其他登录方式。
- iOS：`NativeProviderAuth.swift`／`MndProviderBridge.*` 和 `IosProviderAuth.kt`。微信／QQ 官方桥及系统 Apple 登录；Apple 同时传一次性授权码和 ID Token，服务端仍换取并核验其凭据及可撤销 refresh token。Scene 回跳通过同一 factory 协调，不创建新的业务 Store。
- Web：`ProviderSignIn.tsx`／`core/provider.ts` 复用登录与安全设置。用户手势打开官方 Web code 授权窗口，清空 opener；同源随机 state 的 BroadcastChannel 回传。回跳页先移除 URL 查询，校验 origin、state、内容类型和长度；关闭后的迟到 start 响应只取消原挑战，不触发登录。主操作固定在公共 Modal 底栏。
- Desktop 当前没有官方 SDK／浏览器会话向本机交付链路，入口如实显示不可用；已有密码／邮箱／手机渠道与所有公共状态页面保留。不能把共用页面等同于 Desktop 官方授权已接通。

## 服务端边界

`mobileauth/tencent_apps.go` 按部署方配置的 AppID 和 Android／iOS／Web 选择应用，客户端无法指定 Secret、供应商地址或任意回调。保留明确配置的旧默认应用兼容，Web 不退回移动应用；重复 AppID 的冲突 Secret 关闭该供应商。身份键仍是 provider + appID + openID，不依据昵称、头像、unionID 或返回邮箱自动合并。

Web 适配器仍先校验 Cookie、Origin、CSRF，固定 `client=web` 和 `device=web-v2`，只允许本部署正式 Web AppID／同源 HTTPS 回跳。OAuth 嵌套 `data.login` 的 token 和 refresh token 封入原 HttpOnly、Secure、SameSite=Strict Cookie，返回 JS 的 login 只含安全用户资料。resolution／用途 grant 是短时关联能力，只保存在当前页面内存。

已登录账号尝试关联另一账号身份，或邮箱／手机已有绑定，返回 `identity_in_use`；保留原绑定，不投影另一账号。原生和 Web 登录、绑定及回跳复用冲突说明。创建和关联必须分别主动确认；网络／服务端未知结果不能自动重发。

## 固定的官方分发

| 平台 | 固定版本与来源 |
|---|---|
| Android 微信 | Maven Central `com.tencent.mm.opensdk:wechat-sdk-android:6.8.40` |
| Android QQ | 官方 3.5.19 ZIP，唯一 `open_sdk_3.5.19_r9483ffc7_lite.jar`；构建任务校验 SHA-256 `6272d4123d8327400a655c48f1fb7eb24b83fd5beeb527951ed100b219192b9b` |
| iOS 微信 | 官方 CocoaPods `WechatOpenSDK-XCFramework:2.0.9` |
| iOS QQ | 官方 3.6.20 ZIP，本地 podspec 指向官方 HTTP source，并校验 SHA-256 `77c4c9e4b0f064c5a304e00d036280f6cf47d43ae0fc24ab99785d4659ce6198`；抽取原 XCFramework |

QQ 新版本不使用旧 Maven／CocoaPods 中央条目或第三方重打包；[官方下载目录](https://wiki.connect.qq.com/sdk下载/)、[Server-Side 模式](https://wiki.connect.qq.com/server-side登录模式/)、[code 换取凭据](https://wiki.connect.qq.com/使用authorization_code获取access_token/)。只读取了发布元数据、原档案、Android class 方法表和 iOS 头文件；没有安装、加载或执行这些 SDK。Gradle 下载任务与 CocoaPods prepare 均未执行。

## 替换占位符的位置

| 位置 | 公开配置与缺省行为 |
|---|---|
| Android 构建属性／环境 | `meow.wechat.app.id`／`MND_WECHAT_ANDROID_APP_ID`，`meow.qq.app.id`／`MND_QQ_ANDROID_APP_ID`；默认 `wx0000000000000000`／`00000000`，不可发起 SDK |
| iOS `project.yml` | `MND_WECHAT_IOS_APP_ID`、`MND_QQ_IOS_APP_ID`、两项 Universal Link 和 `MND_PROVIDER_ASSOCIATED_DOMAIN`；默认 example.invalid／零值，不初始化 SDK。`MND_APPLE_SIGNIN_ENABLED=NO` |
| Web `.env.example` | 两个 `VITE_MND_*_WEB_APP_ID` 与 `VITE_MND_OAUTH_REDIRECT_URI`；同源 HTTPS `/app/provider-callback` 才可用，零值不可用 |
| 服务端 `environment.example` | `WECHAT/QQ_{ANDROID,IOS,WEB}_LOGIN_APP_ID`／`APP_SECRET`，Web 另设 `REDIRECT_URI`。Secret 只在部署环境；Web 构建只接公开 ID／回调 |

iOS 已写 URL Schemes、queries 和 associated-domains／Apple Sign-In entitlement 源码。正式应用仍需实际平台审核、AASA 文件、AppID／签名映射及对应 provisioning；本轮没有修改 Team、签名身份或注册能力。Android 也需要平台登记的真实包名与证书摘要。占位符不是正式应用登记或联调证据。

## 验证与剩余工作

10 个只读脚本通过，包含新增 `check_provider_auth_static.py`；当前为 32 公共源码、39 扩展分派、43 TS/TSX、196 本地引用、66 对 DTO。95 份变更 Kotlin／KTS／TS／TSX 语法树无新增错误；四个原有大型 shared 文件仍有与 HEAD 同类的 grammar 限制，不据此声称全文件语法或类型正确。新增三个 Go 回归文件仅为源码，未执行测试。

Figma 本阶段已取原上下文／截图：`460:17333`、`460:17338`、`238:371727–371729`、`238:371731`、`238:371737–371738`，见 [原始证据](../design/figma-study/implementation-2026-10-24)。这些是设计核对，不是应用运行截图；740 状态索引仍无逐状态运行验收。

未运行构建、类型检查、test、lint、Pods／依赖解析、工程生成、浏览器／模拟器／应用／服务。SDK／Swift ABI、SDK 资源与链接、真实回跳、并发和恢复仍需获得运行授权后的分别验证。继续核对年龄资格等尚被宽泛源码族映射覆盖的专用流程。未提交、推送或重试 Git 身份验证。
