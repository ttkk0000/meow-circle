# M&D / meow & doggie Server (Go + Caddy)

AI 或换电脑继续开发，先读[2026-10-22家里接手入口](docs/ai/HOME_HANDOFF_2026_10_22.md)、[项目内 AI 工作跟踪](docs/ai/README.md)、[当前快照](docs/ai/CONTEXT.md)和[任务状态](docs/ai/TASKS.md)。本批包含此前未提交的设计、合规、服务端及移动端源码；远端同步以Git回执为准，家里核对提交后继续，旧`ebfc469`不包含这些成果。

M&D = meow & doggie。项目为猫猫优先、doggie 友好的社区服务端与多端客户端。

客户端实施入口：[架构与代码完成标准](docs/client/README.md)、[任务拆分](docs/client/TASKS.md)、[Codex / Gemini交接](docs/client/AGENT_HANDOFF.md)。Android / iOS 共用KMP逻辑，各自Compose UI与独立平板适配；Desktop延续Compose Desktop，Web规划独立TypeScript客户端。当前仅使用设计默认主题，旧多主题保留参考和扩展位置。[2026-10-22服务端／公共KMP批次](docs/server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md)已补身份资格、用途授权、操作核对与恢复等源码；完整功能、真实配置、UI接入和运行验收尚未完成。

当前产品方案（2026-10-15）以交流与领养为主，补充可信找猫：允许个人、商家和已披露代理发布售猫/有偿转让信息，重点解决身份冒用、虚假地点、隐瞒关系、换猫和隐藏费用。家庭饲养不作为可信前提。首轮信息与预约、不代收款是规划假设，完整新方案尚未在客户端交付，首批源码与当前缺口见客户端实施交接。

2026-10-20：新增对应手机设计的 `/api/v2` 服务源码，采用[模块化单体](docs/server/ARCHITECTURE.md)，保留以后拆分服务的接口与事务事件出口。登录、好友/圈子、私信群聊、寻宠、领养、预约披露、审核与私密媒体见[服务端交接](docs/server/MOBILE_SERVER.md)和[OpenAPI](docs/api/mobile-v2.openapi.json)。尚未运行编译、测试、迁移或供应商联调；客户端未改。

先看[整体流程图](docs/design/flows/index.html)和[文档入口](docs/README.md)。下文 v1 为历史兼容能力；生产默认关闭其写接口，旧交易 API 不代表新业务具有支付或担保能力。

已有基础包括：

- 日常分享发帖
- 评论互动
- 交易信息发布（商品/服务/领养）
- 响应式 Web 界面（手机可用）

存储层做了接口化拆分，**同一套 API 可在三种后端之间零代码切换**：

| 模式 | 触发条件 | 适用场景 |
| --- | --- | --- |
| In-memory | `APP_ENV=development` 且未设置 `DATABASE_URL` | 本地开发、演示、单元测试 |
| PostgreSQL | 设置 `DATABASE_URL` | 生产持久化 |
| PostgreSQL + Redis 读穿透缓存 | 同时设置 `DATABASE_URL` 和 `REDIS_URL` | 高并发读多写少 |

## 1) 项目架构

```text
cmd/server                      # 程序启动入口
internal/domain                 # 领域模型（帖子/评论/交易 …）
internal/mobile                 # 手机业务命令、权限、版本快照、Outbox
internal/identity               # 验证码、登录方式、资料、可撤销会话
internal/store                  # Store 接口 + 内存实现
internal/store/postgres         # pgx/v5 PostgreSQL 实现
internal/store/cache            # Redis 读穿透缓存装饰器
internal/platform/api           # HTTP API 路由与处理器
internal/platform/mobileapi     # /api/v2 DTO 与手机流程接口
internal/platform/mobileauth    # HTTPS SMS / 微信 / QQ / Apple 适配器
internal/platform/auth          # JWT、密码哈希、登录频率限制
internal/platform/payment       # 历史支付桩；仅显式开发开关注册 Mock
internal/platform/audit         # 敏感词过滤
migrations/001_init.sql         # PostgreSQL 表结构
migrations/007_mobile_modules.sql # 新手机业务表（需先应用 001–006）
docker-compose.yml              # 一键启动 PG + Redis
web                             # Web 界面（H5，适配移动端）
Caddyfile                       # Caddy 反向代理配置
```

