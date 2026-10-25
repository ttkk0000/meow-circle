# 独立来源声明合同与存储

2026-10-26，`AI-20261026-FIGMA-FLOW-CONTINUATION`。源码已写；迁移、编译、测试与运行未执行。客户端交付见[来源流程与分享进入](../client/FIGMA_FLOW_CONTINUATION_2026_10_26.md)。

## 声明与发布分开

Figma 的“来源声明已记录”不能由一次预览读取或本地勾选推导。新增独立私密 `source_declarations`：

| 接口 | 含义 |
|---|---|
| `POST /api/v2/source_declarations` | 主动确认素材权利后，追加绑定当前清单的声明；不发布帖子 |
| `GET /api/v2/source_declarations/{id}` | 当前账号读取自己的有效声明 |
| `GET /api/v2/source_declarations` | 当前账号的有效声明列表，数据库按owner筛选 |
| `GET /api/v2/operations/{key}` | 查询声明原请求的真实事务回执；404仍保留原请求 |

请求为 `SourceDeclarationInput`：独立 `client_request_id`、完整 `content: EntryInput`、主动 `rights_confirmed`、可选更正链 `previous_id`。声明请求编号和帖子发布编号分别保存。声明号不替换草稿编号，也不用于发送帖子。

来源清单继续绑定账号、正文、标题、受众、宠物／空间等Entry字段、Entry版本、媒体编号／SHA256／版本及来源。服务端重新计算，拒绝过期清单；已有文件AI标识不能声明为原创。私密草稿走本人私密附件检查，非私密内容保持公开用途检查，不能暗中提升照片用途。

声明正文只存 `manifest_hash`、`origin`、`previous_id`。基础Record另有本人owner、时间、版本及前记录引用；不复制草稿文字、位置、照片或身份材料。通过 `s.command` 与原请求回执／outbox在同一事务提交。同编号不同内容仍拒绝；更正产生新ID，不修改原记录。消费者无声明PATCH、PUT或DELETE；正式资料删除和保留流程仍可按既有政策处理这些私密记录。

## 发布仍重新验证

`PublicationDeclaration.record_id` 是可选关联。提供时，发布服务验证本人、有效记录、清单摘要和来源全部一致；原manifest、显式确认、受众、媒体用途、资格、版本与宠物所有权检查继续执行。兼容旧客户端的 `record_id = 0`，仍要求现有manifest及主动确认；没有放开旧客户端的发布资格。

声明保存只表示用户提交的来源选择已被记录，不代表平台保证素材权利、完成AI模型识别或真实性核验，也不授予发布、领养、线下签约、联系人或核验资格。没有增加本地年龄／监护豁免或SMS自动注册。

## 迁移与数据治理

- [013迁移](../../migrations/013_source_declarations.sql)：独立私密表，owner／引用／owner状态游标索引；无示例账号或数据。
- [开发Compose](../../docker-compose.yml)已接入顺序初始化挂载；生产仍由原显式迁移账本执行，未执行任何迁移。
- [Postgres就绪检查](../../internal/store/postgres/mobile.go)要求13号表及原作业检查点存在，不回退内存。
- 注销盘点、指定资料选择、既有保留与删除执行器覆盖声明；两端资料选择器提供“来源声明”。授权删除来源记录不会自动修改已发布内容的AI展示或原文件来源标识。

当前消费者合同247路径、278操作、126服务端schemas，Web派生128 schemas，77对共享DTO，45张业务表、13顺序迁移。14类技术运维页面仍在独立合同中，不混入消费者740状态。

## 源码验证边界

新增4个Go回归函数仅源码：声明与发布分离／原回执、同号重放与更正追加、过期清单与跨账号关联拒绝、私密照片与已知AI标识保持。没有运行这些函数、Go工具链、数据库或API。静态字段一致和格式语法读取不能代替事务、并发、迁移及HTTP运行验收。
