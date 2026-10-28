# M&D 运行、迁移与交付核对入口

更新：2026-10-27。以[现行基线](CURRENT_BASELINE.md)、[产品交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md)和[文档目录](DOCUMENTATION_CATALOG.md)为准；本文仅记录源码与执行条件，不表示运行通过。

## 当前执行规则

本机AGENTS禁止Gradle与Go build／run／test，默认也不执行编译、构建、assemble、test、lint或应用启动。文档中的命令只是后续参考，不能当执行授权；本轮仅做文档静态核对和Figma写入。

## 按目标选择手册

| 目标 | 权威资料 | 先落实的条件 |
|---|---|---|
| 服务端环境与发布 | [v2恢复手册](server/V2_RELEASE_RUNBOOK_2026_10_23.md)、[服务端合同](server/MOBILE_SERVER.md) | 数据库、真实配置、备份与责任人 |
| Android／iOS／Web交付 | [交付指南](client/DELIVERY_GUIDE_2026_10_25.md)、[占位模板](../deploy/examples/README.md) | 域名／证书／签名、真实参数与运行授权 |
| 数据库升级 | [迁移清单](CURRENT_BASELINE.md)、[数据字典](DATA_DICTIONARY.md) | 001–013依序升级，既有库不依赖首次容器挂载 |
| Worker与外部投递 | cmd/worker、mobileworker、mobileapi/jobs.go | 单个具名job权限、原检查点、租约与真实回执 |
| 业务后台 | [10月27日源码交接](client/COMPLETE_LANDING_2026_10_27.md) | 当前角色／对象版本、未知结果人工核对 |
| 运维台 | [运维源码合同](server/OPERATIONS_CONSOLE_2026_10_25.md) | 环境隔离、审批与审计；执行器仍占位 |
| Figma文档 | [同步交接](figma-sync/README.md) | 文本／节点／来源摘要一致，保留历史状态 |

## 配置与启动前检查

| 配置 | 当前语义 | 未满足时 |
|---|---|---|
| DATABASE_URL | 非development环境必需 | 返回配置错误；PostgreSQL失败不回退内存 |
| REDIS_URL | 可选读缓存 | 失败可直连PostgreSQL |
| APP_ENV、MND_ALLOW_MEMORY_MOBILE | development及显式内存边界 | 不将内存／demo开放为生产数据 |
| MND_AUTO_MIGRATE | 示例默认0 | 不擅自对现有数据库升级 |
| MND_WEB_ORIGIN | Web来源与Cookie／CSRF合同 | 不通过放开CORS绕过身份与角色 |
| MND_OPERATORS_JSON、MND_OPS_ENVIRONMENT | 具名后台及环境 | 不使用消费者会话代替角色 |
| MND_WORKER_JOB、API_URL、OPERATOR_TOKEN、STATE_FILE | 单个Worker任务和持久检查点 | 缺配置不得报告作业成功 |
| 认证／地图／年龄／推送／存储配置 | 见[环境示例](server/environment.example) | 保持关闭或明确不可用，不记录真实Secret |

环境示例与deploy/examples是模板，example.invalid、空凭据与占位参数不代表已经准备好。公众号及第三方注册此前按用户要求暂停，不因本轮文档整理恢复注册、认证或付费。

## 数据库与恢复

001–013包含旧基础表、007的32张mobile表、008动态10表、009讨论2表、010运维记录、011查询索引、012Worker检查点和013来源声明。45张mobile表与技术表分别统计。已配置数据库但Schema不完整时服务初始化失败；应先备份并依手册核对迁移，不能自动重置或删库。

资料清理先核对证据保全、当前权限和外部对象回执；接受数据请求与清理完成分别展示。回滚／恢复必须保存业务版本、原请求和处理记录，不能用UI回退掩盖服务端已执行的命令。

## 静态检查与证据

可做的非编译核对包括git diff --check、冲突标记扫描、文档本地链接、源码符号／路径、OpenAPI与DTO字段映射、资源来源和指纹。实际执行结果应写明时间、工作树和范围。历史检查脚本“通过”、测试源码存在或设计截图不能当本轮运行证据。

构建、类型检查、数据库迁移、真实SDK回调、设备／浏览器、并发／网络恢复、无障碍与容量必须另有授权及可复现结果。当前整体发布未验收；完整搜索接收端与发布／重启执行器继续保持用户指定占位。
