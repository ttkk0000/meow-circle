# kitty-circle 项目上手文档（Android / 鸿蒙背景版）

这份文档假设你：

- 写过 Android（Java/Kotlin）或鸿蒙（ArkTS/Java），理解 Activity / Service / ContentProvider 这套生命周期与分层
- 会看 Go 代码（不用精通），能分辨 `func` / `struct` / `interface`
- 想尽快看懂**一个请求从 URL 落到数据库**的全链路，并能独立改小需求

全文按"先类比、再追踪、后补齐"的顺序来读，不想从头看就直接跳到 §3 映射表和 §5 端到端追踪。

---

## 0. 3 天速成表（推荐节奏）

如果你有完整 3 个半天，按这个表打穿项目，做完就能独立接小需求。做不完也别慌，至少把 Day 1 做掉就能读懂 80% 的代码。

| 天 | 时长 | 做什么 | 验收 |
| --- | --- | --- | --- |
| **Day 1 上午** | 1.5h | 读 §1~§4（全景 + 映射表 + 目录）；跑 `make run`，浏览器开 `http://localhost:8080/` 点一圈 | 能看懂首页 / 登录 / 发帖大致走哪些接口，抓包能看到 `/api/v1/*` |
| **Day 1 下午** | 2h | 读 §5 端到端追踪，边读边 Ctrl+G 跳文件行号；完成 §14 练习 1（登录 + `/me`） | 能徒手用 curl 走通登录 → 拿 token → 访问 `/me` 全流程 |
| **Day 2 上午** | 2h | 读 `internal/domain/models.go` + `internal/store/store.go` + `internal/store/memory.go` 前 200 行；完成 §14 练习 2（发帖 + 审核） | 看得懂 `Store` 接口 50+ 方法，能讲清"内存模式下发一条帖子到底操作了哪些 map" |
| **Day 2 下午** | 2h | 读 `order.go` 全文 + §6 订单状态机；用 curl 跑一遍下单 → 付款 → 发货 → 确认收货 | 画得出订单状态转换图（不看文档） |
| **Day 3 上午** | 2h | 读横切：`auth/jwt.go` + `password.go` + `ratelimit.go` + `store/cache/`；完成 §14 练习 3（三种后端切换） | 能解释 JWT 为什么是三段式、缓存 evict 时机、PG 挂掉为什么不影响开发 |
| **Day 3 下午** | 2h | 挑一个小需求真改：给 `Post` 加 `cover_url` 字段（domain + store + handler + migration + mobile/api.ts 全链改动） | PR 自测通过，`make test` 绿，两端能看到新字段 |

每天结束，对应章节的 §15 自检题能答出来就算过关。

---

## 1. 项目一句话

一个宠物社区 + 二手交易后端骨架：Go 提供 REST API，前端有**原生 HTML 网页**和 **Expo React Native App**，存储层做了接口化抽象，可在 **内存 / PostgreSQL / PostgreSQL+Redis 缓存** 三种后端零代码切换。

---

## 2. 技术栈全景

| 层 | 用了什么 | 对应 Android 里你熟悉的东西 |
| --- | --- | --- |
| 语言 | Go `1.25.0` | Kotlin / Java |
| HTTP 框架 | 标准库 `net/http` + `http.ServeMux`（**没用 Gin/Echo**） | 相当于裸写 `HttpServlet`，没上 Spring |
| 鉴权 | 自实现 JWT（HS256，HMAC-SHA256） | 等价于一个自签名的 Access Token |
| ORM | **没用 ORM**，`pgx/v5` 直接写 SQL | 你在安卓上用 Room，这里相当于手写 `SQLiteDatabase.rawQuery` |
| 缓存 | `go-redis/v9`，读穿透装饰器 | 类比 Retrofit 外挂一层 OkHttp `Cache` |
| 支付 | `Provider` 接口 + Mock/Alipay/Wechat/Stripe 桩 | 接口 + 多实现，你 Android 里常见 |
| 审核 | 纯关键词过滤（`internal/platform/audit`） | — |
| Web | 静态 HTML/CSS/JS（没有 React） | — |
| Mobile | Expo SDK 52 + React Native 0.76 + TypeScript + Expo Router | — |
| 部署 | Dockerfile（distroless + nonroot）、docker-compose、Caddy 反代 | — |

