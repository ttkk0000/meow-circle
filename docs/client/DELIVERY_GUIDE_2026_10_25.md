# 部署、打包与安装运行入口（2026-10-25）

任务 `AI-20261025-DELIVERY`。本批补齐配置和脚本，以下命令均为获准环境中的操作说明，**本轮没有执行构建、迁移、安装、启动或发布**。已有 AppID 占位符保留；真实域名、证书、供应商与运营资料由部署方提供。源码接线不能代替实际运行验收。

用户后续要求先用占位配置，模板集中在 [deploy/examples](../../deploy/examples/README.md)。这是文本示例，不代表服务器或签名已配置；没有将该回复当作运行授权。

2026-10-25后续源码补齐：设备推送、Desktop官方回跳、S3端口、8个worker与011／012迁移见[续作交接](REMAINING_CODE_IMPLEMENTATION_2026_10_25.md)。原批次证据保持历史快照。

## 当前入口

| 交付物 | 入口 | 输出／行为 |
|---|---|---|
| API／Web／worker／迁移／健康探针镜像 | `Dockerfile`、`scripts/delivery.py build` | 单镜像；Web先类型检查再打包，Go二进制运行在nonroot distroless |
| 生产部署 | `deploy/compose.production.yml`、`delivery.py up` | Postgres、带认证Redis、迁移、API、Caddy HTTPS及8个worker，共13服务 |
| Android | `scripts/mobile.py android` | Debug APK、签名Release APK与AAB、指定设备安装／启动 |
| iPhone／iPad | `scripts/mobile.py ios` | XcodeGen＋Pods、Simulator app、Archive、IPA、指定模拟器／真机安装启动 |
| Desktop | `scripts/mobile.py desktop` | 当前操作系统的DMG／MSI／DEB构件，或开发运行 |
| CI | `.github/workflows/ci.yml`、`app-packages.yml` | 常规Go／Docker／Web及完整迁移；App构件仅手动workflow，未触发 |

旧 Expo 页面和打包目标保留作历史入口；当前 App 使用 KMP／clientUi。Makefile 新增 `delivery-*` 和 `app-*` 目标，旧 iOS framework 目标已改为 `iosUi`。移除仓库内机器专属 `org.gradle.java.home`，由执行环境提供 JDK 17。

## 生产部署

执行机器需要 Docker Engine／Compose 插件和 Python 3.10+。实际域名的DNS、80／443访问和证书签发条件需要准备好；API不直接映射公网端口。数据只位于持久卷，Redis／Postgres不映射主机端口。本方案仍为单机部署拓扑；源码已补跨副本刷新回执与数据库作业协调，不代表整套高可用已运行验收。

先把 `MND_DOMAIN` 设成实际域名，选择明确镜像版本：

```bash
python3 scripts/delivery.py init --domain "$MND_DOMAIN" --image mnd:20261025
```

默认生成被Git及Docker忽略的 `deploy/local`；已有目录拒绝覆盖，避免意外轮换会话／数据库密钥。其他私有目录必须位于仓库外，用 `--directory` 显式指定。文件包括：

- `compose.env`：域名、镜像版本、源码及部署目录路径、公开Web构建参数。
- `runtime.env`：实际供应商运行参数，初始为空或关闭；参照 `deploy/runtime.example.env` 和 `docs/server/environment.example`。
- `secrets/`：数据库、Redis、身份密钥、后台服务身份及各job独立凭据。未来执行init时才随机生成，不存在已生成或已启用的凭据。
- `provider-secrets/`：可选供应商Secret文件；API挂到 `/run/provider-secrets`。对应 `NAME_FILE` 与直值互斥，不把AppSecret注入Web构建。
- `reading/`：自行提供经发布核对的正式材料JSON；将 `MND_READING_CATALOG` 设为容器中的 `/app/reading/文件名.json`。没有正式材料时保持草案／受限，不自动发布。
- `associations/.well-known/`：实际 `assetlinks.json` 和 `apple-app-site-association`；不生成假包签名或假Team绑定。

主机部署父目录0700，Compose Secret文件0444供容器nonroot读取，主机其他用户不能穿越私有父目录；`runtime.env` 为0600。可选Secret目录本身需容器可遍历，文件需nonroot可读。不要把私有目录放入Web目录、备份公开目录或源码仓库；换机保留稳定密钥，并更新 `compose.env` 中的路径，不重跑init覆盖旧配置。

init只配置六个独立后台服务身份：push／nearby_alerts／content_updates／privacy_cleanup／provider_revocations／maintenance，逐job最小角色与独立游标卷；不创建消费者账号、人工核验人员或虚构排班。人工角色需另行登记真实具名人员。

