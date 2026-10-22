# 手机 V2 · Friendly 全量统一

历史阶段记录：后续已补社交／私信／群聊至 134 状态，并将按钮更新为 48 高／24 圆角真胶囊；卡片和输入仍为 16。当前入口与验证见 [手机社交交接](SOCIAL_CHAT_HANDOFF.md)，不要用本页旧 PNG 覆盖新版。

2026-10-16｜仅非商业学习 Design｜59 屏样式统一已完成

用户在选择 01 清爽亲切并看过三张关键页后，明确要求“都改了吧”。本轮将其扩展到现有 59 张普通手机参考屏及当前手机评审板，不再仅保留三屏试用。品牌与内容方向不变，不代表真实功能、产品规则或无障碍运行验收通过。

范围：复用现有 Action 主组件和 Friendly 变量，保留页面及按钮实例 ID、Label 属性和已有导航；主／次／文字／禁用操作统一选定外观。手机专用 Mobile Light V2 的品牌文字、浅色强调底和主动作强调色对齐 Friendly；普通输入／选择／错误输入沿用中性细边，圆角对齐 16，保留错误语义。领养详情按三屏样稿使用“先聊一聊＋申请领养”。当前评审板同步，原 00–09、其它设备／深色模式、生产 Stitch 和客户端代码不修改。

已复用前阶段 Code Connect／库发现记录；本轮读取现有 Action、Field、Select、59 根手机画板及评审板，进行原位更新，不新建主题或重复按钮家族。旧 Light／Dark 模式色值保持不变。

验收：59 根画板与其结构保留；主组件／实例样式、字体与标签宽度检查；已有导航源与目的地不丢失；输入边界和错误色不混淆；指定颜色对检查；原生编辑层与离散照片保留；整板及代表性流程视觉检查。禁止把整屏截图当作设计节点；不声称表单、短信、地图、聊天、发布、举报等真实功能已接入。

未运行编译，遵循全局不自行编译规则。

## 当前入口与结果

[Figma 代表页](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=81-192) · [59 屏手机稿](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=56-27) · [当前代表预览](friendly-mobile-final-preview.png) · [当前整板](friendly-mobile-all-preview.png)。

59 个 390×844 根画板保留；1142 Frame、955 Text、546 Instance、451 Vector，共 3094 个原生节点，21 个离散照片。4 个共享 Action 变体及其所有实例同步，3 类输入／选择控件的边框圆角为 16，另统一 179 个直接面板／工具容器圆角。普通正文、错误语义、照片内容和根画板位置未改。

原有 329 条导航源及动作完整保留；领养详情新增“先聊一聊”，现为 330 条静态跳转，无失效目的地。按钮无错误字号／字重／高度／圆角，Noto Sans SC／Manrope 无缺失字体；12 组指定颜色对通过，主按钮白字为 4.60:1。旧 Light／Dark 模式值保持不变。焦点状态仍采用已定的 2px 中性灰环和 3px 间距，不常驻普通触摸按钮。

已检查 3018×8748 整板与代表页，并将评审副本的按钮归到与手机原稿同一 Action 来源，避免来源分叉。没有截图替代原生 UI，保留有意的滚动视口裁切。这里不是业务、设备、读屏或完整无障碍验收。

当前账本：`friendly-mobile-before.json`（更新前样式摘要）、`friendly-mobile-foundations.json`、`friendly-mobile-rollout-state.json`、`friendly-mobile-review-update.json`、`friendly-mobile-review-source-sync.json`、`friendly-mobile-validation.json`。原阶段 PNG／验证仍是历史快照，以本轮文件为当前依据；不要重跑旧 setup/components 覆盖本轮配色。
