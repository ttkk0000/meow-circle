# M&D Web 消费端

对应Figma手机V2默认浅色及跨端布局合同。独立于保留的旧`web/`，使用现行v2服务、React／TypeScript及公共任务页面组件。源码状态与设计追踪见[本批说明](../docs/client/FIGMA_ANDROID_WEB_2026_10_24.md)。本批未编译、类型检查、测试或启动。

## 代码入口

- `src/App.tsx`：主导航、模块导航、任务路由、账号隔离及深链。
- `src/components`：任务骨架、列表行、表单、照片队列、大图、用途验证和原操作重试。
- `src/features`：社区、领养／照护、预约、有偿信息、互助／地图、消息／群、账号与受限事项。
- `src/core`：同源会话、无损int64、OpenAPI派生合同、查询缓存及IndexedDB私有稿。

## 环境与交付

需要Node.js≥22.12.0；依赖以`package-lock.json`为准。仓库中的`dev`、`typecheck`、`build`脚本仅提供入口，本批未执行，应用依赖也未安装。实际验证需用户明确授权后进行；不要通过CI代跑来绕过仓库规则。

服务端配置精确`MND_WEB_ORIGIN`，生产使用HTTPS；开发仅允许loopback HTTP，浏览器与API保持同源。已有身份密钥用于加密HttpOnly Cookie，不把账号access／refresh token返回JavaScript。反向代理需保留Host与HTTPS语义。多副本部署必须为刷新协调提供粘性路由或统一协调；当前进程内锁不能代替分布式互斥。

Vite输出目录为本项目`dist`，Go路由以`MND_WEB_DIST`覆盖默认`web-client/dist`并托管`/app/`；深链回退仅应用页面，缺少产物返回503、缺失静态资源返回404。公开分享位于`/share/{kind}/{id}`，每次重新核对游客ACL，图片地址只使用配置的可信origin。

地图需要可嵌入的HTTPS地址模板`VITE_MND_MAP_EMBED_TEMPLATE`，以`{lat}`／`{lon}`／`{zoom}`占位，仅传公开大致中心，不含账号、私密线索或家庭地址。缺配置显示实际不可用；Android对应`mnd_map_embed_template`。位置只在用户点击后单次读取，可手动输入；仅有文字区域的求助不会画成0/0地图点。发布不要求经纬度，旧地图点须主动选择保留且城市／区域未变；区域提醒按城市保存。

正式协议URL、OAuth供应商、短信／邮箱／年龄监护核验、系统推送及事件消费者调度须按真实环境配置。未配置时保留不可用或待核对状态，禁止固定成功。求助关注接入既有worker的`content_updates`作业与原租约／游标／ACK；还提供受保护的`case-watch-alerts`分批接口，消费者权限需显式包含相应名称。本批没有启动或配置调度器。免打扰排除全局数仍是原设计建议，保持服务端现行计数政策，不能用客户端自行减去未读数。

## 静态核对

`scripts/sync_web_contract.py`默认只检查合同一致，`scripts/check_web_client_static.py`只读核对引用／资源／锁文件／隐私守卫。`--write`仅用于明确同步派生文件，不能据此宣称类型或运行通过。

草稿以origin、账号和任务隔离，保留原请求编号、原材料及版本；结果未知时先查原回执，不能重建编号。敏感验证码与用途授权仅驻内存。受限事项能力只允许查询／补充该事项，不是账号凭据；不要写进URL、日志或分享。

独立阅读、设置、发布管理及宠物交流见[续审记录](../docs/client/FIGMA_READING_PUBLICATIONS_2026_10_24.md)。`MND_READING_CATALOG`指向同结构的UTF-8 JSON发布物；未配置时仅展示草案和指南，正式文本须包含真实主体与有效版本信息，不能覆盖既有历史文本。注册仍要求真实服务／隐私URL。容器源码已加入Web构建阶段及静态产物复制，可通过公开配置build args提供`VITE_MND_TERMS_URL`、`VITE_MND_PRIVACY_URL`、`VITE_MND_MAP_EMBED_TEMPLATE`；本批没有执行构建或部署。worker仅打包为独立二进制，不自动启动调度。
