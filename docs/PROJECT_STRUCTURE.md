# M&D 项目结构与源码入口

更新：2026-10-27。以[现行基线](CURRENT_BASELINE.md)、[产品交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md)和[文档目录](DOCUMENTATION_CATALOG.md)为准；本文仅记录源码与执行条件，不表示运行通过。

## 当前V2结构

| 路径 | 职责 | 维护边界 |
|---|---|---|
| cmd/server/main.go | HTTP进程入口 | 组装服务、配置与退出处理 |
| cmd/worker/main.go | 单个具名job的有限批次／显式循环 | 默认once=true，检查点与业务回执分开 |
| internal/platform/api | 服务组装、静态站与v1兼容入口 | NewRouterWithError；生产配置失败应报错 |
| internal/platform/mobileapi | V2 HTTP、浏览器会话、具名后台、地图代理 | Cookie／Origin／CSRF、权限、错误码与X-Request-ID |
| internal/identity | 认证、会话、资格、年龄与监护 | 登录与互动资格分开 |
| internal/mobile | 领域模型、Repository／Tx与各业务命令 | 无SQL、HTTP或SDK依赖；45种Kind |
| internal/store/postgres | pgx与真实SQL存储 | 只读REPEATABLE READ、写SERIALIZABLE、版本CAS |
| internal/platform/mobileauth | 外部认证、年龄、推送等适配器 | 未配置能力不可伪装成功 |
| internal/platform/mobileworker | Worker HTTP客户端、检查点与租约 | 按命名作业权限请求，失败保留恢复 |
| internal/region | 大陆城市版本化目录与兼容查询 | 城市目录不授予业务准入 |
| migrations | 001–013 SQL及独立backfill | 依序迁移；008含10表动态SQL |
| kmp/shared/.../client | 28份公共Kotlin文件 | MVI、API、DTO、草稿、原请求和恢复 |
| kmp/clientUi | 41份公共Compose文件、48个FeaturePage | Android／iOS／Desktop共用；自适应与系统能力端口分开 |
| kmp/androidApp/.../mobile | Android手机／平板宿主 | API26–37；系统相册、地图、登录与通知桥接 |
| kmp/iosUi 与 kmp/iosApp/ComposeApp | Compose页面与Swift系统宿主 | 地图、官方认证、Keychain及系统能力桥接 |
| kmp/desktopApp | Compose Desktop宿主 | 系统文件、钥匙串、浏览器能力与窗口 |
| web-client | React DOM消费端及两个独立后台 | /app、/app/operator、/app/ops |
| deploy 与 docs/client/DELIVERY_GUIDE_2026_10_25.md | 交付模板、脚本和手册 | 真实构建／签名／部署未执行 |
| docs/api、docs/uml、docs/figma-sync | API、图谱源和Figma同步追踪 | 文档与源码／运行证据分别记账 |

## 旧端与设计资料

web/为旧静态站、API v1兼容页面和Stitch镜像设计板；mobile/为旧Expo；KMP legacy Stitch组件仍保留兼容。它们不自动等于当前V2功能完成，也不能覆盖已批准Figma V2默认浅色。旧主题、存储键和命名按原兼容边界维护，不随文档清理重命名。

## 存储选择与迁移

DATABASE_URL在非development环境必需；已配置PostgreSQL连接失败或Schema不完整时返回错误，不能假定回退内存。仅development且未配置数据库时使用内存；Redis不可用可回退直连PostgreSQL。V2内存使用另受MND_ALLOW_MEMORY_MOBILE条件约束。依据internal/platform/api/router.go的buildStore与相关配置。

Docker首次初始化挂载至013；已有数据库不会自动应用新增SQL。精确迁移名称见[基线](CURRENT_BASELINE.md)，不再沿用旧文档中不存在的005_mobile_auth_identities.sql或006_identity_eligibility.sql。

## 按任务进入

| 要改什么 | 先读 | 对应源码 |
|---|---|---|
| V2业务字段／接口 | API_CATALOG、OpenAPI与代码逻辑手册 | internal/mobile、mobileapi、shared／Web派生合同 |
| 认证、年龄、监护 | 认证与安全合同 | internal/identity、资格模块和专业Store |
| UI页面或布局 | Figma原稿、跨端合同与UI组件规范 | clientUi及各宿主、web-client |
| 业务后台或运维 | 各自合同与设计交接 | web-client/src/operator、ops及具名服务端合同 |
| 数据库或Worker | DATA_DICTIONARY、迁移与运行手册 | postgres、mobileworker、jobs |
| 文档与图谱 | CURRENT_BASELINE、DOCUMENTATION_CATALOG | docs/uml与figma-sync |

本轮不运行上述入口。旧版详细目录与v1模块清单保存在Git历史；本页不再将过时路径、旧依赖版本或生产回退策略描述为现行事实。
