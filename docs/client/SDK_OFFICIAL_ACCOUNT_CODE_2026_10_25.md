# SDK配置核对与公众号回调源码（2026-10-25）

任务：`AI-20261025-SDK-OFFICIAL-ACCOUNT-CODE`。用户已选择个人主体，随后要求暂停注册、先只写代码。没有创建账号／应用或获得真实AppID／Key；没有进行平台认证、付费、应用审核或接受注册协议。注册页面状态见[暂停记录](PLATFORM_REGISTRATION_2026_10_25.md)。

## 本轮代码

- `internal/platform/wechatofficial`：独立公众号回调，地址为 `/integrations/wechat/official/callback`。默认关闭返回404；显式启用但资料不完整则服务器配置报错。接入 `internal/platform/api/router.go`，不属于消费者OpenAPI，不改变现有245路径／275操作合同或数据库结构。
- GET校验Token／时间戳／nonce签名后原样返回 `echostr`。POST仅接收 `encrypt_type=aes`：验签后进行AES-256-CBC解密，检查32字节PKCS7填充、帧长度、尾部AppID和内外两层公众号原始ID；回复同样加密签名。Token不是公众号API `access_token`，不与登录AppSecret混用。
- 关注事件回复欢迎和社区入口；文本“帮助”“菜单”“help”回复入口说明；其它消息／取消关注仅确认 `success`。只做被动回复，不保存OpenID、正文、关注状态或精准位置，不调用身份／业务存储，不主动发送通知。重试没有累加业务写入。
- 请求查询不超过8192字节，消息体不超过64KiB；拒绝重复查询参数、过期或未来超过5分钟的时间戳、压缩消息、XML重复字段／DTD／命名空间／深于16层的结构。回调不写正文、查询或凭据日志；响应不缓存。
- `envfile.LoadServer()`新增Token与AESKey的 `_FILE` 读取，沿用直值／文件互斥。现有Compose已挂载 `provider-secrets`，本轮仅补运行模板，无新服务、worker或迁移。
- Android微信入口增加与其它端一致的AppID形状检查；仍拒绝零值占位。原有微信／QQ各端SDK、Web／Desktop回跳和高德原生／Web源码保持，真实平台联调尚未进行。

回调不会自动创建、登录或关联M&D账号，不以公众号关注证明同意、年龄或监护授权。个人主体当前能开通哪些公众号能力未取得平台核实；本轮没有新增公众号网页OAuth、JS-SDK票据、支付、自定义菜单发布、群发或小程序客户端。

## 配置对照

以下仅为变量名与占位位置，不填写真实凭据：

| 能力 | 客户端公开配置 | 服务端配置／条件 |
|---|---|---|
| Android微信／QQ | `MND_WECHAT_ANDROID_APP_ID`／`MND_QQ_ANDROID_APP_ID`，见[Android模板](../../deploy/examples/android.env.example) | 同值的 `WECHAT_ANDROID_LOGIN_APP_ID`／`QQ_ANDROID_LOGIN_APP_ID`，Secret只在服务器；正式包名与发布签名须匹配平台登记 |
| iOS微信／QQ | `MND_WECHAT_IOS_APP_ID`／`MND_QQ_IOS_APP_ID`和各自Universal Link，见[iOS模板](../../deploy/examples/ios.env.example) | 同值的 `WECHAT_IOS_LOGIN_APP_ID`／`QQ_IOS_LOGIN_APP_ID`及各自Secret；真实Bundle ID／Team／域名关联 |
| Web登录 | `VITE_MND_WECHAT_WEB_APP_ID`／`VITE_MND_QQ_WEB_APP_ID` | 对应 `WECHAT_WEB_LOGIN_APP_ID`／`QQ_WEB_LOGIN_APP_ID`及Secret，回跳固定本域HTTPS `/app/provider-callback` |
| Desktop登录 | `MND_WECHAT_WEB_APP_ID`／`MND_QQ_WEB_APP_ID`，见[Desktop模板](../../deploy/examples/desktop.env.example) | 复用已登记的Web应用与公开HTTPS桥 `/app/desktop-provider`；本地临时回跳不是平台登记域名 |
| Android／iOS地图 | `MND_AMAP_ANDROID_KEY`／`MND_AMAP_IOS_KEY` | 各端Key分别申请，发布签名／Bundle ID匹配；KMP继续处理公开粗点与坐标来源 |
| Web／Desktop地图 | `VITE_MND_AMAP_JS_KEY` | 同值的 `MND_AMAP_JS_KEY`及服务器专用 `MND_AMAP_JS_SECURITY_CODE`；安全码不进入Web构件 |
| 公众号被动回调 | 无客户端凭据 | 独立的 `MND_WECHAT_OFFICIAL_*`，不能把App／Web开放平台AppID直接视作公众号AppID |

公众号配置位于[服务器环境模板](../server/environment.example)和[部署运行模板](../../deploy/runtime.example.env)：

```dotenv
MND_WECHAT_OFFICIAL_ENABLED=0
MND_WECHAT_OFFICIAL_APP_ID=
MND_WECHAT_OFFICIAL_ORIGINAL_ID=
MND_WECHAT_OFFICIAL_TOKEN_FILE=
MND_WECHAT_OFFICIAL_ENCODING_AES_KEY_FILE=
```

启用前需本人在公众平台取得公众号AppID、原始ID（`gh_…`）、Token和43字符EncodingAESKey；Token与AESKey写入仓库外的私有文件，将 `_FILE` 指向容器内 `/run/provider-secrets/…`。不要同时配置直接值。确保 `MND_WEB_ORIGIN` 为正式HTTPS站点；服务器地址填本域回调路径，消息加解密选**安全模式**，最后才将 `ENABLED` 改为1。服务器时钟需同步。没有真实资料时保留0和空值，不生成假Key，也不复制AppSecret凑配置。

本模块只实现公众号协议，不实现企业微信的加密 `echostr` 验证。GET地址校验和POST消息接收需分别验收，GET通过不能证明消息加解密或平台权限已通过。

## 验证与接续

本批补写6个Go回归函数，覆盖关闭／配置缺失、GET签名／时间／重复参数、安全模式加密回复、降级／篡改／跨账号、填充／帧及XML边界。它们是测试源码，**未执行**，不能用自加解密的源码案例替代平台互操作验收。

协议实现研究参考[WxJava加解密源码](https://github.com/binarywang/WxJava/blob/develop/weixin-java-common/src/main/java/me/chanjar/weixin/common/util/crypto/WxCryptUtil.java)的帧结构、摘要参数和回复封装；未引入该Java库或复制其实现。微信官方开发文档入口本轮无法回读，未绕过此前开放／公众平台注册的浏览器安全拦截。仍需在获准环境对照平台当时的正式文档与实际回调复核。

本批仅运行Go格式／语法解析、只读源码接线检查、冲突扫描与 `git diff --check`；没有编译、类型检查、测试执行、SDK解析、服务／App启动、迁移、CI、部署、提交或推送。Android／iPhone／平板及Web的真实登录、地图Key／域名、公众号实际权限和全端运行验收仍待正式配置。
