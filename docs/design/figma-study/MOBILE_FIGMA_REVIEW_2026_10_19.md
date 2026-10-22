# M&D 手机端 Figma 本轮评审

2026-10-19｜原生可编辑 Design｜状态：等待用户评审；客户端实施尚未开始

[手机端评审入口](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=238-371769) · [可点击原型](https://www.figma.com/proto/3OFOC3vu18Kx9sRPjummOC/MND?page-id=56%3A25&node-id=238-371769&starting-point-node-id=238%3A371769&scaling=scale-down&content-scaling=fixed) · [六屏可编辑总览](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=251-375655)

## 本轮交付

- 延续已选“清爽亲切”手机方向：Noto Sans SC、浅中性背景、珊瑚主动作、48 高按钮、16 圆角卡片与输入。主动作使用实心按钮，取消等次动作保留描边；需确认的按钮默认不可提交。
- 手机号登录／注册、验证码、密码备用、微信／QQ，以及 iPhone 的 Apple；首次创建与关联已有账号分开，补取消、失败、冲突、恢复原任务、换绑、恢复、退出与注销。
- 好友未确认／等待／接受／撤回；拾获与自有宠物走失分入口，认领材料私密核实，分别呈现争议、双方交接及关闭公开求助。
- 预约 V1／V2 双方确认、取消、过期、冲突与历史记录。恢复原时间需提出 V3 并重新确认，不把旧 V1 直接恢复为生效。
- 领养申请补充／不通过、双方试养、双方交接、30 天私密照护更新、照护问题及安全接回协商。
- 群主转让与解散分开；原群主和接收人的完成状态分开。领养介绍协作者仅维护介绍／照片／答疑，领养后的照护者仅访问授权更新，两类权限不能互相扩张。
- 每类通知独立开关，附近提醒单独管理；补频率、系统权限、材料过期／待补充、处理与申诉结果。
- 多图进度、局部失败／重试／移除、排序与草稿恢复。上传返回原表单，不把认领或照护材料带到公开发布。
- 沿用 128 个 Noto 矢量表情、24 张猫咪贴纸、16 个底部面板场景。新增连续插入、光标、整颗删除、收起保留、多个最近／收藏样例。
- 320 宽登录／预约／贴底表情面板，160% 字号登录／交接样例；修正字段主组件内部宽度，窄屏控件实测静态宽度为 288／288。

商业到访分支使用原出售示例“橘子／声明总额 ¥2,400”，展示接待方、费用范围与逐项一致／不符／暂不能判断。记录核对不会自动成交或付款。

## 对照此前 13 项发现

此前[覆盖报告](audits/2026-10-19-mobile-docs/MOBILE_DOC_COVERAGE_AUDIT.md)保留为补图前证据，不再作为“认证画板未制作”的当前状态。

| 发现 | 本轮设计结果 | 主要评审入口 |
|---|---|---|
| A01 最新基线 | 统一评审起点、当前节点账本与索引；生产合同待用户确认后映射 | completionHub / 238:371769 |
| A02 认证全链 | 两平台入口、三方、首次建号／关联、异常及任务返回 | reviewAuth / 245:370296 |
| A03 好友前后权限 | 原申请人物卡已改到未确认资料；补等待、撤回与接收端 | friendUnconfirmed |
| A04 拾获／认领 | 独立发布、私密核实、争议及双方交接结果 | helpType / foundActive |
| A05 预约／报价 | V1／V2／V3、等待、拒绝、取消、失效、冲突及商业到访核对 | reviewAppointments / serviceQuote |
| A06 领养结果 | 补充、拒绝、双方试养／交接、私密更新和退出协助 | adoptionDecision / trialAgreement |
| A07 角色邀请 | 转让／解散拆开，两类协作范围及接受／拒绝／撤回／撤权 | reviewStates |
| A08 账号管理 | 绑定、换号、恢复、退出、注销影响及处理状态 | authAccounts |
| A09 通知 | 类别开关、频率、系统权限与附近订阅分开 | notificationSettings |
| A10 核实／处理结果 | 过期、待补充、核实边界、举报与申诉结果 | reviewModeration |
| A11 多图草稿 | 进度、部分失败、重试、移除、顺序与返回上下文 | mediaChoose |
| A12 表情编辑 | 连续插入／光标／删除及多个常用收藏的固定样例 | expressionReview |
| A13 手机适配 | 320 宽与大字原生样例，保留滚动、安全区及点击面积 | reviewResponsive |

本轮追踪 **213 个新补或重绘的页面／状态**，含复用旧根节点。合并历史账本静态回读 **579 个唯一参考根 ID**，没有丢失根节点、空白占位或跨错页面；这些数字包含变体和覆盖层，不代表独立产品流程数量或运行验收百分比。模块目录已重新计数。

## 当前文件与复核方法

- 当前增补：[节点账本](mobile-completion-state.json)、[完整页面文案](mobile-completion-content.json)、[静态核对结果](mobile-completion-validation.json)。
- 历史基础：mobile-closure-state.json、wechat-expression-interaction-state.json、expression-refinement-state.json。当前节点需与增补账本合并；早期 content 分片／auth 草稿不是最新交付入口。
- 构建配方仅用于 Figma Design，不是客户端代码。mobile-completion-renderer.js 需结合主组件宽度、角色保护、原入口重接和最终视觉补丁；不能直接用旧 closure renderer 重建当前页面。最终组件切换需随后应用 mobile-completion-footer-fix.js，恢复固定通栏尺寸和确认变量；单跑早期 polish 补丁会回缩宽度。
- [最终总览](mobile-completion-overview.png)、auth-ios-review.png、expressionNarrow-review.png 等为本轮截图证据。Figma 原稿优先于旧 PNG。

## 评审与实施边界

本轮完成设计补绘与静态回读，没有接入短信、三方授权 SDK、上传、推送、地图、支付或账号删除服务。Figma 的输入与状态为固定样例；连续表情图不能证明任意 Unicode 输入、真实光标、多个常用项持久化或网络并发已经实现。

未逐条执行原型点击、真实设备、系统键盘、读屏或动态字体验收。未运行编译和测试，遵循全局不自行编译规则。

正式协议、数据保留／注销规则、运营核实范围与服务时段仍需与实际能力匹配；本轮没有编造上线承诺。生产 Stitch、Pad／桌面／Web 与应用代码未迁移。用户先评审并确认手机 Figma，再开始客户端实现与工程合同映射。
