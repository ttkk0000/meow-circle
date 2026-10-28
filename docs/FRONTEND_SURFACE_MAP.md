# M&D 前端与多端页面地图

更新：2026-10-27。以[现行基线](CURRENT_BASELINE.md)、[产品交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md)和[文档目录](DOCUMENTATION_CATALOG.md)为准；本文仅记录源码与执行条件，不表示运行通过。

## 当前客户端

| 表面 | 入口与公共依赖 | 已有源码范围 | 尚未验收 |
|---|---|---|---|
| Android手机／平板 | androidApp/mobile＋shared＋clientUi | 共享Compose页面、窗口适配及Android系统桥接 | 设备、旋转／分屏、键盘、权限及真实SDK |
| iPhone／iPad | iosUi＋iosApp/ComposeApp | Swift宿主＋Compose页面、地图／认证／安全存储桥接 | iOS设备、无障碍与真实回调 |
| Desktop | desktopApp＋shared＋clientUi | 原生窗口、系统文件／钥匙串、浏览器能力接续 | 桌面环境、窗口与键盘恢复 |
| Web消费端 | web-client/src/App.tsx，/app | 独立React DOM、Cookie／CSRF、任务和恢复组件 | 浏览器、响应式、网络／角色／视觉 |
| 业务后台 | web-client/src/operator，/app/operator | 13页、分页队列／详情、角色与版本／未知结果 | 真实业务角色和当前环境 |
| 技术运维 | web-client/src/ops，/app/ops | 14类页面、遥测、事件、交付准备与审计 | 发布／重启执行器仍占位，真实交付未验收 |
| 游客安全与公开阅读 | 服务端公开页面、分享与阅读入口 | 免登录报告、回执与安全阅读合同 | 真实通道受理、隐私和失效链接 |

公共clientUi有41份公共Kotlin文件、48个FeaturePage；shared/client目录28份文件。文件数不等于Figma740状态完成数，也不代表五种布局均已运行通过。各批恢复点见[客户端入口](client/README.md)。

## 导航与布局

主导航按已批准设计为社区／领养／发布／消息／我的；宽屏保留互助入口，窄窗从任务上下文进入。Web App导航源包含互助，不以文档统一导航数量删除现有合法入口。

当前Figma V2默认浅色，旧主题按未迁移范围保留。窗口可用宽高决定单栏、主从与模块栏；旋转、分屏和关闭输入面板保留Store、对象、草稿、受众、列表及滚动上下文。具体尺寸查[跨端合同](design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)，不在本表另造断点。

## 状态与系统能力

| 组件／桥接 | 目的 | 核对重点 |
|---|---|---|
| ConsumerApp与FeaturePages | 主壳和业务分派 | 根入口与子分派同时覆盖，不只检查类存在 |
| ClientUiPlatform | 相册、地图、返回、日期、通知、链接和图片解码 | 各宿主真实端口实现，受限时说明原因 |
| WorkspaceShell | 双后台公共框架 | 各后台会话与合同隔离，不继承消费者角色 |
| 草稿／原请求恢复 | 明确失败、未知结果与冲突恢复 | 不换key自动重发，失权不恢复私密材料 |
| CityField与猫咪配图 | 共享城市选择与真实空态说明 | 目录不授予业务准入，配图不冒充业务宠物 |

## 旧表面与视觉来源

web/包含API v1兼容页面、Stitch设计板和cute静态原型；mobile/为旧Expo；KMP legacy Stitch代码保留兼容。cute不调用Go API，演示回退不能写成真实业务数据。原Figma设计继续为新客户端权威；本轮[Figma文档中心](figma-sync/README.md)用于阅读规范、表格和图谱，不取代原稿。

## 验收记录

后续取得运行授权后，按具体状态、角色、对象版本与窗口逐项核对：加载／空／错误／失权／冲突／未知、键盘与返回、TalkBack／VoiceOver、字体放大、媒体与地图、方向与分屏、原请求恢复。本轮只有文档与静态核对，没有执行应用、构建、test或lint。
