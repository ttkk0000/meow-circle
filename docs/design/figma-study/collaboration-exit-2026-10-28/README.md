# 协作范围、本人退出与有限工作区

2026-10-28，任务 `AI-20261028-CONTINUE-DESIGN-CODE`。原设计文件 [M&D Figma V2](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC)，新画面已保存40份。2026-10-29已完成[最终细化、96动作独立回读与五布局完整截图](../collaboration-refinement-2026-10-29/README.md)；下方保留本批初始证据及当时阻塞，不将其当当前待办。

## 入口

| 布局 | Section | 尺寸 | 新状态数 |
|---|---|---|---:|
| 手机 | [910:357845](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=910-357845) | 390×844 | 8 |
| Web | [915:386337](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=915-386337) | 1440×1000 | 8 |
| Desktop | [916:379926](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-379926) | 1440×1000 | 8 |
| 平板竖屏 | [916:1127482](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-1127482) | 834×1194 | 8 |
| 平板横屏 | [916:1128047](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=916-1128047) | 1194×834 | 8 |

每布局包括：退出照护确认、照护已退出、退出介绍确认、介绍已退出、授权照护列表、只读照护、授权不可用、有限介绍编辑。另修改10份既有已接受邀请稿的主／退出入口；新工作区隐藏不适用于协作者的发布人菜单。新状态独立记账，不将既有740索引直接升级为780或已运行完成。

沿用当前V2默认浅色与既有组件／字体。授权照护不开放申请材料、协议和完整宠物档案；介绍协作仅限该宠物介绍与照片，不能再邀请、决定领养或变更归属。本人退出不等于交接、接回或取消原领养，旧记录保留。

## 实际检查

[完整节点清单](nodes-and-validation.json)列出根ID、创建／改动ID、源稿与构建参数。[结构回读](structural-audit.json)覆盖40新稿与241条已有可见链接：未发现目标缺失、跨页、自循环或整屏图片。此项不证明每个必要控制都有动作，也不等于播放原型。

初次检查时，手机 `910:357927`（照护）、`910:357987`（介绍）、`910:357846`（退出确认）的高保真上下文和截图已查看，五布局最终截图当时未完成，现已由[后续细化任务](../collaboration-refinement-2026-10-29/README.md)补齐。发布组件 `836:21`、`838:21`、`838:58`另已读取上下文和截图，接到共享Compose，见[本批源码交付](../../../client/CONTINUE_DESIGN_CODE_2026_10_28.md)。

[builder.js](builder.js)是本批最近成功的Figma MCP构造模板，需注入清单中的plan／states，不是Node应用。手机早批与后续跨端有差异，真实画布与返回节点清单优先。禁止重新整批生成已有Section。

## 历史审批阻塞与恢复

2026-10-29恢复结果：以下五项已通过正常Figma工具完成，另补照片选择返回、结果页顶部返回与平板同状态方向切换。原Section及40根均保留，未绕过审批。当前结果与截图见[本轮收尾](../collaboration-refinement-2026-10-29/README.md)。

40稿保存及结构回读之后，手机、Web、Desktop和平板的最终修订，以及按钮颜色别名读取，均因自动审批审核额度耗尽而未执行。这是审核服务失败，不是不安全裁定；没有绕过自动审批。具体状态见[completion-status.json](completion-status.json)。

当时登记的五项恢复清单（现已完成）：

1. 手机照护／介绍工作区的次入口统一为“查看协作与退出”，指回原接受邀请页；只读照护补同入口。原页careActive `240:368338`、introActive `245:370292`。
2. 五布局publisherWorkspace的“维护介绍照片”明确接同布局circlePhotoChoose：手机 `142:2871`、Web `311:380380`、Desktop `311:380381`、平板竖 `311:380382`、平板横 `311:380383`。检查必要动作完整性，不能只核对已有链接。
3. 每布局四个退出确认／已退出画面补明确的示例事项、邀请与版本标注；示例不得伪称真实数据或原请求回执。
4. 读取70:148按钮颜色集的最终别名值，再决定现有次按钮的必要对齐，不根据别名猜色。主／pressed／secondary／onSecondary／border／disabled／onDisabled引用分别70:150／153／157／159／161／165／167。
5. 再读40稿结构／文本边界与关键动作，并查看五布局最终图。额度失败后未执行的动作不能当作已保存。

源码指纹、运行边界与交接见[本批交付](../../../client/CONTINUE_DESIGN_CODE_2026_10_28.md)。未编译、测试或启动应用；未提交推送；没有后台执行任务。

2026-10-28后续代码已补邀请独立详情、本人退出／邀请人撤销全页、按事项与scope收敛的邀请列表、照护失权／410清理与完整核对页；[13个既有＋8个新增状态的具体分支](../../../client/collaboration-page-source-map-2026-10-28.json)优先于旧family归属。该代码检查点当时尚未执行远端收尾，后由2026-10-29设计任务完成。原`syntax-results.json`仅为上一源码检查点；当时源码和语法器内存失败见[后续指纹](../../../client/collaboration-pages-evidence-2026-10-28.json)／[失败记录](syntax-followup-incomplete.json)，不将非零退出算通过。

2026-10-29后续检查点已改用现有Node18并正常退出，旧Node24失败保留历史；[拉取后的35份语法读取](../../../client/remote-continuation-syntax-2026-10-29.json)与[源码／边界](../../../client/REMOTE_CONTINUATION_2026_10_29.md)是当前源码恢复入口。40根与具体源码分支已纳入合并后的748条索引；[远端最终细化与五布局截图](../collaboration-refinement-2026-10-29/README.md)已完成，没有重新造稿或绕过审批。全量748状态的逐端视觉及真实运行仍未完成。
