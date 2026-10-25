# 高德原生与 Web 地图源码接入

日期：2026-10-24（Asia/Shanghai）。执行者：Codex；起点 `main / 77c479a`，保留此前源码与成本评估。用户明确手机／平板使用原生地图，随后确认 Android 与 iOS 一起接入，其他端使用高德 Web 地图；继续不推送。

本批为**源码接入与静态核对**，不是已配置可运行的地图服务、供应商许可或平台验收。没有执行构建、测试、应用启动、SDK依赖解析、Pods安装、迁移、提交或推送。

## 平台与组件

| 平台 | 当前接线 | 业务边界 |
|---|---|---|
| Android 手机／平板 | [NativeLostMap／CaseMapPicker](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidNativeMap.kt)，原生 `MapView`＋`AndroidView` | 同一组件供手机／平板使用，保留既有 MVI／任务表单；替换模板 WebView |
| iPhone／iPad | [Compose 页面](../../kmp/iosUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/iosui/IosLostMap.kt)＋[UIKit 桥](../../kmp/iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui/IosNativeMap.kt)＋[Swift SDK 适配](../../kmp/iosApp/ComposeApp/AmapNativeMap.swift) | `UIKitView`嵌入原生 `MAMapView`；Swift只处理地图，不建立第二套Store，按Scene宽度与Auto Layout调整 |
| Web | [AmapView](../../web-client/src/maps/AmapView.tsx)＋[SDK加载／公开点工具](../../web-client/src/maps/amap.ts) | 高德 JS API 2.0，供互助地图、详情与发布表单复用；同意后才加载SDK |
| Desktop | [Web地图入口](../../kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/screens/LostMapScreen.kt) | 在系统浏览器打开同一 `/app/map`，不是内嵌WebView；不复制v1令牌，浏览器登录独立 |
| 共享层 | [LostMap.kt](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/LostMap.kt) | 公开点、舍入和城市视野默认值；不含SDK、Compose或私密地址 |

互助地图绘制已经载入、处于 `active` 且坐标来源明确的求助，可按走失／拾获筛选、选中地图点并查看原事项；分页继续沿用既有列表加载。刷新后关闭／失效记录不再进入活动地图。地图不承诺实时推送，不会把当前页数据冒称为全城全部点。

发布／编辑可主动选择最后见到的大致位置，也可只填城市和文字区域。新选点优先于主动保留的原点；改变城市或区域后，旧选点失效，清除选点后可以只发布文字区域。Android沿用已有发布表单，iOS为本次地图增加最小走失／拾获发布与原有求助编辑；共用服务端资格、版本和原请求核对。Web仍使用私有草稿与核对确认步骤。

当前不包含地点搜索／逆地理编码自动填区、聚合、地图视野查全城、GPS宠物追踪或内置导航；不将它们列为已经完成的功能。地图标记属于M&D页面自己的覆盖物，不发布为高德App公共POI。

## 坐标与权限合同

- `Case`／`CaseBody`与附近订阅补可选 `coordinate_system`：`gcj02`／`wgs84`，空值为旧记录来源未知。Go、KMP、OpenAPI与Web派生合同同步；JSON记录模型无需新增SQL迁移。
- SDK新选点为GCJ-02，写入表单／本地稿前舍入两位小数；服务端仍按既有两位小数规则保存，未扩大公开精度。约1km网格，显示的1,000m范围圆只是粗范围示意，不是定位误差保证。
- 来源明确的WGS-84点由对应高德SDK转换用于显示；GCJ-02不二次转换。未知来源与 `area` 文字区域不画点，不以0/0或区县中心伪装发生点。武汉／上海默认视野仅用于打开地图，不自动保存为求助地点。
- 半径查询与附近提醒只在坐标系一致时计算距离；城市提醒不受影响。已知坐标与来源未知旧坐标不会混算。没有静默修改旧记录或把未知数据批量标为GCJ-02。
- 本批地图使用手动选点，不启动定位客户端或后台定位。Android显式移除SDK合包可能合并的定位／电话权限；实际合并Manifest仍待后续解析核对。iOS关闭用户位置显示，未新增定位用途授权。
- 地图加载有独立说明、政策链接与用户主动同意；未同意／缺Key／载入失败时保留文字区域、列表和表单。Web同意只存页面进程内，停止加载会通知同页地图组件；不把认证或私密稿写入通用Web storage。

