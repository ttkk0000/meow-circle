# M&D V2 数据字典

更新：2026-10-27。以[Record与Kind](../internal/mobile/repository.go)、[迁移](../migrations/)及[现行基线](CURRENT_BASELINE.md)为准。以下45张mobile表使用共同关系封套，业务字段由各自Go结构和OpenAPI定义；JSONB字段并非独立SQL列。mnd_ops_records、mnd_worker_checkpoints及旧端users等表另见迁移，不计入45。

## 共用封套

| SQL列 | 类型 | 含义／约束 |
|---|---|---|
| id | BIGINT | 主键，共享mobile_id_seq |
| owner_id | BIGINT | 可空，引用users(id)，删除受限 |
| participants | BIGINT[] | 领域参与方；不是数组外键 |
| record_key | TEXT | 非空业务键具唯一索引，各聚合定义语义 |
| reference_id | BIGINT | 父对象／业务引用，按领域语义校验 |
| media_ids | BIGINT[] | 媒体引用与当前访问权限分别核对 |
| public | BOOLEAN | 是否公开，不能代替细粒度ACL |
| state | TEXT | 每个领域自己的状态集合 |
| version | BIGINT | 正数CAS版本，成功写入递增 |
| created_at / updated_at | TIMESTAMPTZ | 创建／更新时间 |
| data | JSONB | 对象类型业务内容；不信任客户端自造状态 |

## 聚合与实际表

| Go Kind | 实际SQL表 | SQL来源 |
|---|---|---|
| `Accounts` | `mobile_accounts` | 007_mobile_modules |
| `Sessions` | `mobile_sessions` | 007_mobile_modules |
| `Identities` | `mobile_identities` | 007_mobile_modules |
| `AuthChallenges` | `mobile_auth_challenges` | 007_mobile_modules |
| `OTPChallenges` | `mobile_otp_challenges` | 007_mobile_modules |
| `Friendships` | `mobile_friendships` | 007_mobile_modules |
| `Pets` | `mobile_pets` | 007_mobile_modules |
| `Offers` | `mobile_offers` | 007_mobile_modules |
| `Entries` | `mobile_entries` | 007_mobile_modules |
| `Comments` | `mobile_entry_comments` | 007_mobile_modules |
| `PetComments` | `mobile_pet_comments` | 009_pet_discussions |
| `CommentReactions` | `mobile_comment_reactions` | 009_pet_discussions |
| `Reactions` | `mobile_entry_reactions` | 007_mobile_modules |
| `Cases` | `mobile_cases` | 007_mobile_modules |
| `Claims` | `mobile_claims` | 007_mobile_modules |
| `Leads` | `mobile_leads` | 007_mobile_modules |
| `Subscriptions` | `mobile_subscriptions` | 007_mobile_modules |
| `Appointments` | `mobile_appointments` | 007_mobile_modules |
| `Adoptions` | `mobile_adoptions` | 007_mobile_modules |
| `CareUpdates` | `mobile_care_updates` | 007_mobile_modules |
| `Groups` | `mobile_groups` | 007_mobile_modules |
| `GroupMessages` | `mobile_group_messages` | 007_mobile_modules |
| `Conversations` | `mobile_conversations` | 007_mobile_modules |
| `Messages` | `mobile_messages` | 007_mobile_modules |
| `Receipts` | `mobile_receipts` | 007_mobile_modules |
| `Invitations` | `mobile_invitations` | 007_mobile_modules |
| `Notifications` | `mobile_notifications` | 007_mobile_modules |
| `Preferences` | `mobile_preferences` | 007_mobile_modules |
| `Expressions` | `mobile_expressions` | 007_mobile_modules |
| `Drafts` | `mobile_drafts` | 007_mobile_modules |
| `Media` | `mobile_media` | 007_mobile_modules |
| `Reviews` | `mobile_reviews` | 007_mobile_modules |
| `VisitFeedback` | `mobile_visit_feedback` | 007_mobile_modules |
| `Outbox` | `mobile_outbox` | 007_mobile_modules |
| `Qualifications` | `mobile_qualifications` | 008动态SQL |
| `Guardians` | `mobile_guardians` | 008动态SQL |
| `Operations` | `mobile_operations` | 008动态SQL |
| `Deliveries` | `mobile_deliveries` | 008动态SQL |
| `DataRequests` | `mobile_data_requests` | 008动态SQL |
| `RightsCases` | `mobile_rights_cases` | 008动态SQL |
| `Authorities` | `mobile_authorities` | 008动态SQL |
| `AuditEntries` | `mobile_audit_entries` | 008动态SQL |
| `PushDevices` | `mobile_push_devices` | 008动态SQL |
| `RevenueLedger` | `mobile_revenue_ledger` | 008动态SQL |
| `SourceDeclarations` | `mobile_source_declarations` | 013_source_declarations |

身份和会话使用mobile_accounts／mobile_identities／mobile_sessions；用途授权使用mobile_auth_challenges（purpose_grant，有效5分钟）；全部已读快照使用mobile_receipts（有效10分钟）。禁止继续将旧UML的推测表名当作数据库事实。完整字段查[OpenAPI](api/mobile-v2.openapi.json)，主路径查[代码逻辑手册](CODE_LOGIC_AND_UML_REFERENCE.md)。
