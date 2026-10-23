# Android 手机／平板读取状态补全 · 2026-10-23

任务：AI-20261023-ANDROID-STATES；工作树main／5de631b，保留此前服务器、共享与安卓的全部未提交成果。用户授权继续设计修订并完善代码，本批沿用安卓优先、苹果UI后置。没有构建、测试、服务、设备、提交或推送授权。

## 具体行为

| 状态 | 共享语义及安卓展示 |
|---|---|
| 首次读取 | loading与loaded分开；显示进度，不提前显示空数据 |
| 刷新 | 保留旧列表与滚动容器；首屏结果成功后替换，失败保留内容并重试首屏 |
| 下一页 | 原内容保留，标明正在加载下一页；成功按ID去重合并 |
| 翻页失败 | 保留原next游标与appending标记，仅提供“重试下一页”，不退回首屏 |
| 空列表 | 仅loaded、无错误、没有后续游标时显示对应业务空态；过滤后暂时无项但有游标时可继续加载 |
| 到底 | 仅成功、非空、next为0时显示“没有更多了”；加载或失败不冒充到底 |
| 权限失效 | 401／403／404清除对应受保护内容和游标，展示真实错误；不保留失去权限的旧内容 |
| 照片 | 加载中、解码／网络失败、没有照片分开；失败可单图重试，账号或资源切换立即移除旧图 |
| 比例 | 照片Crop保持原比例，未传布局的照片有稳定5:4显示区；已发送贴纸144dp并显式Fit，选择面板64dp保留 |

核心10类列表：社区、待领养、我的宠物、申请、走失／拾获、私信、群聊、消息、评论、通知。
扩展8类列表：好友、邀请、预约、有偿信息、核实事项、照护近况、认领材料、私密线索。另覆盖宠物与资料范围两类选择列表，共20类读取列表；不把静态路由计数当完整业务验收。

私信与群聊分开载入，不再因一个分页按钮推动两类列表。认领／线索也各自保留状态和游标，可分别刷新／翻页／重试；一类普通连接失败后另一类仍会读取。附加社区统计失败不把已成功的列表改成分页错误。现有账号epoch、路由revision与取消检查保留。

## 布局与实现入口

- [共享PageState与主列表](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt)：保留刷新内容、请求模式、独立会话分页；服务资格关卡未更改。
- [扩展读取与选择列表](../../kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileFeatureStore.kt)：共用状态转换，认领／线索支持指定材料读取。
- [安卓通用状态](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidLoadStates.kt)：按容器宽度自然换行，错误面板最大480dp、动作至少48dp，进度／错误使用礼貌播报；沿用MaterialTheme默认浅色。
- [照片](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidMedia.kt)、[贴纸](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidComposeControls.kt)及现有各业务页接入。手机、平板横竖／窄窗使用同一组件及原Store，不另建业务状态。

本批没有修改依赖、包名、签名、旧Stitch镜像、苹果UI或服务器。Web／Desktop新业务UI尚未迁移，这些共享语义不代表所有平台的状态界面已实现。

## 静态证据及未验证项

[证据与文件哈希](android-read-states-evidence-2026-10-23.json)记录本批静态检查。已审阅并执行两份只读脚本：check_android_feature_static.py和check_kmp_client_static.py。检查源码引用、30个安卓扩展分派、单Store、自适应边界、资源、DTO字段、50份Kotlin／Swift基础定界符与编码；这不是Kotlin类型检查或编译。

新增[PageStateTest.kt](../../kmp/shared/src/commonTest/kotlin/com/ttkk0000/meowcircle/client/PageStateTest.kt)七个回归用例源码：刷新保留／替换、翻页失败游标／去重、刷新失败先刷新、权限撤销清理、过滤空页仍可续接、真正空／到底条件、游标不前进拒绝。测试未执行。

获得运行授权后再验收：首次慢网、刷新失败、第二页中断后重试、空首屏／过滤空页／最后一页、会话和两类私密材料独立分页、读取中撤权／换账号、照片慢网／损坏／单图重试，以及手机和平板横竖、窄窗、大字、键盘与屏幕阅读器。未获得这些证据前不能标全功能或上线验收完成。

Figma导航修订与两项待补远端证据见[设计收尾](../design/figma-study/NAVIGATION_DOC_2026_10_23.md)。

未运行编译，遵循全局不自行编译规则。未测试、设备运行、迁移、提交或推送。
