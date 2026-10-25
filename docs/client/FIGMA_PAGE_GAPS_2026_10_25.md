# Figma页面缺口核对与源码完善（2026-10-25）

任务：`AI-20261025-FIGMA-PAGE-GAPS`。用户要求“你核对下差的页面都完善下”。本批核对本地740条状态索引、19个业务族及现行公共页面，直接回读13份Figma高保真上下文和截图，补齐本轮确认的页面与交互缺口。**这是源码／静态交付，没有编译、类型检查、测试执行或逐端运行验收，不等于740个状态逐像素验收完成。**

当前架构继续为 `kmp/shared` 共享业务、`kmp/clientUi` 共享Compose页面、Swift薄宿主和各端系统桥；Web复用同一服务器契约。公共UI35份源码、45个扩展页面分派，继续使用原10个基础组件，没有新增第二套业务Store。真实AppID／Key仍占位，不继续平台注册，不提交或推送。

## 已补的页面与交互

| 页面／状态 | 本批具体实现 | 源码入口 |
|---|---|---|
| 连接暂时中断 | 传输失败与401／403／404／410／429／服务不可用分开；列表保留已有记录、原游标和上次成功读取时间，读取失败不伪装为空结果；写操作的结果未知仍保留原状态机 | `ClientReadState.kt`、`MobileApi.kt`、`MobileStore.kt`、Web `core/readState.ts`／`query.ts` |
| 内容状态已变化／账号失权 | 原爪印状态卡、说明、重新读取、已有沟通、复核与返回入口；失权收起受保护数据和原读取时间，不把旧预约／地址／费用继续当有效信息 | `ReadStatePages.kt`、`FeaturePages.kt`、`LoadStates.kt`、`components/ui.tsx` |
| 首页空白／没有匹配结果 | 区分首次真实空白、筛选无结果、读取失败和仍有下一页；养宠指南、发日常、清空条件都有操作，清空不会暗中放宽其它私密资源范围 | `ConsumerApp.kt`、`features/Records.tsx` |
| 不提供定位也可以 | 原稿的 `permission` 是定位替代页，并非账号失权页；补独立 `LocationHelp`，手动选武汉／上海／其它城市、查看求助并填写文字线索，不读取位置，不伪造行政区坐标 | `ReadStatePages.kt`／`FeaturePage.LocationHelp`、Web `/location-help` |
| 开启系统通知 | 独立 `NotificationHelp`；Android打开本App通知设置，iOS打开本App设置，返回由现有生命周期重新读取权限；Desktop／Web说明站内消息与系统权限的区别，不伪造已开通推送 | 各端 `ClientUiPlatform`、Web `/notification-help` |
| 照片选择说明 | 独立 `PhotoHelp`；系统选择器只提供所选照片，取消后保留文字与草稿；Web表单内可直接打开同一说明组件，避免离开表单 | `ReadStatePages.kt`、Web `/photo-help`／`Attachments.tsx` |
| 选择照片／预览 | 未上传照片也能从当前账号的内存或本机草稿预览；解码留在UI内存，不将字节写入系统恢复状态；已上传照片仍走原API权限读取 | `Photos.kt`、两类Store的 `localPhotoPreview`、`PhotoQueueList` |
| 正在添加照片 | Ktor `onUpload`及浏览器XHR真实字节进度；未知总量显示等待，不编造百分比；100%传输后仍等待服务器确认，拿到媒体ID才算完成 | `MobileApi.kt`／`Models.kt`、Web `core/upload.ts`及原API适配 |
| 调整照片顺序 | 原生长按拖动、Web指针／HTML拖动和前移／后移按钮共用有序队列；第一张明确标封面；可主动保存顺序到本机草稿，原上传Key不改变 | `PageComponents.kt`、`Attachments.tsx`、`core/photos.ts` |
| 部分照片失败 | 每张错误、完成／未完成数、原编号重试、批量重试／移除失败项；保留成功照片；发布、业务表单与聊天复用队列，不重新生成失败上传编号 | 两类Store、`PhotoQueueList`、`WorkflowEditor.tsx`、`chat.tsx` |
| 系统通知点入 | 只接受十进制通知编号；显式查看后读取既有 `GET /notifications/{id}`，校验当前账号、通知ID、代次和交互锁，再按服务器目标白名单定位；保留当前输入，不标已读、不自动确认。无有效编号回到待查看消息中心 | `MobileStore.openPendingNotification`、Android `MainActivity`／FCM、iOS APNs／`IosClient`；Web `/notifications/{id}` |
| 阅读位置与多窗口 | 表单滚动整数按账号／任务保存在UI会话层，读取错误页使用独立scope；不把网络和业务副作用塞进10个基础组件；iPad各Scene只显示待查看提示，不强改另一个Scene的当前任务 | `ClientUiSession.kt`、`Components.kt`、`ConsumerApp.kt`、`MainViewController.kt` |