要点：**这是个"刻意不依赖框架"的项目**。看不到 `@RestController`、`@Autowired` 这种注解，所有接线都是手写的。

---

## 3. 安卓 / 鸿蒙 ↔ 本项目 映射表（重点）

### 运行时与工程化

| 你熟悉的 | 本项目对应 | 位置 |
| --- | --- | --- |
| `AndroidManifest.xml` 里的 `<activity>` / `<service>` 注册 | `router.go` 里 `r.mux.HandleFunc(...)` 的路由注册 | `internal/platform/api/router.go:198-282` |
| `Application.onCreate()` | `cmd/server/main.go` 的 `main()` | `cmd/server/main.go:20` |
| `gradle.build` + 依赖 | `go.mod` | 项目根 |
| `Gradle tasks` | `Makefile` 目标（`make run/test/up/...`） | 项目根 |
| `proguard-rules.pro` | 没有（Go 编译后是原生二进制） | — |
| SharedPreferences / 配置 | 纯环境变量（`os.Getenv`） | 散落各处，见 §8 |
| `build.gradle` signingConfig | JWT 密钥 `JWT_SECRET`、管理员 `ADMIN_KEY` | 环境变量 |

### 业务分层

| 你熟悉的 | 本项目对应 | 说明 |
| --- | --- | --- |
| `Retrofit interface` | `store.Store` 接口 | `internal/store/store.go`，86 行一口气列了 50+ 方法 |
| `Retrofit` 的实际 `OkHttpClient` 实现 | `MemoryStore` / `postgres.Store` / `cache.Store` | 三种实现都满足同一个接口，启动时挑一个 |
| `Room @Entity` | `internal/domain/models.go` 里的 struct | User / Post / Listing / Order 等 |
| `Room @Dao` | `internal/store/postgres/*.go`（按表分文件） | 每个文件相当于一个 DAO |
| `Room @Database.migration` | `migrations/001_init.sql` | 手写 SQL，启动时不自动 apply，需要 `make migrate` |
| `ViewModel` + 业务逻辑 | **没有独立 service 层**，业务规则直接写在 handler 里 | 所以 `router.go` 的 handler 看起来有点长 |
| `OkHttp Interceptor` | `requireAuth` 中间件 | `router.go:804-826`，函数式中间件包一层 `HandlerFunc` |
| `Intent.getStringExtra(...)` | `json.Decoder(req.Body).Decode(&payload)` | 请求体里的 JSON，`decodeJSON` 辅助见 `router.go:908` |
| `startActivityForResult` 的返回值 | 统一 envelope：`{"code":0,"message":"ok","data":...}` | 见 `router.go:963-985` |
| Android Log.d | `log.Printf` | 没有 logback/timber，全项目都是 stdlib log |
| `WorkManager` 异步任务 | 没有。handler 全部同步完成，写库 → 发通知都在一个请求里 | — |

### 客户端 ↔ 服务端契约

| 客户端调用 | 服务端入口 |
| --- | --- |
| `mobile/src/api.ts` 里 `api.login(...)` | `POST /api/v1/auth/login` → `router.go:348` |
| `api.listPosts()` | `GET /api/v1/posts` → `router.go:406` |
| `api.createPost(...)` | `POST /api/v1/posts` → `router.go:418` |
| `api.sendMessage(...)` | `POST /api/v1/messages` → `message.go` |
| web 端 `login.html` 里 `fetch('/api/v1/auth/login', ...)` | 同上 |

移动端 token 存在 `expo-secure-store`（相当于 Android KeyStore 支撑的 `EncryptedSharedPreferences`），见 `mobile/src/auth.tsx`。

---

## 4. 目录结构（带"先看哪个"标注）

