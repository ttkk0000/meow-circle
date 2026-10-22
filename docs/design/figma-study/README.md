# M&D Figma 学习稿交接

更新：2026-10-20｜仅非商业学习｜不属于已实施客户端

2026-10-21 远端展示已重组：[当前项目总览](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=395-3)与[本轮只读核对/新目录](REMOTE_FIGMA_CHECK_2026_10_21.md)。五个总览页连接九组原稿；原稿按同状态并排五种布局，旧页面位置记录已过时。抽查的旧画板 ID 仍在，但不能用旧 pageId 切页或重跑历史生成脚本。下文为有日期的原设计交接，不代表已重新全量验证或全部客户端实现。

最新补充：[全局未读评审](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=352-3)与[2026-10-20交接](GLOBAL_UNREAD_HANDOFF_2026_10_20.md)：8张手机状态、1张99+边界板、4端入口对照、角标与消息行组件。该新增合同尚未逐一映射全部跨端页面，也未实现客户端／服务端；下文旧覆盖数不包含它。

## 当前：全部跨端样式与可视状态已补齐

承接“补齐所有”，569 个产品／交互状态各有 Web、Desktop、平板竖屏与横屏对应稿，共 2,276 张，另补 32 张窄窗／分屏样例。10 个评审入口映射到统一索引。全部目标的布局、原文、字体与基础导航已回读，16 个模块截图抽查；复杂表情内部原型动作的全量重绑遇 MCP 超时，完整行为继续参考手机源原型，未宣称跨端全量交互／运行验收。

[跨端统一评审入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27) · [完整交接与验证边界](CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md) · [逐项映射](CROSS_PLATFORM_FULL_MAPPING.md) · [自适应契约](CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)。只用手机默认浅色，多主题留作参考；客户端和生产 Stitch 尚未迁移。[原缺项核对](CROSS_PLATFORM_COVERAGE_AUDIT_2026_10_20.md)及 56 张代表稿是补齐前基线。

[打开设计总览](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=14-74) · [核心体验](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=14-76) · [演示起点](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=30-3)

## 当前：手机端设计已补齐，等待评审

先看[本轮评审交接](MOBILE_FIGMA_REVIEW_2026_10_19.md)、[手机端 Figma 入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=238-371769)和[六屏总览](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=251-375655)。手机号／微信／QQ／iPhone Apple、账号管理、好友确认、拾获认领、预约交接、角色权限、通知上传、连续表情与窄屏大字已补原生设计。

本轮追踪213个新补／重绘状态，合并历史579个唯一参考根ID；含变体与覆盖层，不等于独立流程或运行验收。当前入口为mobile-completion-state.json、mobile-completion-content.json和mobile-completion-validation.json。先前381／134为阶段记录。

[底部表情交接](WECHAT_STYLE_EXPRESSION_INTERACTION.md)仍适用：128个表情、24张猫咪贴纸、16场景、23处紧凑输入栏；本轮另补连续编辑与多个常用收藏固定样例。客户端代码、生产Stitch和SDK未改，待用户确认设计后再实施。

## 上一轮：互动图标与完整表情素材

按用户截图修正双列卡片、详情及评论区的点赞／评论比例和间距，卡片标题换行后操作行保持对齐。新增128枚Noto表情与24张同一猫咪角色贴纸，已同步16个场景中的32个选择页面。当前入口为[本轮交接与预览](INTERACTION_AND_EXPRESSION_REFINEMENT.md)、expression-refinement-state.json、expression-refinement-validation.json。仅Figma设计与素材，未迁移应用代码。

## 此前增补：宠友圈与自有宠物走失联动（阶段记录）

新增两组四屏位图视觉提案：[宠友圈](pet-friends-circle-concept-v2.png)／[从走失到回家](pet-lost-linkage-concept-v2.png)。分别覆盖圈主页、回复、图文发布、可见范围，以及宠物主页、求助填写、私密线索、找回确认。内置 image_gen 制作，非可编辑 Figma UI；全部资料为示例。连接失败，未新增原生画板，现有 134 状态原型不含这两组新增流程。

先看 [宠友圈三条流程图](pet-friends-circle-flows.svg) 和 [走失联动总图](pet-lost-linkage-flows.svg)，再看 [宠友圈完整规则](PET_FRIENDS_CIRCLE_BRIEF.md)／[走失联动规则](PET_LOST_LINKAGE_BRIEF.md)。圈子采用双向确认、时间序、逐条可见范围的推荐草案；走失采用个人状态与公开求助分离，线索私密，主人确认找回后收尾。定位方式、部分隐私与照护协作仍待评审，不改变真实设置。

场景清单为 22 个圈子＋18 个走失**待创建场景**，不是已完成屏数。当前结果和剩余范围见 pet-social-lost-design-status.json；未改生产 Stitch 或应用代码。

## 前阶段已交付：手机社交、私信与群聊

