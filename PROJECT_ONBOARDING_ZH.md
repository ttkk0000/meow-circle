# bestTry 项目上手文档（客户端开发者视角）

这份文档是给“会看代码、但对 Go 后端项目不熟”的同学准备的。你可以把它当作从 0 到能改需求、能定位问题的路线图。

## 1. 项目一句话

`bestTry` 是一个宠物社区 + 交易服务项目：
- 后端：Go 提供 REST API（帖子、交易、订单、消息、举报、管理后台等）
- Web：原生 HTML/CSS/JS
- Mobile：Expo + React Native + TypeScript
- 数据层可切换：内存 / PostgreSQL / PostgreSQL + Redis 缓存

## 2. 技术栈总览

### 后端（Go）
- 语言/运行时：Go `1.25.0`（`go.mod`）
- HTTP：标准库 `net/http` + `http.ServeMux`（无 Gin/Echo）
- 鉴权：JWT（`internal/platform/auth`）
- 存储抽象：`internal/store.Store` 接口
- 持久化：PostgreSQL（`pgx/v5`）
- 缓存：Redis（`go-redis/v9`，读穿透缓存）
- 支付：Provider 抽象 + Mock/Alipay/Wechat/Stripe（真实通道当前以桩实现为主）
- 内容审核：关键词过滤（`internal/platform/audit`）

### 前端与客户端
- Web：`web/` 下的原生页面 + JS
- Mobile：`mobile/`，Expo Router 文件路由，TypeScript，SecureStore 保存 token

### 工程化
- 本地任务：`Makefile`
- 容器：`Dockerfile` + `docker-compose.yml`
- CI：`.github/workflows/ci.yml`（build/test/lint/docker/integration）

## 3. 目录结构（重点）

```text
cmd/server                     # 服务启动入口（main）
internal/domain                # 领域模型（User/Post/Listing/Order...）
internal/platform/api          # 路由、handler、中间件、统一响应
internal/platform/auth         # JWT、密码哈希、登录限流
internal/platform/payment      # 支付路由与 provider 抽象
internal/platform/audit        # 文本审核
internal/store                 # Store 接口 + 内存实现
internal/store/postgres        # PostgreSQL 实现
internal/store/cache           # Redis 读穿透缓存装饰器
migrations                     # SQL 迁移（初始化表结构）
web                            # Web 静态资源
mobile                         # Expo React Native 客户端
```

你可以先把 `internal/platform/api` 理解成“controller 层 + 一部分 service 层”。
这个项目目前没有单独 `service/` 目录，很多业务规则在 handler 内完成，然后调用 `store`。

## 4. 启动流程（最关键的主链路）

1. `cmd/server/main.go` 启动 HTTP 服务，读取 `APP_PORT`，默认 `8080`
2. 调用 `api.NewRouter()`
3. `NewRouter()` 中 `buildStore()` 按环境变量选择后端：
   - 无 `DATABASE_URL` -> 内存模式
   - 有 `DATABASE_URL` -> PostgreSQL
   - 同时有 `DATABASE_URL + REDIS_URL` -> PostgreSQL + Redis 缓存
4. 注册所有路由和中间件
5. 进入请求处理，支持优雅停机（SIGINT/SIGTERM）

## 5. 请求处理模型（客户端同学最关心）

统一响应包络（`envelope`）：
- 成功：`{"code":0,"message":"ok|created","data":...}`
- 失败：`{"code":httpStatus,"message":"..."}`

典型链路（例如发帖）：
1. 客户端请求 `POST /api/v1/posts`
2. handler 校验方法、鉴权、参数、审核
3. 调用 `r.store.CreatePost(...)`
4. 返回统一 JSON

鉴权规则：
- 使用 `Authorization: Bearer <token>`
- token 由 `/api/v1/auth/login` 或 `/api/v1/auth/register` 返回
- 管理端用 `X-Admin-Key`

## 6. API 版图（按业务）

### 认证与用户
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `PUT/PATCH /api/v1/me`（见实现）
- `GET /api/v1/users/{id}`

### 社区内容
- `GET/POST /api/v1/posts`
- `GET/DELETE /api/v1/posts/{id}`
- `POST /api/v1/posts/{id}/comments`
- 搜索：`GET /api/v1/search`

### 交易与订单
- `GET/POST /api/v1/listings`
- `GET/DELETE /api/v1/listings/{id}`
- `GET/POST /api/v1/orders`
- `GET/... /api/v1/orders/{id}/...`（支付、状态流转等看 `order.go`）
- `GET /api/v1/payments/methods`

### 消息与通知
- `POST /api/v1/messages`
- `GET /api/v1/me/conversations`
- `GET /api/v1/notifications`