```text
cmd/server/main.go              # ★ 入口，先看这里
internal/domain/models.go       # ★★ 全部领域模型，相当于 Room Entity 合集
internal/platform/api/          # ★★★ Controller + 一部分 service 层
  router.go                     #   路由注册 + auth/posts/listings/admin handler
  order.go                      #   订单状态机（pay/ship/complete/refund/cancel）
  media.go                      #   multipart 上传 + 审核
  message.go                    #   私信
  notification.go               #   通知读/未读
  report.go                     #   举报
  search.go                     #   关键词搜索
  audit.go                      #   管理端审计日志
  profile.go                    #   /me 读写
internal/platform/auth/         # JWT / 密码哈希 / 登录限流
internal/platform/payment/      # 支付 Provider 抽象 + 具体桩实现
internal/platform/audit/        # 关键词审核
internal/store/                 # ★★ Store 接口
  store.go                      #   接口定义（看这个就知道后端能干嘛）
  memory.go                     #   内存实现（开发默认）
  postgres/                     #   pgx/v5 的 SQL 实现（10 个文件）
  cache/                        #   Redis 读穿透装饰器
migrations/001_init.sql         # 表结构
web/                            # 原生 H5 页面
mobile/                         # Expo + RN App
Caddyfile / docker-compose.yml / Dockerfile / Makefile
.github/workflows/ci.yml        # 四个 job：build+test / lint / docker / integration
```

---

## 5. 端到端追踪：从登录到发帖

这是**整个文档最有用的一节**，读完基本能独立改任何小需求。所有行号对应当前仓库状态，便于你直接 Ctrl+G 跳。

### 5.1 服务怎么活起来

1. `cmd/server/main.go:20` — `main()` 读 `APP_PORT`、`MAX_OPEN_CONNS`
2. `main.go:28` — `api.NewRouter()` 返回 `http.Handler`
3. `router.go:52` — `NewRouter()` 里：
   - `buildStore()` 按环境变量挑一个后端（§7 再展开）
   - `ensureDefaultUser()` 确保 `demo / 123456` 这个测试账号存在（首次启动用）
   - `auth.NewTokenService(secret, 72h)` 创建 JWT 签发器
   - `audit.NewFilter(...)` 装好审核
   - `auth.NewLoginLimiter(5, 10m, 10m)` — 每账号+IP 10 分钟内最多 5 次失败
   - 调 `r.routes()` 批量 `HandleFunc`
4. `main.go:26-50` — 包一个 `http.Server`，设了读写超时 + 最大连接数守卫
5. `main.go:54-80` — 注册 SIGINT/SIGTERM，10 秒优雅停机

### 5.2 登录链路：客户端点"登录"到拿到 token

**请求**：`POST /api/v1/auth/login`，body `{"username":"demo","password":"123456"}`

1. `router.go:236` — `/api/v1/auth/login` → `r.handleLogin`
2. `router.go:348` — `handleLogin`：
   - `decodeJSON(req, &payload)` 解析 body（`router.go:908`，禁止未知字段 + 自动 close body）
   - `clientIP(req)` 拼 `limiterKey = username + "|" + ip`
   - `r.loginLimit.Allow(key)` 问限流器（`internal/platform/auth/ratelimit.go`），超限直接 429，并设 `Retry-After`
3. `r.store.FindUserByUsername(username)`：
   - 如果是内存后端：`internal/store/memory.go` 里的 map 查一下
   - 如果是 Postgres：`internal/store/postgres/users.go` 执行 `SELECT ... WHERE lower(username) = lower($1)`
4. `auth.VerifyPassword(pwd, hash, salt)` — `internal/platform/auth/password.go`，**PBKDF2-SHA256 / 100 000 轮 / 16B 盐 / 32B 密钥**；用 `subtle.ConstantTimeCompare` 做定时比较防侧信道
5. `r.tokens.Issue(user.ID, user.Username)`：
   - `internal/platform/auth/jwt.go:29`
   - 构造 header `{"alg":"HS256","typ":"JWT"}` + payload（Claims）
   - Base64URL + HMAC-SHA256 拼出 `header.payload.sig`
   - **注意**：这不是用 `github.com/golang-jwt/jwt`，是**手写的**，别去搜库 API
6. 成功：`writeOK(w, {token, user})` → 返回 `{"code":0,"message":"ok","data":{"token":"...","user":{...}}}`

### 5.3 带 token 发帖：客户端保存 token 后调发帖

**请求**：`POST /api/v1/posts`，header `Authorization: Bearer <token>`，body `{title, content, category, tags, media_ids}`

