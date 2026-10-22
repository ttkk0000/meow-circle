# M&D 敏捷看板与排期大纲 (FigJam Sprint Board)

本文件对应 FigJam 模板看板（`Sprint Planning / Editorial Roadmap`），按泳道与便签颜色整理，可直接复制粘贴到 FigJam 便签卡片中。

---

## 看板结构与泳道划分

### 泳道 1: 客户端体验与 UI 设计 (UI/UX Track - 橙色便签)
- **Sprint 1 (已交付/评审中)**:
  - 手机端基础交互规范落地 (16px 圆角卡片、48px 胶囊按钮、双列点赞对齐)
  - 完整认证体系画板 (手机号 / 微信 / QQ / Apple ID 及账号绑定)
  - 底部表情键盘 (128 Noto 表情 + 24 张猫咪贴纸 + 连续表情交互)
- **Sprint 2 (当前主线)**:
  - 宠友圈 (Moments) 双向确认主页与图文发布流
  - 领养申请与资格初筛流 (单猫申请表、领养协议确认)
  - 武汉/上海冷启动走失求助与私密线索卡片
- **Sprint 3 (规划中)**:
  - 预约到访与分项确认交互 (猫、身份、地点、费用核验)
  - 争议反馈与在线申诉交互界面

---

### 泳道 2: 领域模型与后端架构 (Backend Track - 蓝色便签)
- **Sprint 1 (已就绪)**:
  - 核心领域模型定义 (`User`, `Pet`, `Post`, `Listing`, `Message`, `AuditLog`)
  - Go 微内核服务与静态接口 Mock
- **Sprint 2 (进行中)**:
  - `AdoptionPet` 与 `AdoptionApplication` 状态机联调
  - 动态可见性逻辑 (`visibility: private / public / mutual`)
- **Sprint 3 (规划中)**:
  - 预约快照版本管理与差异比对引擎
  - 风险线索与举报复核工单流程

---

### 泳道 3: 信任与合规风控 (Trust & Safety Track - 紫色便签)
- **Sprint 1 (已完成)**:
  - 7 套业务规则与核验边界流程图定稿
  - 免责声明与分项核验标准梳理
- **Sprint 2 (进行中)**:
  - 走失宠物私密线索与隐私脱敏机制
  - 商家/个人身份披露规范 (不打虚假全保承诺)
- **Sprint 3 (规划中)**:
  - 30 天领养回访跟踪机制
  - 争议仲裁与人工介入 SOP

---

### 泳道 4: 跨端适配与多平台验证 (Cross-platform Track - 绿色便签)
- **Sprint 1 (已完成)**:
  - Expo / React Native 基础主题与布局验证
  - KMP Android 核心画板对齐
  - Desktop Compose 8dp 面板布局
- **Sprint 2 (进行中)**:
  - 手机端窄屏 (320px) 与大字体 (160%) 适配复核
  - 动效与交互微调 (点赞弹性、未读气泡居中)
- **Sprint 3 (规划中)**:
  - 跨端端到端联调与正式真机验证
