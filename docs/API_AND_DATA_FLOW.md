# M&D API与数据流

更新：2026-10-27。以[现行基线](CURRENT_BASELINE.md)、[产品交付矩阵](PRODUCT_AND_DELIVERY_MATRIX.md)和[文档目录](DOCUMENTATION_CATALOG.md)为准；本文仅记录源码与执行条件，不表示运行通过。

## 权威合同与响应

当前V2接口为[OpenAPI](api/mobile-v2.openapi.json)，共253路径／285操作／139 schemas；[API速查](API_CATALOG.md)仅作索引。Web派生141 schemas，跨语言显式映射87对；维护时按字段和语义核对，不按类数量推断兼容。

mobileapi.Handler成功返回ok=true及data；错误返回ok=false、error、error_code、request_id，HTTP状态与错误类别对应。401表示认证缺失，403表示权限或业务策略拒绝，409表示版本或幂等／身份冲突，429限流并返回Retry-After，503表示能力未配置或不可用。以handler.go为准，不能直接套用旧v1的code/message/data封套。

## 认证与权限链

| 来源 | 会话传输 | 写入与隔离 |
|---|---|---|
| Android／iOS／Desktop | 原生Bearer会话 | 服务端当前身份、资格、资源与版本校验 |
| Web消费端 | HttpOnly Cookie | Origin与X-MND-CSRF，浏览器适配器不接受客户端伪造身份 |
| 业务后台 | 具名凭据与角色 | 凭据仅当前页面内存，队列／详情权限和版本复核 |
| 技术运维／Worker | 具名环境与job权限 | 与消费者、业务后台分别持有合同；不得复用隐式角色 |

显式密码／手机／邮箱与oauth-start／finish／resolve以OpenAPI实际路径为准。三方登录、创建、绑定分开，identity_in_use不静默合并；短信不隐式注册。登录成功后仍读取eligibility，未知互动资格关闭。

## 通用写入数据流

1. 客户端保存草稿、原client_request_id与已读对象版本；敏感操作先请求具体scope。
2. auth/reauth核对scope与当前凭据，返回5分钟单次purpose_grant。
3. HTTP层读取Idempotency-Key并与正文原请求关联；领域服务在事务内核对当前身份、父资源、权限、资格、版本和用途。
4. pgx写事务使用SERIALIZABLE；Record.version成功递增，业务结果和mobile_operations原回执同事务持久化；必要通知／事件进入mobile_outbox。
5. 明确失败保留可恢复输入；传输中断的未知结果按operations原key回读，不换key自动重发。回执回放仍核对当前ACL，不能因历史参与身份泄露失权内容。

## 特殊主路径

| 路径 | 数据／版本规则 | 时序或状态说明 |
|---|---|---|
| 发布与来源声明 | 独立追加声明绑定实际内容与附件，不覆写旧记录 | SOURCE_DECLARATIONS专项与图07 |
| 预约 | effectiveVersion、pendingVersion及history分开；暂停同版重提后双方确认 | 图09 |
| 领养、试养与接回 | pet_id＋adoption_cycle排他；协议／试养／实际交接分别确认 | 图08 |
| 走失与私密线索 | 公开位置粗量化、坐标系明确；当前父权限与Lead版本重核 | 图10 |
| 全部已读 | POST准备10分钟快照，确认只推进已知范围；GET汇总不产生快照，无成功后撤销 | 图12 |
| 资料权利与清理 | 接受请求不等于清理完成；跨事项证据保全先核对 | 图11 |

## 真实存储与异步交付

[数据字典](DATA_DICTIONARY.md)列45张mobile表与公共封套。身份、资格、用途授权、幂等、消息快照都使用实际mobile聚合，不存在旧图推测的account_identities或mobile_inbox_snapshots等独立表。

Worker接受8个具名job：push、nearby_alerts、content_updates、privacy_cleanup、provider_revocations、maintenance、search_projection、audit_projection。检查点、租约、投递和外部真实回执分开。完整搜索接收端及运维执行器按用户要求保留占位；不能据作业名存在宣称已上线。

## 维护与历史边界

修改合同后核对服务端、OpenAPI、shared DTO与Web派生；[UML](uml/README.md)需保持实际对象、时限、路由和权限前提一致。旧v1交易、JWT及localStorage机制只适用于旧兼容页面，不自动构成V2支付或资格能力。旧版逐路由清单保留在Git历史，本轮以V2权威合同和静态速查代替过期复制表。