1. `router.go:250` — `/api/v1/posts` → `r.handlePosts`
2. `router.go:406` — `handlePosts`，switch 到 POST 分支
3. `router.go:419` — `r.parseAuth(req)` 读 `Authorization` 头、切掉 `Bearer` 前缀、走 `tokens.Parse()`（验签 + 验过期）→ 得到 `Claims`
4. 解 JSON → 校验 title/content → `r.filter.Check(title+" "+content)` 过审核
5. `r.validateMediaOwnership(...)` — `router.go:932`，检查传入的 media_ids 都属于当前用户且不是 `rejected` 状态
6. `r.store.CreatePost(domain.Post{...})`：
   - Postgres：`internal/store/postgres/posts.go:35` — `INSERT INTO posts (...) RETURNING ...`
   - 缓存装饰器：同步 evict `posts:list` 键
7. `writeCreated(w, post)` → HTTP 201，封装 envelope

### 5.4 requireAuth 中间件长啥样（Kotlin 类比）

```go
// router.go:804
func (r *Router) requireAuth(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, req *http.Request) {
        header := req.Header.Get("Authorization")
        if !strings.HasPrefix(header, "Bearer ") {
            writeError(w, http.StatusUnauthorized, "missing bearer token")
            return
        }
        token := strings.TrimPrefix(header, "Bearer ")
        claims, err := r.tokens.Parse(token)
        if err != nil { ... }
        user, ok := r.store.GetUser(claims.UserID)
        if !ok { ... }
        // 把 user 挂到 request context，下游 handler 用 currentUser(req) 取
        ctx := context.WithValue(req.Context(), contextKeyUser, userContext{...})
        next.ServeHTTP(w, req.WithContext(ctx))
    }
}
```

Kotlin 的 OkHttp Interceptor 基本就是这套路，只是用闭包代替匿名类。

---

## 6. API 版图（按业务）

所有路径前缀 `/api/v1`。有 `requireAuth` 标记的需要 Bearer token，有 `adminKey` 的需要 `X-Admin-Key` 请求头。

### 认证 / 个人

- `POST /auth/register`、`POST /auth/login`
- `GET /auth/me`（auth）
- `PUT /me`、`PATCH /me`（auth，更新昵称/头像/简介）
- `GET /users/{id}`（公开资料）

### 社区

- `GET /posts`、`POST /posts`（auth）
- `GET /posts/{id}`、`DELETE /posts/{id}`（auth，作者本人）
- `POST /posts/{id}/comments`（auth）
- `GET /search?q=...`

### 交易 / 订单（**状态机重点**）

订单状态机长这样（参与方写在箭头上）：

```text
              买家POST /orders
                    │
                    ▼
         ┌────────────────────┐
         │  pending_payment   │
         └─────────┬──────────┘
       买家/pay│          │买家/cancel
                │          ▼
                │   ┌──────────┐
                │   │cancelled │(终态)
                │   └──────────┘
                ▼
         ┌──────────┐
         │   paid   │───────┐
         └────┬─────┘       │
     卖家/ship│     卖家/refund
             ▼               │
       ┌─────────┐           │
       │shipped  │─卖家/refund┤
       └────┬────┘           │
  买家/complete                │
             ▼               ▼
       ┌───────────┐   ┌──────────┐
       │ completed │   │ refunded │
       └───────────┘   └──────────┘
         (终态)             (终态)
```

对应接口：

- `GET /listings`、`POST /listings`（auth）、`GET/DELETE /listings/{id}`
- `POST /orders`（auth，买家下单）
- `GET /orders/{id}`（auth，买家或卖家）
- `POST /orders/{id}/pay`（买家，`pending_payment` → `paid`）
- `POST /orders/{id}/cancel`（买家，仅 `pending_payment` 时）
- `POST /orders/{id}/ship`（卖家，`paid` → `shipped`）
- `POST /orders/{id}/complete`（买家，`shipped` → `completed`）
- `POST /orders/{id}/refund`（卖家，`paid|shipped` → `refunded`）
- `GET /me/orders?role=buyer|seller`
- `GET /payments/methods`

### 私信 / 通知

- `POST /messages`、`GET /me/conversations`、`GET /me/conversations/{peerID}`
- `GET /notifications`、`POST /notifications/{id}/read`、`POST /notifications/read-all`、`GET /notifications/unread-count`

### 媒体 / 举报

- `POST /media`（multipart，auth）
- `GET /media/{id}`（公开，带 rate-limit 的 status 查询）
- `POST /reports`（auth）

### 管理后台（`X-Admin-Key`）