最新范围：视频已由用户明确延期，首版图文宠物日记优先；多宠物空间、独立可见性与全站表情为新增待细化需求。详见 [交接](SOCIAL_CHAT_HANDOFF.md)。Figma 连接恢复后已将现有手机页六处文字同步为照片，专项预览见 [照片选择说明](image-only-permission-preview.png)／[真实性说明](image-only-verify-preview.png)；旧 PNG 是历史版本。新增功能界面不计入这次文字同步完成度。

手机交互演示已生成：[手机打开原型](https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/M-D?page-id=56%3A25&node-id=56-27&starting-point-node-id=56%3A27&scaling=scale-down&content-scaling=fixed) · [八屏总览与三条流程图](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=109-3347) · [最终预览](social-chat-final-preview.png) · [交接与验证边界](SOCIAL_CHAT_HANDOFF.md)。原 59 个根 ID 保留、六屏更新、新增 75 状态，合计 134 屏；589 条 NODE 动作含点赞组件切换，不含 BACK。横向主次按钮为 48 高／24 圆角真胶囊，卡片和输入仍为 16。

当前账本为 social-chat-state.json／social-chat-content.json／social-chat-final-validation.json／social-chat-prototype-flows.json。晒宠、评论点赞举报、一对一、消息请求、互动通知和群聊已供评审；输入与发送为固定状态，无真实服务，不同步全局数据。未实机逐条点击或完成读屏验收；全部设计确认后才完善代码。生产 Stitch 与应用代码未动。

## 前一阶段：59 屏手机 V2 样式统一（历史）

用户已要求全部应用 01 清爽亲切，现 59 屏普通手机稿已统一：[Figma 代表页](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=81-192) · [当前预览](friendly-mobile-final-preview.png) · [59 屏整板](friendly-mobile-all-preview.png) · [本轮范围与验收](FRIENDLY_MOBILE_ROLLOUT.md)。共享按钮、文字入口、输入边框、面板圆角与手机品牌强调色同步；详情使用“先聊一聊＋申请领养”。仅 Figma 手机视觉及静态原型，生产和其它端未改。

[原双方案对照](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=70-209)／[焦点修订预览](button-comparison-focus-preview.png) 保留作过程记录。已选方案保留中性灰 2px 外轮廓＋3px 间距，仅在焦点状态出现。

用户要求先做完整手机主版本，再补其他端，并反馈旧暗绿／陶土不舒服。因此当前评审入口改为 [手机 V2 视觉对照](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=61-12)／[手机主流程](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=56-27)。旧 00–09 保留，新增 10、11；Pad、桌面、Web 和暗色扩展暂缓。

59 个参考屏覆盖社区、领养主闭环、发布披露、互助、沟通与举报／申诉、账号隐私、次要出售分支及共用异常。轻浅中性底、深灰中文、珊瑚白字主动作和浅珊瑚次操作；照片承担主要情感。这是用户选定方向，不是大陆用户统一审美的证明。

[当前代表页 PNG](friendly-mobile-final-preview.png) · [当前手机整板 PNG](friendly-mobile-all-preview.png) · [范围／依据／待做](MOBILE_V2_BRIEF.md)。原 mobile-v2-*-preview.png 和第一轮 friendly-key-screens-preview.png 保留为历史过程，不作为当前视觉依据。

当前账本为 `mobile-v2-state.json`、`mobile-v2-content.json`、`mobile-v2-validation.json`、`mobile-v2-navigation.json`、`mobile-v2-fixes.json` 与各流程 `mobile-v2-*.json`。`mobile-v2-renderer.js` 需显式传入状态与指定内容；组件阶段还依赖已读取的官方 helper，不能不看账本整套重跑，以免创建重复节点。

当前验收为 friendly-mobile-validation.json：59 根画板、330 条静态导航／返回，原 329 条全部保留；21 个离散照片、无缺失字体，主按钮白字 4.60:1、控件边界 3.46:1，12 组指定颜色对通过。mobile-v2-validation.json 是统一前的阶段快照。不是设备／完整无障碍／逐条点击验收。输入、筛选、真实地图、短信、发布／报告提交未接入；其它物种完整字段、正式政策及边缘状态仍需补齐。普通 Design 不是原生 Spec。

## 前一阶段：四端黑夜版（保留参考）

先打开 [四端视觉对照](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=44-241)，再看 [跨端规则](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=36-28)。同一文件新增 05–09 页面；原 00–04 不变。当前团队未启用原生 Specs，经用户同意使用普通 Design，不是模拟原生 Spec。

| 平台 | 已交付 | Figma 起点 |
|---|---|---|
| 手机 | 14 屏，390×844；社区、领养、核验／举报、登录、互助等 | [手机黑夜版](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=36-4) |
| Pad | 2 屏，竖屏领养主从／横屏社区阅读 | [Pad 黑夜版](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=36-19) |
| 桌面客户端 | 2 屏，持续会话／发布工作区 | [桌面黑夜版](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=36-22) |
| Web | 3 屏，游客首页／分享详情／移动网页 | [Web 黑夜版](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=36-25) |