### 媒体与风控
- `POST /api/v1/media`（multipart）
- 举报：`POST /api/v1/reports`

### 管理后台
- `/api/v1/admin/*`（summary/posts/comments/listings/media/reports/orders/audit-logs）

## 7. 数据与存储设计

### 领域模型
集中在 `internal/domain/models.go`，包括：
- 用户、帖子、评论
- 交易列表（listing）、订单
- 会话/消息、通知
- 媒体、举报、审计日志等

### 存储层策略
- `Store` 接口定义业务所需读写能力
- `memory` 实现用于本地开发、测试和降级
- `postgres` 实现用于生产持久化
- `cache` 实现是对 `postgres` 的装饰器（读穿透 + TTL + 失效）

### 一些当前约定/注意点
- 部分 `Store` 方法以 `bool` 表示成功与否，而不是返回 `error`
- 这会让 handler 里出现较多“判空/判 false”逻辑
- 如果后续做复杂可观测性，建议逐步统一成 `(..., error)` 风格

## 8. 配置与环境变量

常用环境变量：
- `APP_PORT`：服务端口（默认 8080）
- `DATABASE_URL`：PostgreSQL 连接串
- `REDIS_URL`：Redis 连接串
- `JWT_SECRET`：JWT 密钥（生产必须改）
- `ADMIN_KEY`：管理后台密钥（生产必须改）
- `CORS_ALLOW_ORIGIN`：跨域控制
- 支付相关：`ALIPAY_*`、`WECHAT_*`、`STRIPE_SECRET_KEY`

内置默认测试账号（服务启动自动确保存在）：
- 用户名：`demo`
- 密码：`123456`

移动端：
- `mobile/.env.example` 中 `EXPO_PUBLIC_API_URL`

## 9. 本地开发命令

```bash
# 启后端（内存模式）
make run

# 启数据库与缓存
make up

# 跑单测
make test

# 跑集成测试（需配置 DATABASE_URL）
make integration

# 启移动端 Expo
make mobile
```

## 10. 给你的推荐阅读顺序（客户端转后端）

1. `README.md`：先看全局心智图
2. `cmd/server/main.go`：看服务是怎么活起来的
3. `internal/platform/api/router.go`：看全路由和中间件
4. `internal/domain/models.go`：看核心数据结构
5. 挑一条完整业务线读（推荐订单）：
   - `internal/platform/api/order.go`
   - `internal/store/store.go`
   - `internal/store/memory.go`
   - `internal/store/postgres/*.go`
6. 再看横切能力：
   - 鉴权 `internal/platform/auth`
   - 缓存 `internal/store/cache`
   - 审核 `internal/platform/audit`

## 11. 你需要补的“相关知识栈”（按优先级）

如果你是客户端出身，建议按这个顺序补：

1. **Go HTTP 基础（必修）**
   - `net/http`、`ServeMux`、`http.Handler`
   - 请求体解码、响应编码、状态码语义

2. **Go 语言常见写法**
   - `context.Context` 传递
   - `struct + interface` 分层
   - 错误处理（`error`、`errors.Is`）
   - goroutine 与 channel（先会读，不急于重并发编程）

3. **REST API 设计**
   - 资源化路由
   - 统一响应格式
   - 分页、过滤、幂等、鉴权

4. **JWT 与会话安全**
   - token 签发/校验
   - 过期策略
   - 常见安全坑（弱密钥、泄露、重放）

5. **PostgreSQL + SQL 调优基础**
   - 建表、索引、事务
   - explain 基础
   - 常见查询性能瓶颈

6. **Redis 缓存策略**
   - read-through / cache-aside 概念
   - TTL、失效、缓存一致性

7. **容器与 CI 基础**
   - Docker 多阶段构建
   - GitHub Actions 流水线
   - 本地与 CI 环境差异排查

## 12. 常见上手坑（提前避雷）

- 默认 `JWT_SECRET` 和 `ADMIN_KEY` 只适合开发环境，生产必须覆盖
- 本地上传目录是 `data/uploads`，容器部署要考虑卷挂载或对象存储
- 登录限流是进程内实现，多实例部署下不共享
- 支付真实通道目前偏“可扩展骨架”，不要默认它已可直接生产接入

## 13. 你可以马上做的 3 个练习

1. **登录链路练习**：从 `POST /api/v1/auth/login` 追到 token 生成与校验
2. **发帖链路练习**：从 `POST /api/v1/posts` 追到 store 写入与返回
3. **模式切换练习**：分别在内存/PG/PG+Redis 下跑服务，观察 `/healthz` 返回的 `store`

做到这三步，你基本就能独立接小需求了。
