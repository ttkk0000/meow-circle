# 业务后台设计到代码：2026-10-27

文件`3OFOC3vu18Kx9sRPjummOC`，页面`843:21`，本轮读取并查看`843:22`至`843:34`共13份高保真上下文及截图。每份JSON保留生成上下文与组件使用说明；其中临时资产URL只作为来源记录，运行代码没有使用这些URL。

对应[原13稿设计截图和节点账本](../admin-2026-10-26/README.md)，源码见`web-client/src/operator/`。两类后台共用`web-client/src/components/WorkspaceShell.tsx`与现有运营视觉组件；业务凭据、角色、版本与回读单独处理。

本轮实际下载并核对了house、message-circle、shield-check、user-round、check、sliders-horizontal、circle-alert七枚原SVG，与`web-client/public/assets/{ops,figma}`对应文件完全一致，固有及显示尺寸均24px，未新增或描摹图标。

详见[当前源码恢复点](../../../client/COMPLETE_LANDING_2026_10_27.md)。这是设计读取及源码证据，未进行浏览器、逐像素或运行验收。