地图适配层只取得公开点、标题和记录标识，私密地址、线索、认领材料与令牌不进入地图URL或SDK数据。SDK调用前的隐私接口依据[Android官方初始化说明](https://lbs.amap.com/api/maps-sdk-for-android/guide/create-project/dev-attention)及[iOS官方说明](https://developer.amap.com/api/ios-sdk/guide/create-project/note)。

## 配置与依赖

| 配置 | 注入位置 | 说明 |
|---|---|---|
| Android Key | Gradle属性 `meow.amap.android.key` 或环境 `MND_AMAP_ANDROID_KEY` | 写入Manifest的 `com.amap.api.v2.apikey`；未提供时地图入口明确不可用 |
| iOS Key | Xcode Build Setting `MND_AMAP_IOS_KEY` | 经Info配置供Swift适配读取；使用目标App对应平台Key |
| Web公开JS Key | 构建环境 `VITE_MND_AMAP_JS_KEY` | `.env.example`／Docker公开build arg；配置高德域名白名单 |
| Web代理Key | 服务端 `MND_AMAP_JS_KEY` | 与前端JS Key相同，避免代理代用其他Key |
| Web安全密钥 | 服务端 `MND_AMAP_JS_SECURITY_CODE` | 只在代理上游请求中附加，不进入Vite产物、浏览器或原生包 |
| Web origin | 服务端 `MND_WEB_ORIGIN` | 必须与实际页面scheme／host／port精确一致，生产HTTPS；开发可用现行loopback配置 |
| Desktop Web入口 | 可选进程环境 `MND_WEB_MAP_URL` | 默认根据已有API根地址打开 `/app/map`；不附带认证信息 |

只新增配置入口与空模板，未读取、创建、申请或写入实际Key，未购买或声称取得商用许可。[成本评估](AMAP_LOST_PET_ASSESSMENT_2026_10_24.md)中的授权条件仍适用，需核实同一运营主体的各平台覆盖范围。

Android固定 `com.amap.api:3dmap-location-search:11.3.100_loc11.3.101_sea9.8.1`；这是当前官方Maven分发的地图／定位／搜索合包，仅实例化地图。版本存在性读取自[Maven元数据](https://repo.maven.apache.org/maven2/com/amap/api/3dmap-location-search/maven-metadata.xml)，[地图发行说明](https://developer.amap.com/api/android-sdk/changelog/3d)记录11.3.100对Android17的适配。本轮未下载、解析或检查二进制ABI／16KB页大小。

iOS新增[Podfile](../../kmp/iosApp/Podfile)，固定 `AMap3DMap 11.2.100`、`AMapFoundation 1.9.1`，以本轮实际回读到的[CocoaPods地图分发元数据](https://trunk.cocoapods.org/api/v1/pods/AMap3DMap)和[基础包元数据](https://trunk.cocoapods.org/api/v1/pods/AMapFoundation)为依据。官网已有较新地图版本的说明不等于该版本已在所选分发中可解析；未写虚构的Podfile.lock。未执行工程生成、Pods安装或现有framework预构建脚本；后续必须遵守仓库的构建限制再处理workspace与真实签名。

Web不新增npm依赖，用异步script加载JS API。`/maps/amap/_AMapService/`由[固定上游代理](../../internal/platform/mobileapi/amap.go)处理：只允许GET与本批实际使用的地图样式／坐标转换／逆地理编码路径，核对origin与公开Key，有并发／请求上限和超时，不转发Cookie／Authorization、不自动跟随上游重定向。不能当任意URL代理。配置方式遵循[官方安全密钥代理指引](https://lbs.amap.com/api/javascript-api-v2/guide/abc/jscode)。

## 验证结果与未验收项

- 5项只读脚本通过：KMP源码／配置（73份Kotlin／Swift）、共享合同（66对DTO）、Android扩展页（39分派）、Android UI公共组件、Web引用／资源（37份TS／TSX、171个引用、114份派生schema）。这些是静态核对，不是业务测试运行。
- 本批22份Kotlin／KTS／TS／TSX完整语法树通过；`MobileFeatureStore.kt`的既有解析器报错与HEAD完全相同，本次改动的Map分支另行解析通过。Swift目前只完成源码／定界符和桥接对应项阅读，没有类型或链接验证。
- 最后对57个当前改动路径做UTF-8／BOM／空白／冲突扫描，35个新增本地文档引用有效，`git diff --check`通过；其中包含先前未提交的成本评估和本轮交接。
- 新增[附近坐标系回归源码](../../internal/mobile/map_coordinates_test.go)与[代理拒绝路径回归源码](../../internal/platform/mobileapi/amap_test.go)，未执行测试。Go只做格式／语法读取；未调用Go构建或测试。
- 未编译、构建、test、lint、类型检查、迁移、启动服务／应用／浏览器／模拟器或设备；没有通过CI绕过限制。未提交或推送。

Figma在线session／design context均因MCP传输失败，未修改远端；本次沿用既有页面组件与仓库保存的[地图设计回读](../design/figma-study/audits/2026-10-16-mobile-readiness/07-lost-map.png)进行SDK接线，没有宣称重新完成像素对齐或运行截图验收。

剩余实际验收为三平台Key／供应商授权、依赖解析与ABI、原生SDK／Kotlin-Swift头文件真实兼容、手机／平板旋转分屏、地图故障与弱网、原请求恢复和真实数据联调。用户未授权运行，本批不启动这些步骤。当前源码未推送，换机需一并携带工作树文件。
