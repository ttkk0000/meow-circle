# 平板横竖屏原型预览优化 · 2026-10-22

更新：2026-10-22 10:44，Asia/Shanghai（UTC+08:00）。执行者：Codex。任务：AI-20261022-PAD-PREVIEW。

已直接优化当前Figma平板页403:3。打开 [Pad竖屏预览](https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/MND?page-id=403%3A3&node-id=311-25&starting-point-node-id=311%3A25&content-scaling=responsive) 或 [Pad横屏预览](https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/MND?page-id=403%3A3&node-id=311-26&starting-point-node-id=311%3A26&content-scaling=responsive)；也可在View prototypes菜单选择同名Flow。进入页面后，点右上角“预览 · 横屏／竖屏”即可切到同一状态的另一套布局。转动平板后仍需手动点击，未模拟自动感应方向。

## 实际修改

- 预览菜单由88个Flow精简为“Pad 竖屏 · 完整流程”“Pad 横屏 · 完整流程”。原画板、业务连接和场景原稿链接保留，原Flow名称与节点在[初始备份](pad-preview-2026-10-22-before.json)中。
- 常规703组加最新未读消息1组，共704组、1408张横竖屏对应稿加入方向切换。配对以同分组状态键为依据，最新消息311:379702／311:379703单独精确对应；没有用相邻坐标猜测目标。
- 按钮复用原生Secondary组件56:180，144×48，Noto Sans SC Medium。位于现有72高顶部栏，灵活文字列改为Fill，保留M&D／meow & doggie和既有色彩；原稿834×1194／1194×834尺寸及正文布局保持。
- 每个按钮仅执行一次Navigate到对应方向原稿；不附加提交、确认或变量赋值，resetScrollPosition、resetInteractiveComponents、resetVideoPosition均为false。此设置不等于已验证真实输入草稿或滚动锚点跨方向保留。
- 平板索引425:1839920补充手动切换说明，入口清晰标注方向与尺寸；直接预览链接带content-scaling=responsive。
- Chrome实际Figma原型设置面板已确认Device为No device；没有强行改动设备框或假称设计API支持该设置。
- 16张窄分屏专项稿保留独立用途，未指向不对应的横屏状态。

## 验证

[验收汇总](pad-preview-2026-10-22-validation.json)与[前半704控件](pad-preview-2026-10-22-audit-1.json)、[后半704控件](pad-preview-2026-10-22-audit-2.json)记录完整ID及双向目标：1408个唯一根、1408个唯一按钮；目标、标签、字体、根尺寸、48高触控区、72高顶部栏边界及兄弟重叠核对均无发现。后两批684稿另有修改前后正文签名一致证据；前两批响应中断后回读，确认已提交、无重复或错目标，未重复创建。

[原生结构回读](pad-preview-2026-10-22-native.json)的4个代表顶部栏各包含1个Frame、4个Text、1个Instance，均指向组件56:180，无整页截图或图片填充。这些是可编辑原生结构。

4张最终截图已查看：[竖屏首页](audits/2026-10-22-pad-preview/homeP.png)、[横屏首页](audits/2026-10-22-pad-preview/homeL.png)、[验证码状态](audits/2026-10-22-pad-preview/safetyP.png)、[横屏最新消息](audits/2026-10-22-pad-preview/messagesL.png)；[图片清单](audits/2026-10-22-pad-preview/manifest.json)不含临时资源URL。

## 恢复与实施边界

恢复依据为初始Flow、[1402稿头栏原宽及默认值](pad-preview-2026-10-22-preflight.json)、[4稿试装原值](pad-preview-2026-10-22-pilot.json)、[最新消息2稿原值](pad-preview-2026-10-22-messages-before.json)及最终两份audit的root／header／flex／control／target映射。移除演示按钮并恢复对应头栏值即可恢复原布局；不要自动重放生成配方。

**Prototype only / Orientation switch是Figma演示控制，不属于M&D正式功能，客户端不实现这两个方向预览按钮。** 真实App按可用窗口宽高适配；旋转时草稿、对象、会话和滚动锚点的保留仍按[跨端自适应合同](CROSS_PLATFORM_ADAPTIVE_CONTRACT.md)实施与验收。

静态回读和截图不代表原型播放或iPad实机验证。未运行编译，遵循全局不自行编译规则；未运行工程测试、服务或设备，未提交或发布。

交付文件静态检查通过：25个文件、14份JSON、4张PNG及145处本地链接；文本为UTF-8无BOM，JSON可解析、PNG签名有效，设计索引diff空白检查通过。最终静态检查时间为2026-10-22 10:46 UTC+08:00。