- `/admin/summary`、`/admin/posts[/{id}]`、`/admin/comments[/{id}]`、`/admin/listings[/{id}]`、`/admin/media[/{id}]`、`/admin/reports[/{id}]`、`/admin/orders`、`/admin/audit-logs`

### 健康 / 静态

- `GET /healthz`、`GET /readyz` — `{status, store}`，`store` 字段告诉你当前后端是 memory / postgres / postgres+redis
- `GET /`、`/login`、`/register`、`/dashboard`、`/admin`、`/architecture.html` — 静态页面，从 `web/` 目录服务

---

## 7. 存储分层：为什么这样设计

```text
          handler (router.go, order.go, ...)
                     │
            r.store (store.Store interface)
           ┌─────────┴──────────┐
           │                    │
       MemoryStore       cache.Store ──── Redis
                              │
                          postgres.Store ──── PostgreSQL (pgx/v5)
```

几个关键事实：

- **整个项目只认 `store.Store` 这个接口**（`internal/store/store.go`）。handler 代码不知道底下是内存还是 PG
- **切换由 `buildStore()` 在启动时决定**（`router.go:97-128`）：
  - 没 `DATABASE_URL` → 内存（方便本地 demo）
  - 有 `DATABASE_URL` → Postgres
  - 再加 `REDIS_URL` → Postgres 外包一层读穿透缓存装饰器
- **失败降级**：PG 连不上自动回落内存，Redis 连不上自动回落直连 PG —— 开发体验优先
- **缓存粒度谨慎**：只缓存 `posts:list` / `post:{id}` / `listings:list` / `listing:{id}` / `notif:unread:{userID}`，TTL 30s，写操作同步 evict。**跨用户维度才缓存**，"我的帖子 / 订单 / 会话"直接透传避免串号

### 当前约定中需要留神的点

> 这几条要提前知道，不然读代码会困惑。

1. **`Store` 接口大量使用 `(T, bool)` 返回值，而不是 `(T, error)`**。Go 里这是"查不到就 false"的惯用法，等价于 Kotlin 的 `T?` nullable。Postgres 实现内部遇到真错误只打 `log.Printf("xxx", err)` + 返回零值/false —— 目前还没法把真错误往上透传。
2. **没有事务边界**。handler 里连续多次 `store` 调用各走各的事务。如果你要加"下单 + 扣库存"这类强一致场景，当前接口撑不住，需要先扩接口。
3. **Context 几乎没传下去**。`postgres/*.go` 里都是 `ctx, cancel := bg()`（见 `postgres.go`），内部用的是 5 秒超时的背景 context，没拿请求 ctx。做链路追踪/请求级取消时得补。
4. **所有 ID 是 `int64` 自增**。没有 UUID，也没有软删。

---

## 8. 配置与环境变量

| 变量 | 默认值 | 作用 |
| --- | --- | --- |
| `APP_PORT` | `8080` | 监听端口 |
| `MAX_OPEN_CONNS` | `500` | 单实例最大连接（到达拒绝新连接） |
| `DATABASE_URL` | 空 | PostgreSQL DSN，空就走内存 |
| `REDIS_URL` | 空 | Redis DSN，空就不上缓存装饰器 |
| `JWT_SECRET` | `change-me-in-production` | **生产必须覆盖** |
| `ADMIN_KEY` | `admin123` | 管理后台密钥，**生产必须覆盖** |
| `CORS_ALLOW_ORIGIN` | `*` | CORS 允许的 Origin |
| `ALIPAY_APP_ID` / `ALIPAY_PRIVATE_KEY` / `ALIPAY_GATEWAY` | 空 | 有则启用支付宝 provider（桩实现） |
| `WECHAT_MCH_ID` / `WECHAT_API_KEY` | 空 | 微信支付（桩实现） |
| `STRIPE_SECRET_KEY` | 空 | Stripe（桩实现） |

内置测试账号（启动自动确保存在）：

- 用户名 `demo`，密码 `123456`

Mobile 端只有一个：

- `mobile/.env.example` 里的 `EXPO_PUBLIC_API_URL`，指向你本机后端地址

---

## 9. 本地开发命令

