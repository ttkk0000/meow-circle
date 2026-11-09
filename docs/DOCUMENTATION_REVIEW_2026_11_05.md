# 拉取分支与文档核对

2026-11-05（Asia/Shanghai），任务`AI-20261105-PULL-DOCS`，执行者Codex。范围为当前main上游同步、文档入口与现行基线、目录覆盖、实施台账及证据一致性；未恢复业务开发。

## Git与本地成果

- 工作树为当前仓库唯一工作树，main／48a3cfd。`git fetch origin`成功；HEAD与origin/main双向差异为0／0，没有新增提交。
- `git pull --ff-only origin main`的重复联网遇到TLS连接中断；随后使用本次已获取的origin/main执行`git merge --ff-only origin/main`，返回`Already up to date.`。没有创建合并提交、切换分支或暂存本地成果。
- 既有源码、批次005–020文档／证据及未跟踪文件保留；这些成果仍未提交。本轮未commit或push，不能据此认定未提交成果已到远端或另一台电脑。

## 静态核对与修订

| 核对项 | 实际证据与处理 |
|---|---|
| 当前源码数量 | OpenAPI 255路径／288操作，服务端141／Web143个schemas，DTO映射88对，SQL迁移13份；直接读取JSON、脚本中的映射字典和文件目录 |
| 当前客户端数量 | shared/client直接子文件33份Kotlin，clientUi/clientui直接子文件53份Kotlin；web-client/src递归100份TS／TSX。基线原33／51已改为33／53，并补Web计数 |
| 重复入口漂移 | 17份文档顶部“现行入口”仍复制旧数量，部分停留253／285及28／41。现统一指向现行基线；正文带日期的旧批次结果保留 |
| 新旧状态表述 | 基线与交付矩阵补后续消息／后台源码交接，最新恢复点指向批次020；005–010尾部交接明确标为历史检查点 |
| 文档目录 | 197份已跟踪Markdown中，skills工具文档1份不纳入产品目录；发现的10份既有交接漏项已补。196份既有文档加本报告共197份，目录逐文件覆盖 |
| 本地文档链接 | 初始扫描2144处本地文件目标，无失效发现；修改后198份Markdown再次检查，无失效。跳过代码块、外部URL及纯锚点，不包含远端可访问性或标题锚点验证 |
| JSON与编码 | docs内718份JSON均可解析；197份既有Markdown可按UTF-8读取，无BOM或替换字符；本轮变更再次检查UTF-8无BOM及差异空白 |
| 状态追踪 | 两份台账均748条，key、设计根和各端源码归属一致，3772个有效设计引用，适用源码路径无缺失；运行标记均false |
| 当前源码证据 | current-source-checkpoint.json的57份源码SHA-256（LF归一）全部匹配，批次020的6份最新源码指纹全部匹配；历史批次指纹保留原时点，不重写成当前验证 |

入口修订只移除顶部重复统计，不改变PRD、UX、认证资格、设计主题或业务决定。[AI跟踪入口](ai/README.md)和[文档入口](README.md)接手顺序改为先读现行基线，10月22日换机文档保留历史用途。

## 仍未完成的工作

批次020最终五份语法待读仍保留：AuthPhonePages.kt、SecurityPages.kt、AuthPhone.tsx、Account.tsx、PurposeCheck.tsx。历史907／746MiB低内存跳过记录与当前指纹一致性分别解释；本轮未运行语法器，不将指纹匹配记为语法或类型检查通过。

认证删除／会话／返回、其余消费者／后台状态、部分受限服务合同及748状态逐端视觉／真实运行仍待继续。当前运行标记保持0，未把设计覆盖、源码路径或文档整理升级为功能完成。

本轮仅修改本地文档，Figma远端、历史manifest及同步快照未更新；远端阅读副本继续以[同步入口](figma-sync/README.md)记载的实际日期为准。

未运行编译，遵循全局不自行编译规则；构建、类型检查、应用测试、浏览器／设备、数据库迁移和部署均未验证。执行标记在核对结束后释放，无后台任务。

证据入口：[现行基线](CURRENT_BASELINE.md) · [交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md) · [分批进度](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md) · [主状态台账](client/figma-implementation-trace-2026-10-24.json) · [实施台账](client/figma-implementation-2026-10-29/state-progress.json) · [57份当前源码指纹](client/figma-implementation-2026-10-29/current-source-checkpoint.json) · [批次020证据](client/figma-implementation-2026-10-29/batch-020-evidence.json)。
