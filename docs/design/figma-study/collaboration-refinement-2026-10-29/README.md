# 协作退出与有限工作区：最终细化

2026-10-29，`AI-20261029-FIGMA-REFINE`。本轮在原 Figma V2 中完成既有 8 状态 × 5 布局共 40 张稿的收尾，并补齐共用照片选择页的返回动作。原 Section、页面根 ID、默认浅色和组件体系保留；消费者追踪仍为 748 条，本轮没有新增消费者状态或业务源码。

## 已保存的修改

- 手机照护、只读照护、介绍工作区统一“查看协作与退出”，进入对应已接受邀请页；其他布局已有入口一并核对。
- 五布局的“维护介绍照片”均进入本布局照片选择页；确认和取消共 10 个控件使用 `BACK` 回到调用页。该共用页仍为学习素材示意，不表示已经选择真实相册或保存媒体。
- 四类退出确认／结果页各补事项、邀请与基准／响应版本，共 20 处，均明确“学习示例”。不把示例 V4／V3 当服务端真实回执。
- 退出照护、退出介绍与授权不可用的顶部返回共 15 处进入本布局消息页，避免返回到旧授权画面。
- 平板 16 个方向切换改为本批相同状态的横竖屏对应稿，不再落入旧模板。方向按钮仅作 Figma 预览，不是客户端功能。
- Notice 正文按容器宽度自动换行，父容器随内容增高；沿用原次按钮、字段、行和返回图标实例。已读取按钮七项最终语义颜色别名，无需改色或新建主题。

创建 22 个节点：20 个示例标注、1 个只读工作区按钮实例及其文字子节点。修改节点清单见[合并证据](evidence-summary.json)，其中创建与修改计数可能重叠，不能相加为页面数量。

## 入口与完整截图

| 布局 | Figma Section | 已查看的完整八稿图 |
|---|---|---|
| 手机 | [910:357845](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=910-357845) | [phone-final.png](phone-final.png) |
| Web | [915:386337](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=915-386337) | [web-final.png](web-final.png) |
| Desktop | [916:379926](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-379926) | [desktop-final.png](desktop-final.png) |
| 平板竖屏 | [916:1127482](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-1127482) | [tabletP-final.png](tabletP-final.png) |
| 平板横屏 | [916:1128047](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-1128047) | [tabletL-final.png](tabletL-final.png) |

五张均为 Figma 原生导出、未经拼贴或图像修改的 PNG；已查看完整布局，未发现本轮修改造成的裁切、重叠、底部动作遮挡或文字异常。截图回执保留尺寸与 SHA-256，不保存短期下载地址。

## 验证层次

- 40 张稿的可编辑层级／节点类型、图片填充、字体、文字与直接父容器边界已读取。字体仅 `Noto Sans SC`／`Manrope`；未发现文字越界或整屏 UI 位图。各稿图片填充节点清单为空，既有文本、向量与组件实例保留。
- 55 个关键按钮动作通过；随后对全部关键按钮、10 个照片返回、15 个顶部结果返回、16 个方向切换，共 **96 个动作作独立只读回读，失败 0**。完整预期见[expected-actions.json](expected-actions.json)，实际结果分存四份 `actions-readback-*.json`。
- 全稿结构读取时共 249 条可见 NODE 链接，目标均存在、同页且无自循环。随后 12 个跨端结果返回由 BACK 改为消息目标，16 个旋转目标替换；按原结构与最终写入回执合并为 261 条。该 261 是回执合并口径，不冒充第二次全量遍历；最后独立回读的范围为上述 96 动作。
- 五布局截图检查已完成；未播放原型、调用真实相册或验证 API／设备。未编译、类型检查、构建、测试、lint 或启动应用，未提交／推送。

## 证据与恢复

[上批 40 稿清单](../collaboration-exit-2026-10-28/README.md)、[原节点／源稿](../collaboration-exit-2026-10-28/nodes-and-validation.json)和[对应源码恢复点](../../../client/REMOTE_CONTINUATION_2026_10_29.md)保留。

`write-*.json` 为首轮四页写入回执；`audit-first-56-25.json` 与 `audit-final-403-*.json` 为 40 稿结构读取；`navigation-final-*.json` 为返回与旋转的最终写入；`actions-readback-*.json` 为后续独立读取。`recovery-*.json` 记录中断后先核对的真实状态。

`refine-template.js` 是本轮首批成功模板；`finish-and-audit-template.js` 是中间尝试，仅手机调用完成。它按精确名称 `Back` 查找，不能直接用于名称为 `Back / preserve current task` 的跨端稿，也不要重新运行整批写入。实际最终节点与动作回执优先。

10 月 28 日自动审批额度失败保留为历史。本轮正常 Figma 工具已成功，原五项收尾清单完成，没有绕过审批。跨端中间调用未取得完整返回时，先读取画布，再按实际节点继续；没有把中断当保存成功。PNG 的 URL 下载先遇沙箱出站限制，正常审批后仍非 PNG，改用工具支持的原生内联导出并成功保存。

本轮设计细化与证据交付完成；全量 748 状态的逐端视觉、真实运行及既有上线条件仍未完成。本轮本地文件未提交／推送，执行标记已释放，无后台任务。
