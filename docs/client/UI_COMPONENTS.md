# Android UI 基础框架与组件维护规范

2026-10-23。适用新 Android 手机／平板入口；沿用 [Figma V2 默认浅色及适配标准](DESIGN_AND_ACCEPTANCE.md)。这是应用内组件库，已经接入现有页面；暂不另拆 Gradle 模块或发布依赖。iOS 使用自己的 Compose UI，Web／Desktop 的旧样式不在本次迁移范围。

## 结构与职责

| 层 | 当前入口 | 职责 |
|---|---|---|
| 主题 | `MobileTheme.kt` | 唯一现行颜色、Typography、字体与 Shapes；页面读 MaterialTheme，不按模块硬编码主题 |
| 尺寸 | `MobileUi.kt` | 页面／区块／表单／弹窗间距、正文限宽、触控高度、圆角、表情显示尺寸 |
| 基础组件 | `MobileComponents.kt` | 不依赖 Store／API 的展示和输入；值、禁用／加载状态、回调由调用方传入 |
| 窗口与页面框架 | `AndroidAdaptiveWindow.kt`、`androidWindowPolicy()`、`MobileShell` | 系统安全区、铰链、IME、单／双栏、导航、主从布局与按账号／任务隔离的状态容器 |
| 通用状态与素材 | `AndroidLoadStates.kt`、`AndroidMedia.kt`、`AndroidComposeControls.kt` | 首次加载／刷新／分页／空／失败／末页、私有图片读取与单图重试、表情比例和输入 |
| 领域适配组件 | `AndroidFeatureContent.kt`、`AndroidDateFields.kt`、`AndroidPeopleFields.kt`、`AndroidPetFields.kt`、`AndroidRegionFields.kt` | 绑定领域字段和权限；使用基础组件，不把准入或网络请求塞进视觉组件 |
| 状态 owner | `MobileViewModel.kt`、`AndroidUiSessionState.kt`、shared 的 Store | 业务状态／草稿由 Store 负责；旋转内 UI 状态由 ViewModel 保留；组件不另建 Store |

上述 Android 源码均在 `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/`，Activity 为相邻 `MainActivity.kt`。目录内 [README](../../kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/README.md)提供回链。

## 基础组件接口

| 组件 | 输入／输出 | 已接入位置及状态约定 |
|---|---|---|
| `MndForm` | modifier、gap、ColumnScope 内容 | 五主入口的滚动内容及30扩展页面的 FeatureColumn；滚动状态不由窗口方向作为 key |
| `MndSection` | 标题、modifier、内容 | FeatureSection；统一表面／内边距／标题语义 |
| `MndPrimaryButton` | label、enabled、loading、modifier、onClick | FeaturePrimary；loading 时阻止重复点击，动作必须来自回调 |
| `MndSecondaryButton` | 同上 | FeatureButton 与分页重试／继续加载；标签明确区分重试当前页和下一页 |
| `MndTextField` | String 值／回调、label、enabled、行数、键盘、isError、supportingText | 普通受控文本；调用方提供业务错误，不解析金额／资格 |
| `MndRichTextField` | TextFieldValue／回调、label、enabled、行数、键盘 | Feature FormField、发布正文、评论／消息输入；光标由适配层维护，不保存私密正文到系统恢复状态 |
| `MndChoiceGroup` | label、value、options、enabled、onValueChange | ChoiceField；窄窗按 FlowRow 换行，单选语义明确 |
| `MndCheckLine` | label、checked、enabled、onChecked | CheckLine；整行触控与勾选语义一致，禁用不响应 |
| `MndScreenTitle` | 文案 | 主页面 SectionTitle；统一字号、字重与 heading 语义 |
| `MndActionCard` | 标题、说明、onClick | 个人页等 ActionCard；统一表面／间距 |

现有 `FeatureColumn/FeatureSection/FeaturePrimary/FeatureButton/FormField/ChoiceField/CheckLine` 保留为兼容适配入口，并已经调用上表组件；不要求30个页面逐一改名。业务权限、确认和未知结果判断仍在原 Store／适配层，不能因为组件可点击就跳过服务端资格。

其他复用入口已经存在：`MobileDialog/MobileAlertDialog`、`LoadingStatus/PageRefresh/PageFooter`、`MobilePhoto/StickerPhoto`、`FormPair/DateTimeField`、`PeopleChoiceField/PetChoiceField/RegionFields`。这些与基础组件共同构成当前组件库；有些领域组件依赖 Store，因此不当作纯视觉 primitive。