共 21 个唯一界面；对照板另有 4 个原生可编辑快照，不另计屏数、不自动同步，且移除了交互。新手机页有 49 条导航／返回动作；选中手机顶层画板后 Present。Pad／桌面／Web 为视觉稿，不宣称全部控件可操作。筛选、输入、获取验证码、发布与举报提交不执行；没有接入地图、真实案例或支付。

### 黑夜版预览与账本

- [四端对照 PNG](night-comparison-preview.png)
- [手机整板](night-phone-preview.png) · [Pad 整板](night-pad-preview.png) · [桌面整板](night-desktop-preview.png) · [Web 整板](night-web-preview.png)
- [跨端说明](night-overview-preview.png) · [新增组件样例](night-components-preview.png)
- [设计范围与验收清单](NIGHT_DESIGN_BRIEF.md) · [当前节点索引](night-state.json) · [最终结构／颜色核对](night-validation.json)

`night-renderer.js` 为原生画布生成源，不是客户端代码。`night-components.json`、`night-phone.json`、`night-pad.json`、`night-desktop.json`、`night-web.json`、`night-overview.json` 是创建阶段账本；`night-navigation.json`、`night-responsive-fixes.json`、`night-comparison.json` 记录后续连接／修正。早期阶段的尺寸以创建时为准，最终读回以 `night-validation.json` 为准。

最终核对：21 根画板明确 Dark、无缺失字体，13 个离散宠物照片节点；共 59 变量、7 文字样式、15 主组件（原 10＋新 5）。9 组指定暗色颜色对满足所设阈值：文字最低 6.59:1，控件边界 8.32:1。所有平台导出已查看，修正实例内容宽度、手机导航与对照行高；手机 49 条动作目标结构有效，未逐条点击验收。不能据此宣称完整 WCAG 合规。

仍待：走失详情／私密线索／状态维护、发布者回应／申诉等完整屏，320 宽度与文字放大、键盘／读屏和设备验证。这里是固定参考尺寸的跨端提案，不是全量自适应实现。工程继续遵守 Stitch V2，未迁移客户端。

## 原 V1 阅读顺序（00–04）

00 设计总览 → 01 视觉与组件 → 02 核心体验 → 03 信任与异常 → 04 可点击原型（示意）。04 中选中手机画板点击 Present；本页是评审稿的独立快照，后续需明确同步。

## 原 V1 文件说明

| 文件 | 用途 |
|---|---|
| `state.json` | 原 V1（00–04）页面、变量、组件、屏幕和动作节点的精确 ID；暗色增补另见 night-state.json |
| `state-*.json`、`refined-*.json`、`visual-fixes.json` | 分阶段创建与修正账本；旧动作 ID 可能已被替换，以 state.json 为当前入口 |
| `validation.json` | 最终结构、字体、离散照片、指定颜色对与原型目标检查 |
| `renderer.js` | 初版 Figma 原生节点生成源；需显式传入 PHASE / STATE，不是客户端代码 |
| `refine.js` | 首轮视觉复查后的定点修正；无真实业务处理 |
| `prototype.js`、`state-prototype.json` | 12 屏演示快照及导航／返回动作账本，不可当完整 App |
| `assets/` | 示例宠物照片及素材边界；不得把示例当真实待领养／出售档案 |

## 原 V1 最终预览

- [设计总览](overview-preview.png)
- [视觉与组件](foundations-preview.png)
- [核心体验](core-preview.png)
- [信任与异常](trust-preview.png)

PNG 仅为方便评审的导出，Figma 内仍是可编辑原生布局、组件与文字。总览保留初版导出，后续仅整理画板外素材，不影响画板内容；其余三张为最终修正后导出。

## 原 V1 验证记录

已核对 5 页、59 变量、7 文字样式、10 主组件、12 评审屏和 12 原型屏。检查的 18 组浅／深模式颜色对均满足所设目标：文字最低 4.62:1，控件边界最低 3.12:1。字体无缺失；仅图片照片使用位图。32 条原型动作的目标结构有效，未逐条执行点击验收。

不能据此宣称 WCAG 完整合规。当时待做的暗色页面已按上方范围补 21 屏；320 宽度与文字放大、键盘与读屏、逐条原型走查、完整申请／交接／回访／复核状态，以及狗狗和其他物种的具体资料字段与准入规则仍待验证或补齐。原 V1 的物种筛选、输入、上传、提交、接受／拒绝未模拟；消息 Tab 直接展开一条示例会话。

未运行编译，遵循全局不自行编译规则。未改应用代码、共享权限、付费套餐或其他云端文件；未删除任何云端文件。

原 V1 本地静态检查：23 个文本文件 UTF-8／无 BOM、JSON 解析及 Markdown 本地链接通过；相关已跟踪文档的差异空白检查通过。本次黑夜版连同交接文档核对 38 个文本文件、25 个 JSON、96 个本地 Markdown 链接，无编码／链接／解析问题；3 个已跟踪交接文档的差异空白检查通过。详见 `night-validation.json`。