设计原则：

- 传输层（HTTP）和业务模型分离
- 存储层抽离，后续可替换数据库实现
- 先保证 MVP 能跑，再逐步演进鉴权、搜索、推荐、订单等能力

## 2) 运行方式

### 启动 Go 服务（内存模式）

```bash
APP_ENV=development go run ./cmd/server
```

默认监听 `:8080`，可通过环境变量覆盖：

```bash
APP_ENV=development APP_PORT=8081 go run ./cmd/server
```

### 启动 Go 服务（PostgreSQL + Redis 模式）

首次使用先起依赖：

```bash
docker compose up -d   # 新数据卷执行 001–007；已有卷需手动补迁移
```

然后带上连接串启动服务：

```powershell
$env:DATABASE_URL = "postgres://meow:meowpass@127.0.0.1:5432/meow?sslmode=disable"
$env:REDIS_URL    = "redis://127.0.0.1:6379/0"
$env:APP_ENV      = "development"
go run .\cmd\server
```

启动日志里会看到：

```
store: connected to PostgreSQL
store: Redis read-through cache enabled
```

如果 PG 或 Redis 临时不可用，服务会**自动降级**：Redis 不通时直连 PG；PG 都不通时回到内存模式，保证开发环境体验。

手动应用 schema（例如用自己的 PG 实例）：

```bash
psql "$DATABASE_URL" -f migrations/001_init.sql
# 按序应用其余 002–007；迁移操作不由服务启动自动执行
```

### 缓存命中策略

`internal/store/cache` 只对读多写少的接口做缓存，默认 TTL 30 秒：

- `GET /api/v1/posts`（`posts:list`）
- `GET /api/v1/posts/{id}`（`post:{id}`）
- `GET /api/v1/listings`（`listings:list`）
- `GET /api/v1/listings/{id}`（`listing:{id}`）
- 未读通知计数（`notif:unread:{userID}`，事件驱动失效）

写入 / 删除会同步 evict 对应 key，因此读写一致性是"写后最多延迟一次失效"。个人维度（我的帖子、订单、会话）直接透传底层，避免跨用户数据窜扰。

### 启动 Caddy（参考 Getting Started）

在项目根目录执行：

```bash
caddy run
```

因为已经提供了 `Caddyfile`，Caddy 会自动加载并反向代理到 `127.0.0.1:8080`。

配置热更新可以用：

```bash
caddy reload
```

> Caddy 官方入门文档：<https://caddyserver.com/docs/getting-started>

### 访问 Web 界面

启动 Go 服务后，直接打开：

- `http://localhost:8080/`（首页 / 帖子 / 交易）
- `http://localhost:8080/login` （**登录独立页**）
- `http://localhost:8080/register`（**注册独立页**）
- `http://localhost:8080/dashboard`（个人管理中心，未登录自动跳 `/login?return_to=/dashboard`）
- `http://localhost:8080/admin` （管理员后台，`X-Admin-Key` 校验）
- `http://localhost:8080/architecture.html`（系统可视化文档）
- `http://localhost:8080/healthz` / `/readyz`（健康与就绪探针，返回当前 store 后端）

内置默认测试账号（服务启动自动确保存在）：

- 用户名：`demo`
- 密码：`123456`

当前视觉权威为 [Stitch V2](.stitch/DESIGN.md)和 [M&D UI 对齐指南](docs/design/MND_UI_ALIGNMENT_GUIDE.md)：

- Honey、Mint、Night、Neutral 四主题；Neutral 是正式主题。
- Outfit/Inter 与中文字体回退；消费者卡片 16px、Neutral 卡片 8px。
- 页面继承全局主题；新业务页面需映射现有组件。
- 历史页面与演示回退的实际行为以源码为准，不用早期 Cursor/crimson 规范覆盖当前设计。