```bash
make help          # 列出所有目标
make run           # 启 Go 服务（内存模式）
make up            # docker compose 起 Postgres + Redis（自动 apply migrations）
make migrate       # 手动 psql 应用 001_init.sql
make test          # go test -race ./...
make cover         # 覆盖率 + HTML 报告
make integration   # 跑集成测（要 DATABASE_URL）
make lint          # golangci-lint v2
make docker        # 构建生产镜像
make mobile        # 起 Expo 开发服务
```

Windows 下 `make` 需要自己装（Chocolatey `choco install make` 最省心），或者直接用底层命令 —— 打开 `Makefile` 抄一行。

---

## 10. 移动端（客户端背景最快收益）

`mobile/` 是一套独立 Expo RN 项目，代码量不大：

```text
mobile/app/
  _layout.tsx       # 根 Stack + <AuthProvider>
  index.tsx         # 启动分流：已登录进 tabs，未登录进 auth
  (auth)/           # login / register 两个公开页
  (tabs)/           # 社区 / 市场 / 消息 / 我的
mobile/src/
  api.ts            # ★ 类型化 fetch 客户端，所有 API 调用都在这里
  auth.tsx          # ★ <AuthProvider> + useAuth()，token 存在 expo-secure-store
  components.tsx    # Screen / Card / Button / Input / Txt / Pill
  theme.ts          # Cursor 设计系统 token（和 web 同源）
```

上手路径：

1. 先读 `mobile/src/api.ts` —— 这是**最接近你经验的代码**（相当于一个超轻量 Retrofit）
2. 顺着 `api.login` / `api.createPost` 跳到后端对应 handler，对照验证契约
3. 改一个小需求：加一个字段、加一个新接口，两端同改

跨平台能力：EAS Build（Windows 上云构 iOS）、EAS Update（OTA 热更新）、Expo Router（文件路由）、SecureStore、Web 输出。对应 Android 你原本需要 Play Store + Firebase Remote Config + 手写路由 + KeyStore 的一整套。

---

## 11. 给你的推荐阅读顺序

按这个顺序，你大约能在半天内打通主线：

1. **`README.md`** — 全局心智图（5 分钟）
2. **`internal/domain/models.go`** — 所有领域模型，221 行看完就知道项目在管什么数据
3. **`internal/store/store.go`** — 接口定义，86 行看完就知道项目能做什么操作
4. **`cmd/server/main.go`** + **`internal/platform/api/router.go`** 前 200 行 — 服务启动 + 路由表
5. **一条完整业务线（推荐订单）**：
   - `internal/platform/api/order.go`（状态机，256 行，跟安卓里的 Activity state 机很像）
   - `internal/store/store.go` 里 `CreateOrder / UpdateOrder / ...`
   - `internal/store/memory.go`（看内存实现是怎么 mock 的）
   - `internal/store/postgres/orders.go`（看 SQL 版本）
6. **横切能力**：
   - `internal/platform/auth/jwt.go` + `password.go` + `ratelimit.go`
   - `internal/store/cache/`（缓存装饰器怎么包 PG）
   - `internal/platform/audit/` （关键词过滤，不到 50 行）
7. **mobile/src/api.ts** — 从客户端视角反向验证一遍契约

---

## 12. Go 语法陷阱（只讲这个项目会踩的）

> 不是教你学 Go，只说你读这套代码会被卡住的那几处。

### 1. 多返回值的 "ok 惯用法"

```go
user, ok := r.store.FindUserByUsername("demo")
if !ok { /* 没找到 */ }
```

相当于 Kotlin `val user = ... ?: return`。注意项目里**很少返回 error**，全部用这种 bool。

### 2. 指针接收器 vs 值接收器

```go
func (r *Router) handleLogin(...)   // 指针，可改 r 内部字段
func (s *TokenService) Issue(...)    // 指针
```

项目里 handler 全是指针接收器，别被 `*Router` 吓到。

### 3. interface 自动满足

`MemoryStore` / `postgres.Store` / `cache.Store` 都不写 `implements Store`。只要方法签名全对，就自动满足。编译器通过 `var _ store.Store = (*MemoryStore)(nil)` 这类断言做编译期校验（搜一下仓库能看到）。

### 4. context 是个值，显式传

没有 Kotlin 的 `CoroutineScope` / `coroutineContext` 隐式继承。这个项目 handler 里**几乎没把 `req.Context()` 往 store 里传**，是已知债务（见 §7）。