## 现行设计数值

| 语义 | 当前值／来源 | 修改入口 |
|---|---|---|
| 主要色与背景 | 沿用 `#C84B3D`、`#F7F8FA`、`#FFFEFC` 等默认 Token | `MobileTheme.kt`，具体映射查设计标准，不新建页面主题 |
| 正文／品牌字体 | Noto Sans SC／Manrope | `MobileTheme.kt`，字体资源和许可沿用原配置 |
| 页面边距／区块间距 | 16dp | `MobileUi.pagePadding/sectionGap` |
| 表单／内容／紧凑间距 | 14／12／8dp，保留已有布局 | `MobileUi.formGap/contentGap/compactGap` |
| 最小主次动作高度 | 48dp，文字可以换行 | `MobileUi.actionHeight` |
| 卡片／弹窗圆角 | 16／24dp | `MobileUi.cardRadius/dialogRadius` → MaterialTheme.shapes |
| 正文／普通弹窗／登录限宽 | 820／560／520dp | `MobileUi.contentWidth/dialogWidth/authWidth` |
| 双列表单阈值 | 可读宽度620dp，按字体比例折算 | `MobileUi.formPairWidth` + FormPair |
| 发送表情显示区 | 144×144dp，内部按原比例 Fit | `MobileUi.stickerSize` + StickerPhoto；外层卡片扩宽不拉伸素材 |

窗口策略仍集中在 `androidWindowPolicy()`：可用宽度、可读宽度、高度、字号和 IME 一起判断。系统栏／铰链由 AndroidAdaptiveWindow 扣除；不使用设备型号或锁屏方向代替适配。

## 状态与维护规则

1. 新页面优先组合现有框架及领域组件；已有模式缺一项配置时扩组件参数，不复制一套按钮／表单皮肤。列表继续使用 PageState 和 PageFooter，不用多个布尔值拼出相互冲突的加载／空态。
2. primitive 只渲染传入值并回调。API、导航、埋点／写操作、草稿落盘放在 owner 或领域适配层；重组、旋转、展开侧栏都不能触发提交。
3. 同一任务只保留一个 owner。`SaveableStateProvider` 按账号＋资源区分；HomeContent 和 FormPair 用 movableContentOf 移动既有内容，捕获的最新内容通过状态参数或 rememberUpdatedState 更新，不以宽高重建状态。
4. 非持久 UI 输入使用 `rememberSessionUiState`；`generation` 绑定当次确认内容。密码／验证码／安全联系方式等使用 transient scope，流程关闭或切换账号时清除。此内存容器不存 Activity、View、回调，也不写 SavedStateHandle／Bundle；进程死亡后不恢复这些秘密。
5. 业务表单／附件／原请求恢复继续使用既有私有存储。系统恢复只放路由、游标、光标偏移等小状态；不要对私密 TextFieldValue 直接使用通用 Saver。
6. 全局视觉升级先改主题／尺寸，再改 primitive；领域组件只接新参数。保持现有函数入口和已确认业务回调，逐批迁移后再删除兼容封装。新主题、字体或导航变更先更新批准的设计标准。

组件使用示例（调用方的 state/actions 提供值和动作）：

```kotlin
MndSection("本次安排") {
    MndTextField(place, onPlaceChanged, "见面地点", enabled = !submitting)
    MndCheckLine("我已核对本次内容", checked, onChecked, enabled = !submitting)
    MndPrimaryButton("提交本次提案", enabled = checked, loading = submitting, onClick = onReview)
}
```

## 升级检查与当前边界

组件级检查覆盖默认／选中／禁用／提交中／字段错误、空／首加载／刷新保留／下一页重试／末页；布局检查覆盖窄窗、平板横竖屏、分屏、大字、长文案、键盘和铰链。日期确认前只存临时选择，取消不写半份日期。变更不得复活旧请求或自动勾选新用途。

本批静态入口：`check_android_ui_static.py`、`check_kmp_client_static.py`、`check_android_feature_static.py`；[本批交接](PAD_STATE_AND_UI_2026_10_23.md)另列状态恢复矩阵与证据。尚未把所有页面的每一个特殊 Material 控件替换成 Mnd 包装；认证等专用输入、业务状态卡和特殊照片布局仍有直接使用，但统一读取现行 MaterialTheme。后续按重复模式继续收敛，不预先造大量没人使用的组件。

未运行编译，遵循全局不自行编译规则。组件源码和静态通过不等于设备视觉／焦点／旋转验收通过。