登录、注册页已从首页抽离：未登录用户无需离开首页就能阅读帖子与交易，只有发帖、评论、下单、个人中心等操作才会带着 `return_to` 参数跳转到独立的登录页，登录完成后自动回到原位置。

### 后台管理系统

访问：

- `http://localhost:8080/admin`

默认管理员密钥是 `admin123`，可通过环境变量覆盖：

```bash
ADMIN_KEY=your-secret go run ./cmd/server
```

后台管理 API：

- `GET /api/v1/admin/summary`
- `GET /api/v1/admin/posts`
- `DELETE /api/v1/admin/posts/{id}`
- `GET /api/v1/admin/comments`
- `DELETE /api/v1/admin/comments/{id}`
- `GET /api/v1/admin/listings`
- `DELETE /api/v1/admin/listings/{id}`

### 可视化文档维护方式

本地可视化文档由以下文件组成：

- `web/architecture.html`（展示页）
- `web/architecture.css`（样式）
- `web/architecture.js`（渲染逻辑）
- `web/architecture-data.json`（文档数据源）

后续你只需要更新 `web/architecture-data.json`，页面内容就会自动同步，适合持续反映功能与架构演进。

## 3) API 设计（v1）

### 健康检查

- `GET /healthz`

### 帖子

- `GET /api/v1/posts` 列表
- `POST /api/v1/posts` 发帖
- `GET /api/v1/posts/{id}` 帖子详情（含评论）
- `POST /api/v1/posts/{id}/comments` 评论

示例：发帖

```json
{
  "author_id": 1001,
  "title": "我家橘猫今天学会握手",
  "content": "分享一个训练小技巧...",
  "category": "daily_share",
  "tags": ["橘猫", "训练"]
}
```

### 交易信息

- `GET /api/v1/listings` 列表
- `POST /api/v1/listings` 发布

示例：发布交易

```json
{
  "seller_id": 2001,
  "type": "product",
  "title": "猫砂盆 9 成新",
  "description": "同城自提优先",
  "price_cents": 8900,
  "currency": "CNY"
}
```

## 4) 后续建议（下一步改造）

1. ~~加入用户系统与鉴权（JWT/Session）~~ ✅
2. ~~抽象 Repository 接口并接入数据库~~ ✅（`internal/store.Store` + `postgres` + `cache`）
3. ~~增加媒体上传（图片/短视频）和内容审核~~ ✅
4. ~~增加交易流程（收藏、下单、私信、订单状态）~~ ✅
5. ~~增加运营能力（举报、置顶、推荐、标签体系）~~ ✅（举报、审计日志已完成）

下一阶段可以考虑的规模化演进：

- 把 pgx 查询改为带 `ctx = req.Context()` 并返回 `error`，以便接入链路追踪与超时控制
- `Search*` 接入 PostgreSQL 全文索引（`tsvector` + `pg_trgm`）或独立搜索引擎
- 把 Redis 缓存扩展到按标签 / 作者聚合的子列表，加上 LRU 热度统计
- 用 `golang-migrate` 管理 schema 迁移，CI 中自动 apply

## 5) 移动端（Expo + React Native）

仓库里的 `mobile/` 目录是一套 **Expo SDK 52 + React Native 0.76**（新架构）+ **TypeScript** + **Expo Router** 的跨端 App 脚手架。一套代码出 **iOS + Android + Web** 三端，直接消费当前的 Go API，从登录到发帖到私信基础流程都已经跑通。

```
mobile/
├── app/                # Expo Router — 文件即路由
│   ├── _layout.tsx     # 根 Stack + <AuthProvider>
│   ├── index.tsx       # 鉴权分流
│   ├── (auth)/         # 公开页：login / register
│   └── (tabs)/         # 登录后：社区 / 市场 / 消息 / 我的
└── src/
    ├── api.ts          # 类型化 fetch + SecureStore token
    ├── auth.tsx        # <AuthProvider> + useAuth()
    ├── components.tsx  # Screen / Card / Button / Input / Txt / Pill
    └── theme.ts        # M&D 主题 token；参考 Stitch V2 对齐指南
```

本地跑：