### 5. `defer`

类似 Kotlin `use { }` / Java try-with-resources，但更灵活，可以 defer 任何语句：

```go
rows, err := s.pool.Query(ctx, sql, args...)
defer rows.Close()
```

### 6. `any` = `interface{}`

你会看到 `map[string]any` 大量用来拼 JSON，就当它是 `Map<String, Any?>`。

### 7. struct tag

```go
Username string `json:"username"`
```

反射序列化用的，类比 Kotlin `@SerialName("username")`。

### 8. 包内大小写即可见性

大写开头（`CreatePost`）= public，小写（`handleLogin`）= 包内。没有 `private` / `public` 关键字。

---

## 13. 常见上手坑

- **默认 `JWT_SECRET` 和 `ADMIN_KEY` 只能开发用**，生产必须覆盖
- **登录限流是进程内的**，多实例部署下不共享（要上 Redis 版限流器自己加）
- **上传目录默认 `data/uploads`**，容器里必须挂卷或者换对象存储，不然重启数据丢失
- **支付三个真实通道都是桩实现**，能走通签名 / 回调格式，但别默认它已经能收钱
- **`ListPosts()` 在 PG 里没有 LIMIT**（`postgres/posts.go:55`），数据量一大要自己加分页
- **JSON body 默认上限 1 MiB**（`router.go:38`），`/api/v1/media` 上传接口走自己的 multipart 逻辑
- **审核只有关键词过滤**（`internal/platform/audit`），真要上线得接外部内容安全服务

---

## 14. 三个立刻能做的练习（练完你就能接小需求）

### 练习 1：登录链路

目标：手动用 curl / Postman 登录，抓到 token，再带 token 访问 `/api/v1/auth/me`。

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"123456"}'
```

验收：拿到的 token 能解析出 `uid/usr/exp`（Base64 解 payload 就行），再带上访问 `/auth/me` 能拿到同一个用户。

### 练习 2：发帖链路

目标：带 token 发一条帖子，从日志看到 `POST /api/v1/posts 201`，在 `/api/v1/posts` 列表里能看到。

扩展：故意把 content 设成含审核关键词的内容，观察返回 400。

### 练习 3：后端切换

目标：分别在三种模式下启动服务，观察 `/healthz` 返回的 `store` 字段。

- 默认启：`make run` → `{"store":"memory"}`
- 起 compose：`make up` + 带 `DATABASE_URL` → `{"store":"postgres"}`
- 再加 `REDIS_URL` → `{"store":"postgres+redis"}`

验收：同一份发帖流程在三种后端上行为一致（进程重启后，memory 数据丢失，PG 持久化）。

做完这三步，`make integration` 也应该能跑通，你就可以独立在这个项目里加小需求了。

---

## 15. 自检题（每章一道，答不上来就回头重读）

> 不抄代码、不打开 IDE，能用自己的话说清楚才算过。

1. **§2 技术栈**：为什么这个项目**没上 Gin/Echo、没上 GORM**？换成 Gin + GORM 会丢掉什么？（提示：看 §7 的三后端切换）
2. **§3 映射表**：`Room @Dao` 在这里对应哪个目录？Room 的 `@Entity` 对应哪个文件？为什么后端**没有** `ViewModel` 这一层？
3. **§4 目录**：`internal/` 前缀在 Go 里的特殊含义是什么？为什么 handler 都放在 `internal/platform/api/` 而不是 `pkg/api/`？
4. **§5 登录链路**：`handleLogin` 里为什么**不是一登录失败就返回 401**，而是先问 `loginLimit.Allow`？`limiterKey` 拼 `username + ip` 的原因是什么？
5. **§5 发帖链路**：为什么 `validateMediaOwnership` 要塞在 handler 里而不是 store 里？如果放 store 里会出什么问题？
6. **§6 订单状态机**：`refunded` 能从哪几个状态进来？**买家能不能主动退款**？为什么？
7. **§7 存储**：`cache.Store` 是用继承还是组合包装 `postgres.Store` 的？写入时缓存 evict 和 SQL 提交谁先谁后，有什么风险？
8. **§8 配置**：本地跑 `make run`，`/healthz` 返回的 `store` 字段会是什么？怎么让它变成 `postgres`？
9. **§10 移动端**：mobile 端 token 为什么**不用 `AsyncStorage`** 而用 `expo-secure-store`？Android 上这个 API 底下是什么？
10. **§12 Go 陷阱**：`var _ store.Store = (*MemoryStore)(nil)` 这行代码什么都不执行，那它存在的意义是啥？删掉会发生什么？
11. **§13 上手坑**：多实例部署（比如 2 个 Go 进程挂 Caddy 负载均衡），当前项目最先坏的是哪个功能？为什么？

> 答案不在这里。答不上来的题回到对应章节找线索，比直接告诉你答案学得更快。

---

## 16. 快速参考速查表

贴出来放工位上用。

### 16.1 常用 curl（Windows PowerShell 可直接粘）

```bash
# 1) 登录，拿 token（把 token 存到 shell 变量 $TOKEN）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"123456"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo $TOKEN

