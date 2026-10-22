# M&D 手机互动图标与表情修订

> 交互接续：[底部面板规则](WECHAT_STYLE_EXPRESSION_INTERACTION.md)已取代下文整页选择、清空草稿与发送前检查的旧流程。图标修订与128／24素材继续有效；下文预览为阶段记录。

2026-10-19｜普通 Figma Design；基于当前手机浅色主版本与 closure 账本继续。仅设计稿、设计配方与素材，未迁移应用代码。

[卡片互动修订](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=197-6381) · [当前手机首页](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=56-27) · [表情选择](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=142-3051) · [猫咪贴纸](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=142-3052)

## 互动图标

用户指出双列卡片点赞／评论不整齐。原组件把24px图标容器缩到20px，内部几何仍按24px显示；评论数字只有4px间距，标题自动增高也会推低操作行。

保留当前手机稿的字体、颜色和轮廓图标家族。图标框统一24px，爱心和气泡做20×18的光学校正，1.8px轮廓；图标与数字间距8px，按钮高48px。卡片使用“爱心＋数量／评论＋数量”，详情与评论区保留操作文字；已赞用实心与品牌强调色。标题预留两行，卡片统一326px高，同行操作区对齐。不同卡片仍使用12／6／8／5的原设计样例数量。

宠友圈“喜欢／评论／更多”也统一为24px图标框、14px文字与48px动作行。已赞按钮沿用取消喜欢流程回到未赞详情，评论和更多分别保留各自页面目标；没有把评论／更多变成点赞切换。

## 表情与猫咪贴纸

128枚 Google Noto 官方SVG，分常用24、情绪24、手势24、猫狗16、心意16、日常24。表情选择格图形36px、独立中文标签，六列；分类入口滚动到对应分组。静态资源来源、日期和署名见 [素材清单](expression-library-manifest.json) 与 [NOTICE](assets/noto-emoji/NOTICE.md)。

24张同一橘白猫角色的透明贴纸，内置 image_gen 生成；[原始提示词](assets/mnd-cat-stickers-prompt.md) 和 [图集](assets/mnd-cat-stickers-atlas-v1.png) 保留。图集6列×4行，1536×1024 RGBA；Figma每张贴纸是独立144px组件，96px选择预览，标签为独立原生文字。

替换16个场景中的表情／贴纸页面，共32页，保留各根画板ID、返回和确认上下文；包含圈子表态、日记、各类评论／回复、领养、私信、群聊和私密线索。预览结果使用选中图形与标签；打开选择器清除上一枚的显示变量，避免重复选择残留。只是固定设计原型，不是真实发送或全局业务数据。

## 验证与预览

读回32个选择页：每个表情页128枚、贴纸页24枚；所有选项的选中变量、标签及下一页目标与各自场景匹配。32个选择前／完成页的预览框为144px。首页两排卡片各自同高且操作行Y相同；分类中文标签保持一行。导出并查看卡片、表情、贴纸、选择预览和全部评论。详见 [最终读回](expression-refinement-validation.json)。

[卡片预览](interaction-cards-refined.png) · [表情预览](noto-picker-refined.png) · [贴纸预览](cat-sticker-picker-refined.png) · [选择结果](expression-preview-refined.png)

本轮为结构读回和导出目检，未逐项点击所有原型、未设备或读屏验收。未运行编译，遵循全局不自行编译规则。

## 接续

以 [节点账本](expression-refinement-state.json)、[素材节点目录](expression-refinement-catalog.json) 和 mobile-closure-state.json 的 expressionRefinement 为当前入口。本轮原生配方为 reaction-icons-refine.js、compact-engagement-refine.js、circle-inline-action-refine.js、noto-expression-components.js、cat-sticker-components.js、expression-selected-display.js、expression-panel-refine.js、expression-prototype-refine.js；配方使用的输入须来自上述账本，不能独立整套重跑。最终圈子已赞按钮的取消目标以验证账本为准。

旧 social-chat／mobile-closure renderer 的八枚表情配方为历史生成源；继续修改时检查本轮同步账本，避免覆盖当前128枚与24张贴纸界面。旧PNG不代表当前图标与表情状态；八枚旧Twemoji文件保留历史用途，当前共享表情主组件已替换为Noto。
