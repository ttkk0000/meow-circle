# 2026-10-29 Figma文档中心增量

已完成18份来源、21卷原生Specs全文同步与分页回读。[打开增量目录24:3](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=24-3) · [文档中心总导航1:9](https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=1-9)。新页为24:2，既有19卷ID保留，仓库README与文档总入口另建28:2／28:4。

## 本批范围与结果

- 18份来源覆盖现行基线、API速查、产品状态、设计索引、客户端README／方案／验收、UX、全局未读、消息分类、后台角色原文与任务交互、协作追踪、本轮交付、完整目录和同步总说明，以及仓库README和文档总入口。逐项路径、冻结SHA、节点与分卷见[manifest.json](manifest.json)。
- 每卷正文末尾含仓库来源、SHA-256与分卷链接；21卷都按120行分页读到终点，规范化正文与写入内容匹配，18份冻结源摘要匹配。GFM软换行、空白及表头强调的规范化不算内容遗漏；原始回读保存在[readbacks](readbacks)。
- 两份复核后的文档保留26:24／26:32节点，通过各2处editContent更新当前UX分类说明、173处动作／9张PNG和来源摘要，再次全文回读。没有使用setContent覆盖非空正文。
- 增量目录首次写入24:3并完整回读；总导航1:9通过12处精确editContent修改当前入口、日期和历史口径，未整篇替换。编辑前后正文保存在[navigation-before.json](navigation-before.json)／[navigation-after.json](navigation-after.json)，目录回读正文为[navigation-directory-after.md](navigation-directory-after.md)。
- 正文与两份导航中的188个文档中心链接目标逐个实读存在；21份新Spec均为DOCUMENT类型、归属正确且无画布重叠。原设计外链沿用本批设计证据，本同步不重新验收全部设计稿。

## 快照与评论边界

保留2026-10-27的189份来源、226分卷及旧manifest；旧manifest与b269504中内容逐字相同，未覆盖旧Specs或删除评论。旧API285操作／740消费者状态在总导航明确属于10月27日历史快照，当前入口使用288操作／748状态（740基线＋8协作补录）的本批文档。

动态AI CONTEXT／TASKS／TASK_LOG不在本批18份冻结来源中；旧中心的AI副本按原日期解释，当前工作状态回仓库核对。新增阅读副本不搬移旧评论，原讨论继续留在旧节点。

正文相对文档链接优先转成本批新Spec，其次旧Spec或原设计入口；与b269504内容一致的已提交源码使用该固定版本GitHub链接。未发布JSON／证据附件保留仓库相对路径文字，不伪造GitHub现成文件；具体路径见[unpublished-references.json](unpublished-references.json)。

## 验证资料与继续维护

[validation.json](validation.json)保存本次实际验证。`prepare_delta.py`准备分卷、冻结摘要和链接载荷，`verify_delta.py`逐卷核对回读；两个脚本只处理本地来源／JSON／Git读取，不启动工程。`sources.json`是本批固定来源清单；payload、write与frozen-batch文件是分批操作证据，当前有效摘要以manifest.files为准。

编译、构建、Gradle、Go程序、test、lint、类型检查、应用、迁移、注册、部署、Git提交与推送均未执行。文档回读、设计截图及源码静态核对不表示真实接口、设备、数据库或发布验收通过。