```bash
python3 scripts/delivery.py build
python3 scripts/delivery.py up
python3 scripts/delivery.py status
python3 scripts/delivery.py logs --service api
```

`up` 先停止旧Caddy、API和worker，启动数据库／Redis并迁移，成功后才恢复全套服务；不假设旧程序可跨schema在线写入。失败保持停止，不自动回退数据库。worker只走本域名HTTPS，证书／DNS暂不可用时保留原游标重试。Caddy使用服务启动／健康依赖；这遵循[Compose官方启动顺序](https://docs.docker.com/compose/how-tos/startup-order/)。

镜像不含shell/curl，使用专门健康探针请求loopback `/healthz`；该探针证明HTTP存活，不替代数据库故障演练、媒体和真实渠道验收。生产数据目录由镜像准备为UID65532，命名卷首次初始化继承权限。

### 迁移、备份与恢复

迁移入口为 `cmd/migrate`，支持 `DATABASE_URL` 或 `DATABASE_URL_FILE`，不输出DSN／密码。按001起连续编号加载，单连接advisory lock；每文件事务连同SHA-256账本一起提交。007–009的外层BEGIN／COMMIT被剥离，内部DO块保留。文件改变、账本空洞、数据库比源码新时停止。

旧数据库若已存在users而没有账本，必须人工核实已执行范围，显式登记基线，不能猜测：

```bash
python3 scripts/delivery.py migrate --baseline-through 009_pet_discussions.sql
```

该命令只适用于确实完成009的旧库；若只完成008，应填写 `008_mobile_policy_commands.sql`，余下文件才实际执行。基线记录在一个事务中完成；新空库无需基线。既有 `make migrate` 也改为此账本入口，不再盲目重放001–009。

数据库备份入口：

```bash
python3 scripts/delivery.py backup --output /absolute/private-backups/mnd-20261025.dump
```

backup先停止写入口及worker，生成0600的Postgres custom-format备份，不自动恢复服务。还需同期保存媒体卷、六个worker游标卷、Redis持久数据、正式阅读材料、域名关联文件及稳定Secret；应保留同一维护窗口的版本和镜像ID。随后按实际备份策略保存到受控／加密位置，再执行up。停止服务不会删除卷；脚本没有 `down -v`、自动清库或自动破坏性恢复。

恢复先使用隔离环境及匹配Postgres版本，通过 `pg_restore` 导入新空库，恢复媒体、游标和稳定密钥，再运行账本迁移。核对删除／保全／撤权记录、未决operation及实际渠道回执后才切换域名。详细恢复验收沿用 [v2运行手册](../server/V2_RELEASE_RUNBOOK_2026_10_23.md)，不编造RPO／RTO或宣称已演练。

## Android 手机／平板

需要 JDK17、Android SDK Platform37／Build Tools37.0.0及platform-tools。模拟器或真机地址需从该设备可达；Debug允许HTTP，Release仅允许真实HTTPS origin。

```bash
python3 scripts/mobile.py android debug --api-url http://10.0.2.2:8080
python3 scripts/mobile.py android run --api-url http://127.0.0.1:8080 --device "$ANDROID_SERIAL" --reverse-port 8080
```

run会构建Debug、在指定设备安装并启动MainActivity；install只安装已有APK，可通过 `--artifact` 指定。不会自动选择多台设备中的某一台。

Release从环境注入 `MND_ANDROID_KEYSTORE`、`MND_ANDROID_KEY_ALIAS`、`MND_ANDROID_STORE_PASSWORD`、`MND_ANDROID_KEY_PASSWORD`。继续使用已发布App的原密钥；新App可由负责人使用keytool交互生成，脚本不生成或替换签名身份。原生高德Key和微信／QQ公开AppID沿用既有环境入口，可继续留占位关闭。

```bash
python3 scripts/mobile.py android release --api-url "$MND_API_BASE_URL"
```

同时产生签名APK和AAB，复制到 `artifacts/android/release`；Debug位于 `artifacts/android/debug`。缺签名配置或HTTPS地址时停止，不回退到debug签名或静默导出unsigned Release。`MND_VERSION_NAME`／`MND_VERSION_CODE` 可设置构件版本。配置方式参考[Android官方签名文档](https://developer.android.com/studio/publish/app-signing)。

## iPhone／iPad

需要Apple Silicon macOS、完整Xcode（本项目兼容基线26.4）、JDK17、XcodeGen、CocoaPods。共享模块当前只有iosArm64与iosSimulatorArm64；Intel模拟器不是已配置目标。prepare先生成当前 `MndIOS.xcodeproj` 再安装Pods，构建使用 `MndIOS.xcworkspace`。已有Podfile.lock时使用deployment模式；首轮实际解析后才能生成真实锁文件，本轮未伪造锁文件。

```bash
python3 scripts/mobile.py ios prepare
python3 scripts/mobile.py ios simulator --api-url http://127.0.0.1:8080
python3 scripts/mobile.py ios run --device "$IOS_SIMULATOR_UDID"
```

Simulator构件在 `artifacts/ios/simulator`。真机运行必须使用可达的后端和有效签名 `.app`，通过 `--physical --device ... --artifact ...` 指定，不能把Simulator构件装到真机。源入口使用Apple的[设备命令行工具](https://developer.apple.com/documentation/xcode/interacting-with-devices-using-the-command-line)，本轮未连接设备。

Archive需实际 `MND_IOS_TEAM_ID` 及已安装证书／描述文件；可选 `MND_IOS_SIGNING_STYLE`、`MND_IOS_PROFILE_SPECIFIER`、`MND_IOS_CODE_SIGN_IDENTITY` 支持人工或CI签名。按实际已启用的Apple登录和Associated Domain生成entitlements，不使用占位域名要求开通能力。

```bash
python3 scripts/mobile.py ios archive --api-url "$MND_API_BASE_URL"
python3 scripts/mobile.py ios export --export-options /absolute/private/ExportOptions.plist
```

输出为 `artifacts/ios/MndIOS.xcarchive` 和 `artifacts/ios/export/*.ipa`。`ExportOptions.example.plist` 仅示例，需换真实teamID及对应分发方式；建议使用当前Xcode Organizer导出的选项文件。脚本拒绝 `destination=upload`，只导出，不自动上传TestFlight／App Store。具体文件用途见[Apple Archive导出说明](https://help.apple.com/xcode/mac/current/en.lproj/deva1f2ab5a2.html)。

## Desktop与手动CI

```bash
python3 scripts/mobile.py desktop package
python3 scripts/mobile.py desktop run --api-url "$MND_API_BASE_URL"
```

生成当前OS对应DMG／MSI／DEB，位于 `kmp/desktopApp/build/compose/binaries/main`。各系统单独构建；开发／内测构件不代表已经具备Windows发行签名或macOS Developer ID／公证。正式商店或公开渠道发布仍需实际渠道签名和独立放行，未配置任何自动发布。

App workflow仅 `workflow_dispatch`：Android Debug／Release、iOS Simulator／Archive、三系统Desktop任选。Android37平台安装、JDK17、版本输入及构件留档已接线。iOS使用arm64 `macos-26`、显式Xcode路径；运行器标签与架构依据[GitHub官方说明](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)，Xcode安装项须以当次镜像为准。

签名Secrets／Variables须由仓库管理员在GitHub配置，不能在聊天或仓库中填写：

| 范围 | 配置名 |
|---|---|
| Android签名Secrets | `MND_ANDROID_KEYSTORE_BASE64`、`MND_ANDROID_STORE_PASSWORD`、`MND_ANDROID_KEY_ALIAS`、`MND_ANDROID_KEY_PASSWORD` |
| iOS签名Secrets | `MND_IOS_CERTIFICATE_BASE64`、`MND_IOS_CERTIFICATE_PASSWORD`、`MND_IOS_PROFILE_BASE64`、`MND_IOS_EXPORT_OPTIONS_BASE64` |
| iOS团队Variable | `MND_IOS_TEAM_ID` |
| 可选平台配置 | 原生高德Key Secrets、各端AppID与Universal Link／Associated Domain Variables，名字与本机环境一致 |

iOS签名助手只允许在GitHub Actions中运行，创建临时Keychain、保留并恢复原搜索列表，使用Xcode当前描述文件目录并删除本轮导入的描述文件和临时证书；Android也有always清理步骤。签名材料不进入上传目录。构件保留14天；没有自动安装到用户设备或对外发布。

## 本批验证边界

[源码指纹与静态回执](delivery-evidence-2026-10-25.json)记录本批来源。13项只读静态脚本通过，包括新增 `check_delivery_static.py`；Python AST、YAML／plist／JSON、Go格式／语法读取及源码引用均只作静态核对。新增Secret和迁移回归源码没有执行。没有运行Docker、Gradle、Go build/run/test、npm typecheck/build、XcodeGen、Pod install、xcodebuild、CI dispatch、adb、模拟器或应用；没有产生已验证安装包、部署环境或成功运行回执。

后续实际执行必须使用真实配置并遵守当次运行授权。全量Figma视觉／设备验收另见 [全端页面进展](ALL_FIGMA_IMPLEMENTATION_2026_10_24.md)，本批交付接线不改变其未验收状态。