通知读取复用已有只读合同，消费者API仍为245路径／275操作／124 schemas，75对跨语言DTO；没有新数据库迁移。通知目标的具体资源仍由原读取接口再次校验可见性。当前输入或确认弹层不会被推送到达自动替换；只有主动“查看原事项”才切换任务，查看失败可以重试或暂不查看。

推送网关必须在FCM `data`／APNs `userInfo`中提供字符串 `notification_id`，保留超过2^53的编号精度。不要提供凭据、私密正文、URL或确认指令；无字符串编号时客户端只提供消息中心入口。FCM宜使用data消息，展示沿用客户端通用隐私提醒。实际供应商投递、冷启动、跨账号和多Scene仍待设备验证。

## 设计与公共资源

[本批原始上下文](../design/figma-study/page-gaps-2026-10-25/)保留13份工具响应及截图已查看标记：`56:81–84`、`56:28`、`311:380192`、`311:380195`、`240:368353`、`238:371770`、`106:1424`、`240:368361–363`。手机状态正文及Web／平板横屏代表稿已回读；没有修改远端Figma或把整屏截图当运行时素材。

新状态卡复用16圆角、24内边距、12间距、20／30标题和14／22说明；48图标留白容器内使用24×24原SVG。核对发现旧 `paw-print.svg` 与本批状态稿原件不同，因此新增独立 `state-paw-print.svg`，原文件不改、不重画。Web／Android旧资源镜像／公共Compose三份字节一致，已登记到Figma素材manifest，共22枚原SVG。示意猫图在实际选图中继续使用用户自己的照片，不放入虚假已选图片。

13份设计截图是设计侧证据，没有运行客户端截图；资源字节、调用路径和声明尺寸的静态一致不能替代实际渲染几何验收。

## 全量账本核对范围

740条仍按19个业务族追踪：认证105、领养94、图文媒体20、核实举报29、预约29、好友宠友圈47、消息20、社区24、有偿到访11、走失拾获73、全局状态4、隐私数据58、宠物日记44、表情91、群组35、规则9、附近提醒28、设置通知16、运营参考3。它们包含同一任务的角色、错误、成功与确认状态，不是740个独立页面文件。

本批核对全部源码路径及45个扩展分派，将具体缺口改映射到实际状态组件／帮助页／媒体队列；更新了11个状态的源码状态与本批设计依据。其余既有业务流程沿用原资格、版本、原请求恢复和权限合同，没有将“映射到文件”升级为逐状态完成证据。后来新增的14张技术运维设计仍由独立运维账本管理，不混入这740条。

## 验证与保留项

验证细项与文件指纹见[本批证据](figma-page-gaps-evidence-2026-10-25.json)：16项只读脚本；变动TS／TSX格式与语法读取；Kotlin／Swift／TS语法树、既有大文件中20个相关方法的独立语法读取；JSON／SVG、链接、冲突、尾空白与 `git diff --check`。初次基础组件边界检查指出滚动Effect不应放在primitive中，现已移入UI会话层并通过检查；没有放宽原检查。5个KMP回归函数只写源码，覆盖缓存／游标／时间、失权清除、错误分类、64位通知编号和传输完成不等于服务器接受，未执行。

遵循当前用户和仓库规则，没有编译、Gradle、Go build/run/test、Xcode／Web构建、类型检查、test、lint、App／服务启动、CI、打包、迁移、部署、提交或推送。仍需正式SDK／Key／域名／签名和通知供应商配置，并在获准环境验收布局、触控／键盘、SDK ABI、设备权限、冷启动和真实通知。

用户此前明确保留的运维发布／重启执行器、完整搜索接收端继续占位；本批没有把这些改成假执行成功。未因补页面迁移SwiftUI、增加新主题或新建小程序。

传输进度接口依据[Ktor请求文档](https://ktor.io/docs/client-requests.html#upload-a-file)和[XHR上传文档](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/upload)，Android设置入口依据[Settings API](https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_SETTINGS)核对；未通过查阅文档执行SDK或运行应用。
