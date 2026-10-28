# 平板方向切换状态与UI组件续作

2026-10-23，AI-20261023-PAD-STATE；main / 5de631b 加既有工作树。用户要求横竖屏切换尽量保留状态，随后追加统一基础框架／组件库以便维护和整体升级。本批只改 Android UI／状态 owner 及文档、只读静态脚本；没有更改服务器、API、依赖、苹果UI或远端设计。

## 横竖屏核对及修复

已有 `MobileViewModel`／单 MobileStore、按账号和任务的 SaveableStateProvider、窗口／IME／铰链策略，表单正文和附件不由方向决定。发现局部 remember 状态只随当前 Composition 存活，以及 FormPair 两个分支会生成不同输入树；本批补下列路径：

| 状态 | 当前实现 |
|---|---|
| 当前Tab、详情／会话／业务事项、表单正文、受众／宠物、附件、分页数据、待确认与未知请求 | 沿用同一个 ViewModel／Store；旋转不新建业务 owner。Activity 不锁方向，也不靠声明 configChanges 绕过配置变化 |
| 首页列表与主从列表 | 一个 movableContentOf 实例在全宽与主栏间移动，继续使用同一首页 SaveableStateProvider，避免分支各建一个列表。Lazy 列表／滚动容器沿用可恢复滚动状态，数据项使用资源ID key |
| 单／双列表单 | FormPair 用 movableContentOf 移动每组字段；rememberUpdatedState 提供最新内容，避免捕获旧表单。FormField 使用受控 TextFieldValue，正文仍在 Store，系统恢复只保存光标偏移 |
| 人员筛选、人员／宠物列表展开、表情分类、搜索未提交输入 | 新 AndroidUiSessionState 由 MobileViewModel 持有；按当前任务 scope 和键取值，方向不是状态 key |
| 日期与时间的未确认选择 | 日期弹窗开闭、当前日历日期、已选日期与未确认时分保留在内存。旧原生弹窗随旧Activity释放，在新UI重建；只有日期／时间均主动确认才写字段，取消或旋转不提交 |
| 登录、用途验证、安全页联系输入 | 当前流程内存保留；流程关闭／离开安全页／用途切换／切换账号时清对应 transient scope。密码／验证码不写系统恢复或普通偏好，进程死亡后重新输入 |
| 发布核对的来源／勾选、敏感核对勾选 | 绑定原 EntryInput 或当次 intent 的 generation；同一内容旋转保留，换内容／用途重新确认，不自动提交 |
| 跨账号 | UI根按 owner 隔离，内存容器清旧 owner；业务仍按服务器会话和现行资格核对 |

`AndroidUiSessionState` 只持有值和 MutableState，不保留 Activity／View／回调。MainActivity 传入 ViewModel 的同一实例，旋转不会用临时新实例替换；onCleared 清理。独立使用 AndroidMobileApp 时默认实例只适用于当前 Composition，正式入口已明确注入 ViewModel。

当前恢复范围分层：Activity 重建／窗口重排尽量保留本次状态；进程终止后仅用现有路由、私有草稿和原请求恢复，不恢复秘密、未提交用途确认或整个内存UI。键盘是否重新弹出、焦点回到哪个控件、输入法组合区及不同厂商原生选择器仍需设备验证，不能承诺像素位置或输入法会话完全一致。

实现参考 Android 官方 [Compose状态保存](https://developer.android.com/develop/ui/compose/state-saving)、[movableContentOf](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)及[TimePickerDialog回调](https://developer.android.com/reference/android/app/TimePickerDialog)。前两者分别说明配置重建状态保留和跨布局移动组件状态，时间回调用于保存临时选择；本批没有执行设备操作。

## 基础框架与组件

[组件入口／设计映射／维护规范](UI_COMPONENTS.md)定义现行职责及升级方式。新增 MobileUi 集中现有尺寸，MobileComponents 提供10个纯视觉／输入 primitive；已有 Feature*、FormField、ChoiceField、CheckLine、ScrollContent、SectionTitle、ActionCard 已接入兼容封装，30扩展页面继续使用既有业务接口。发布／消息编辑、弹窗、分页动作和部分卡片／列表尺寸也已接统一入口；数值沿用默认设计，没有换字体／颜色／主题。

## 验证及未验证项

- `check_kmp_client_static.py`：58份 Kotlin／Swift 的基础定界符、配置和请求字段检查通过；无 Kotlin 类型检查。
- `check_android_feature_static.py`：30个扩展分派、方法引用、单 Store、适配约束和24贴纸资源通过。
- `check_android_ui_static.py`：10个 primitive、依赖方向、尺寸映射、内存UI owner／scope／generation 和可移动表单／首页结构通过。
- `check_android_finish_static.py`：前批34网页控件、分享／已读接线静态仍通过；网页没有本批改动。
- 本批UTF-8／无BOM、冲突标记／增量空白、文档链接和SHA256见[证据](pad-state-ui-evidence-2026-10-23.json)，历史证据不改写。

这些是源码／结构核对；未执行UI测试、编译、设备或浏览器。未运行编译，遵循全局不自行编译规则，依据本机全局规则（历史Windows全局规则路径；接手读取当前环境规则及[仓库规则](../../AGENTS.md)）“长期禁止 AI 自行执行编译、构建、打包、安装、测试或设备启动”。本批未提交／推送。

## 后续设备验收矩阵

在获运行授权和具备真实设备后，至少验证：

1. 竖屏阅读长列表后转横屏／返回，保留对应记录附近；主从栏开关与导航返回同样验证。
2. 单列／双列表单中输入长中文、选中一段文字、切换方向，正文与选择尽量保留，不换成另一条记录。
3. 选宠物／受众／排除好友、筛选和翻页后旋转，检查选择、筛选、末页／错误和原游标。
4. 已选择日历日期、时间尚未确认，及正在改时分时旋转；确认只写一次，取消不改原值。
5. 本地照片选择／上传失败／重试过程中旋转，保留原附件及原请求，不重传成功附件。
6. 发布来源和用途确认页旋转，保留同一意图；修改内容／换用途后勾选重置。
7. 登录／注册／用途验证码输入时旋转，字段与倒计时保留，关闭流程／退出账号后清理秘密。
8. 消息未发送文字／回复对象／表情面板、已打开聊天历史分页，旋转不发送、不自动标已读或跳最新。
9. 请求结果不明时旋转，只核对原编号；已读弹窗／预约／领养不得多提交。
10. 分屏拖动、字体放大、键盘开闭、短高横屏和折叠铰链切换，核心动作仍可到达；进程终止与Activity重建分别验收。

未把这些待执行用例标为通过。真实焦点／IME、厂商对话框、滚动锚点与进程恢复仍是 Android 手机和平板实际验收余量。