# 2) 查自己的资料
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/auth/me

# 3) 发帖
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"hello","content":"world","category":"daily_share","tags":["猫"]}'

# 4) 列表
curl 'http://localhost:8080/api/v1/posts?page=1&page_size=10' | jq

# 5) 管理员看审计日志
curl -H 'X-Admin-Key: admin123' http://localhost:8080/api/v1/admin/audit-logs | jq
```

PowerShell 里把 `$TOKEN` 换成 `$env:TOKEN`，`$(...)` 换成 `$(...)` 或 `Invoke-RestMethod`。最懒的做法是装 Git Bash。

### 16.2 手动解 JWT（不需要装库）

Token 是 `header.payload.signature`，中间那段是 Base64URL 的 JSON。

```bash
# Linux / macOS / Git Bash
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq
# → {"uid":1,"usr":"demo","iat":1700000000,"exp":1700259200}
```

PowerShell 版：

```powershell
$parts = $TOKEN.Split('.')
$pad = 4 - ($parts[1].Length % 4); if ($pad -lt 4) { $parts[1] += '=' * $pad }
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($parts[1].Replace('-','+').Replace('_','/')))
```

### 16.3 psql 常用（先 `make up` 起容器）

```bash
# 进 Postgres
docker compose exec postgres psql -U meow -d meow

# 看表
\dt

# 看 posts 表结构
\d posts

# 最近 10 条帖子
SELECT id, author_id, title, created_at FROM posts ORDER BY id DESC LIMIT 10;

# 清所有数据但保留表结构
TRUNCATE TABLE users, posts, comments, listings, orders, media, reports, messages, notifications, audit_logs RESTART IDENTITY CASCADE;
```

### 16.4 看日志快速定位

服务端访问日志格式（`router.go:174`）：

```text
POST /api/v1/posts 201 183B 1.234ms ip=127.0.0.1
```

排查顺序：

| 症状 | 先看什么 |
| --- | --- |
| 启动就退出 | 终端首屏日志，找 `fatal` / `panic` |
| 登录不了 | 看有没有 `429` + `Retry-After`，或 `invalid username or password` |
| store 不对 | `curl /healthz` 看 `store` 字段；启动日志里的 `store: ...` |
| PG 连不上 | 启动日志 `store: postgres unavailable (...) falling back to in-memory` |
| Redis 连不上 | 启动日志 `store: redis unavailable (...) serving direct Postgres` |
| 中文乱码 | Windows 终端 chcp 65001 切 UTF-8，或换 Windows Terminal |
| 端口占用 | `netstat -ano \| findstr :8080`（Win）/ `lsof -i :8080`（Unix） |

### 16.5 跳转记忆表（Ctrl+G 常用行号）

| 目的 | 文件:行号 |
| --- | --- |
| 看所有路由 | `internal/platform/api/router.go:198` |
| 看三后端怎么挑 | `internal/platform/api/router.go:97` |
| 看 JWT 签发 | `internal/platform/auth/jwt.go:29` |
| 看密码哈希 | `internal/platform/auth/password.go:17` |
| 看限流逻辑 | `internal/platform/auth/ratelimit.go:53` |
| 看订单状态机 | `internal/platform/api/order.go:74` |
| 看缓存 key 约定 | `internal/store/cache/cache.go` |
| 看 DB schema | `migrations/001_init.sql` |
| 看 mobile 调 API | `mobile/src/api.ts` |
| 看统一响应 envelope | `internal/platform/api/router.go:963` |
