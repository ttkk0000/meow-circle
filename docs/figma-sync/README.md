# M&D Figma文档与图谱同步

同步日期：2026-10-27（Asia/Shanghai）。本轮用户明确授权完善本地文档并同步至Figma。

- [打开Figma产品与工程文档中心](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=1-9)：原生Specs全文与表格，按8个页面分类，长文按段分卷。
- [打开FigJam UML图谱](https://www.figma.com/board/t6ongKuaGHleURYNfsLA6e?node-id=18-2707)：组件、类关系、真实数据库表、时序和状态图。
- [原设计文件](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC)：消费者、平板／Web／Desktop与后台原稿继续保留，文档中心不替代视觉权威。

## 来源与格式

| 内容 | 仓库来源 | Figma形式 |
|---|---|---|
| 产品／UX、设计规范、客户端、服务端、工程手册 | [全仓目录](../DOCUMENTATION_CATALOG.md) | 原生Figma Specs，保留正文、表格、编号和代码块 |
| API速查 | [API_CATALOG.md](../API_CATALOG.md) | 原生Specs表格，253路径／285操作 |
| 数据字典 | [DATA_DICTIONARY.md](../DATA_DICTIONARY.md) | 原生Specs表格；FigJam ER实体为可编辑表格 |
| 12份UML源 | [uml](../uml/README.md) | 10份Mermaid转换图＋2份原生类关系图，源文件另存Specs |
| 既有产品流程SVG与HTML | [产品流程](../design/flows/index.html) | 保留来源日期与适用范围，独立归档查看 |
| AI交接与历史审计 | [任务与日志](../ai/README.md)及设计审计 | 原生Specs，明确为交接或历史证据，不升级为功能完成 |

同步记录由[catalog.json](catalog.json)、[source-baseline.json](source-baseline.json)及完成后的manifest.json维护。本地原始文本与UML源为维护输入；Figma中的正文是本次阅读快照，后续双端修改应先对比再更新，不能直接覆盖他人评论。

## 核对与边界

完成标准为：全文源清单、每卷Figma节点与来源SHA-256、写入后分页回读、图表结构及抽样截图、文档本地链接与冲突标记和git diff --check。未推送的新文件链接优先指向本中心对应原生文档；已在HEAD存在的源码可使用GitHub固定版本链接。目录中的带日期报告保留原结果，不伪造重跑。

当前文档完善不授予构建／迁移／真实发布／第三方注册或收款权限。按仓库规则未运行编译、构建、test、lint或应用。整个产品尚未完成全量运行与发布验收。


## 2026-10-27同步批次完成结果

- 175份Markdown完成全仓目录与本地链接核对；现行入口、PRD／UX2.2、代码逻辑／运行手册、数据字典及API速查已维护。带日期历史报告保留原验证层次，不宣称所有历史段落已重新按当前源码验收。
- 同步189份文本来源（175份Markdown、12份PlantUML、2份历史Mermaid），共226个原生Specs分卷；另有文档中心导航。每卷均写入全文、来源摘要与分卷跳转，分页回读后按GFM规范化核对正文，无遗漏卷。
- FigJam有12张图谱；ER为45张可编辑实体表，按五个业务区查看，users在各区重复出现仅为同一表的阅读锚点。2张类关系图为原生形状和连接线；复杂完整支路继续查源合同与代码。
- 7张产品流程SVG保留可编辑文字；49张文档配图已上传并逐一核对IMAGE填充。原文中的图片引用在阅读副本中链接到本文件的配图节点，未丢弃图片。
- 截图抽查了类图、ER分区、时序、状态与产品流程；发现并修正类图穿线和预约自循环异常路由。不是所有图像逐像素或应用运行验收。
- 全仓175份Markdown的UTF-8／BOM、冲突标记和本地文件链接核对通过，12份PlantUML起止闭合通过，git diff --check通过。PlantUML未用JVM渲染；未运行构建、test、lint或应用。

[source-baseline.json](source-baseline.json)保存源码口径；[manifest.json](manifest.json)保存每个来源的SHA-256、远端节点／分卷、配图与图谱映射；[validation.json](validation.json)保存本轮实际核对结果。Figma中格式会被原生GFM规范化（软换行、表格分隔线、引用列表等）；验证比较内容，不要求原始Markdown字节完全相等。

本轮本地文档尚未提交／推送，Figma同步已获用户本轮授权并实际完成。换机需同时携带本地改动；Figma里的源码链接固定到已存在的1fe5302版本，新文档互链指向本中心，避免把尚未推送文件链接成GitHub现成文件。

## 2026-10-28后续增量边界

本地新增邀请协作源码、后台角色设计与实现后，旧manifest仍代表2026-10-27全文同步快照，不再保证当前每份文件与旧SHA一致。后台新增18稿与原生角色规范已单独写入Figma，见[本批设计清单](../design/figma-study/admin-roles-2026-10-28/README.md)；本地现行基线和API速查已随代码更新，完整中心的后续增量应按实际写入／回读结果维护，不能只改旧SHA冒充已同步。
