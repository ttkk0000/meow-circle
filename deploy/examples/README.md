# 部署与App签名占位配置

2026-10-25：按用户要求先提供占位配置；未创建服务器、证书、密钥或运行环境，也未获得构建／CI执行确认。

| 文件 | 填写内容 |
|---|---|
| [server-target.env.example](server-target.env.example) | 实际服务器地址、部署用户、SSH密钥路径与域名；仅登记，脚本不自动登录服务器 |
| [compose.env.example](compose.env.example) | Compose公开参数及主机目录；带 `MND_CONFIG_MODE=placeholder` 标记 |
| [android.env.example](android.env.example) | Android API地址、版本、keystore路径／alias及凭据变量，保留官方登录占位AppID |
| [ios.env.example](ios.env.example) | iOS API地址、版本、Team、签名方式、能力和官方登录占位AppID |
| [desktop.env.example](desktop.env.example) | Desktop运行端点、条款与复用Web应用的公开AppID；不含Secret |

API／供应商参数继续使用 [runtime.example.env](../runtime.example.env)，IPA选项参照 [ExportOptions.example.plist](../../kmp/iosApp/ExportOptions.example.plist)。没有放假密码、假证书或可误认为真实结果的签名文件。

这些 `.example` 文件只供填写，不会被交付脚本自动加载。Compose示例即使被复制为 `compose.env`，带占位标记时也会在调用Docker前停止；Release打包已有真实HTTPS、签名／Team校验，原有检查保留。

后续准备真实环境时：

1. 在仓库外或被忽略的私有目录填写实际服务器／域名及签名配置，凭据由本机私有配置或Secret管理器提供，不提交回此目录。
2. 用实际域名与明确镜像版本执行交付手册里的init流程，生成私有生产配置。不要仅删除占位标记就把示例当成完整生产配置；Secret、正式材料和实际签名仍须落实。
3. 将Android／iOS配置作为构建进程的环境变量提供，按[交付手册](../../docs/client/DELIVERY_GUIDE_2026_10_25.md)选择构建／安装命令。脚本不自动source任何环境文件。

当前只生成了文本模板。没有执行init、构建、签名、安装、启动、部署、CI或推送。
