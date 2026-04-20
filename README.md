# Cat/Pet Share Server (Go + Caddy)

一个面向猫咪主人（并可扩展到宠物主人）的社区与交易服务端骨架：

- 日常分享发帖
- 评论互动
- 交易信息发布（商品/服务/领养）
- 响应式 Web 界面（手机可用）

存储层做了接口化拆分，**同一套 API 可在三种后端之间零代码切换**：

| 模式 | 触发条件 | 适用场景 |
| --- | --- | --- |
| In-memory | 未设置 `DATABASE_URL` | 本地开发、演示、单元测试 |
| PostgreSQL | 设置 `DATABASE_URL` | 生产持久化 |
| PostgreSQL + Redis 读穿透缓存 | 同时设置 `DATABASE_URL` 和 `REDIS_URL` | 高并发读多写少 |

## 1) 项目架构

```text
cmd/server                      # 程序启动入口
internal/domain                 # 领域模型（帖子/评论/交易 …）
internal/store                  # Store 接口 + 内存实现
internal/store/postgres         # pgx/v5 PostgreSQL 实现
internal/store/cache            # Redis 读穿透缓存装饰器
internal/platform/api           # HTTP API 路由与处理器
internal/platform/auth          # JWT、密码哈希、登录频率限制
internal/platform/payment       # 支付 Provider（Mock / Alipay / Wechat / Stripe）
internal/platform/audit         # 敏感词过滤
migrations/001_init.sql         # PostgreSQL 表结构
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
go run ./cmd/server
```

默认监听 `:8080`，可通过环境变量覆盖：

```bash
APP_PORT=8081 go run ./cmd/server
```

### 启动 Go 服务（PostgreSQL + Redis 模式）

首次使用先起依赖：

```bash
docker compose up -d   # 启动 Postgres + Redis，并自动执行 migrations/001_init.sql
```

然后带上连接串启动服务：

```powershell
$env:DATABASE_URL = "postgres://meow:meowpass@127.0.0.1:5432/meow?sslmode=disable"
$env:REDIS_URL    = "redis://127.0.0.1:6379/0"
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

- `http://localhost:8080`（直连 Go）
- 或 `http://localhost`（走 Caddy）
- 可视化系统文档：`http://localhost:8080/architecture.html`

页面包含：

- 帖子发布、列表、评论
- 交易发布、列表
- 后台管理：`/admin`（管理员密钥保护）

并采用响应式布局，后续接移动端时可直接复用现有 API。

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

## 5) 移动端接入建议

为了后续 iOS/Android 小程序/Flutter/React Native 接入，建议保持：

1. `/api/v1` 版本化不变
2. 统一 JSON 结构（成功/失败格式）
3. 鉴权统一为 Bearer Token（后续可加刷新令牌）
4. 媒体上传与帖子/交易解耦（对象存储 + 媒体服务）