```bash
cd mobile
npm install
cp .env.example .env   # 把 EXPO_PUBLIC_API_URL 指到你本机后端
npm start              # 扫二维码即可在真机看到
```

为什么选 Expo 而不是 bare RN：**Windows 上也能云构 iOS**（EAS Build）、**OTA 热更新**（EAS Update）、**文件路由**（Expo Router）、**SecureStore**、**web 输出**同一份代码——这些都是 bare RN 需要自己拼的东西。完整说明见 [`mobile/README.md`](./mobile/README.md)。

移动端视觉按 M&D 四主题与 Stitch V2 对齐；当前实现和后续拟实施业务范围分别见源码及 PRD，不以本说明代替多端验证。

## 6) 开发、运维、CI

### Makefile 常用命令

```bash
make help          # 看所有目标
make run           # 起 Go 服务（内存模式）
make up            # docker compose 起 Postgres + Redis
make migrate       # psql 应用 migrations/001_init.sql
make test          # go test -race
make cover         # 覆盖率 + HTML 报告
make integration   # 集成测试（需 DATABASE_URL）
make lint          # golangci-lint v2
make docker        # 构建生产镜像
make mobile        # 起 Expo 开发服务
```

### Docker 镜像

`Dockerfile` 采用 **多阶段 + distroless/static:nonroot**：没有 shell、没有包管理器、非 root 启动，镜像 ~20 MB。`web/` 与 `migrations/` 都被打进镜像，容器工作目录即应用根。

### CI / CD (GitHub Actions)

`.github/workflows/ci.yml` 跑四个 job，全部进入 `main` 分支保护的必需检查列表：

| Job | 做什么 |
| --- | --- |
| **Build & Test** | `go vet` + `go build` + `go test -race` + 覆盖率上传为 artifact |
| **Lint** | `golangci-lint` v2（errcheck / govet / staticcheck / unused / misspell / unconvert + gofmt） |
| **Docker build** | buildx 构镜像 → `docker run -d` 启一次 → poll `/healthz` 验证能起来 |
| **Integration (Postgres + Redis)** | service container 起 postgres:16 + redis:7 → apply migrations → `go test -tags=integration` |

Dependabot (`.github/dependabot.yml`) 每周一早 9 点 (Asia/Shanghai) 扫描 Go module / GitHub Actions / Dockerfile / npm（mobile），自动开 PR 升级依赖。

### Cursor Agent（可随 Git 同步）

- **索引**：`.cursor/README.md`（规则 / 技能 / User Rules 分层说明）
- **项目规则（短、常驻上下文）**：`.cursor/rules/design-system.mdc`（token/组件/气质/文案格式）、`.cursor/rules/design-user-context.mdc`（仅用户心智与动效提要，避免与上重复）、`.cursor/rules/karpathy-guidelines.mdc`（Karpathy 四条提要；[上游](https://github.com/forrestchang/andrej-karpathy-skills)）——**Karpathy 与 design-user 的完整长文只在对应 SKILL，勿与 .mdc 双份维护**
- **技能（按需读，勿与规则同轮重复全文）**：`.cursor/skills/karpathy-guidelines/SKILL.md`、`.cursor/skills/kitty-circle-design-user-context/SKILL.md`
- **User Rules 粘贴块**（合并 Karpathy + kitty-circle）：`.cursor/USER_RULES_PASTE_DESIGN.md`；全局副本 `~/.cursor/USER_RULES_PASTE_DESIGN.md`
- 当前设计权威：`.stitch/DESIGN.md`；产品/设计阅读顺序见 `docs/README.md`。历史 Cursor 资料不作为新 UI 的视觉权威。

## 7) API 稳定性约定

1. `/api/v1` 版本号内保持兼容；有破坏性变更开 `/api/v2`
2. 统一 JSON 成功/失败结构：错误体 `{"code":<int>,"message":<string>,"retry_after":<int?>}`
3. 鉴权统一为 `Authorization: Bearer <token>`（JWT，HS256，默认 72h）
4. 登录错误 429 一律携带 `Retry-After` 响应头
5. 媒体上传走独立 multipart 端点，不内嵌在 JSON 里
