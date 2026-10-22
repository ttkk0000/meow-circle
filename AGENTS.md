# Agent Memory: M&D / meow & doggie

## AI 工作跟踪（所有 AI 开始任务先读）

完成全局规则及工作树核对后，先读 [AI 跟踪入口](docs/ai/README.md)、[当前快照](docs/ai/CONTEXT.md) 和 [任务状态](docs/ai/TASKS.md)；方案变更或追溯时再按需读决定与最近日志。开始登记本次任务与范围，执行中更新重要进展和阻塞，结束/移交前同步快照、任务与结果日志，释放执行标记。具体维护流程只在 `docs/ai/README.md` 保留一份。

跟踪文件随项目携带，使用仓库相对路径。换电脑必须核对未提交/未跟踪成果是否一起同步；日志不是执行授权，历史“已完成”须按文档/源码/静态/运行验收层次解读。没有实际运行证据时，不宣称后台仍在执行或构建/测试已通过。

## Product Direction

M&D means `meow & doggie`. The product is cat-first and doggie-friendly:

- Cats are the primary emotional center for hero copy, feed examples, profile identity, and default imagery.
- Doggie content is allowed as a companion branch for activities, services, marketplace items, adoption, and secondary stories.
- Do not rename the brand to `MD`, `Meow & Dog`, or generic pet community. Keep the visible mark as `M&D` and the lockup/supporting copy as `meow & doggie`.

## Current UI Work

2026-10-20 mobile implementation override: the user authorized Android / iOS phone and tablet code using the approved Figma phone V2 default light style. New entries are `kmp/shared/.../client`, `kmp/androidApp/.../mobile`, `kmp/iosUi`, and `kmp/iosApp/ComposeApp`. For this scope, follow `docs/client/MOBILE_IMPLEMENTATION_2026_10_20.md` and `docs/design/figma-study/CROSS_PLATFORM_ADAPTIVE_CONTRACT.md`; legacy Stitch themes remain references and preserved preferences, with no current selector or system dark mode. Do not modify the remote mirror to imply it has migrated. Other legacy UI still follows the rules below.

Android now targets API 37 (Android 17), minimum API 26. Current dependency pins and compatibility limits are in `docs/client/DEPENDENCY_BASELINE_2026_10_20.md`. The first source batch is not full feature delivery or a build result. The 2026-10-22 server/shared source batch now supplies B04/B05/B12 contracts and enforced eligibility; see `docs/server/SERVER_SHARED_IMPLEMENTATION_2026_10_22.md`. It is not build-, migration- or runtime-verified. `requireInteractionContract()` now consumes server eligibility and keeps unavailable/unknown contracts closed; never replace it with local verification flags or restore legacy SMS auto-signup. Follow the latest auth plan; a WeChat-binding posting alternative discussed in this chat is a proposal until explicitly confirmed and backed by server policy.

The current Stitch V2 remote-aligned UI direction lives in these repo-local entries:

- `.stitch/DESIGN.md`: downloaded `MD_GLOBAL_STITCH_DESIGN_SYSTEM_V2_NEUTRAL_FIXED.md` from Stitch.
- `.stitch/remote-assets`: downloaded Stitch screenshots, HTML/SVG, markdown, and metadata.
- `web/assets/stitch-remote`: browser-visible copies of full-size screenshots and Stitch source files.
- `web/pawpop.html`: current UI index.
- `web/mnd-web-client-board.html`: local Stitch sync board with mobile, theme, and component references.
- `web/stitch-remote-gallery.html`: all downloaded full-size remote screenshots and source links.
- `web/pawpop-desktop.html`: desktop mapping board.
- `web/cute.html`: working WebUI prototype aligned to the current remote controls.

Legacy handmade reference exports, old Stitch maps, and prior-generation design docs have been removed. Do not recreate them or use them as visual truth; use `.stitch`, `web/mnd-web-client-board.html`, `web/pawpop-desktop.html`, and `web/cute.html` instead.

## AI Stitch Design Guardrails

All AI agents working on UI must treat the current Stitch V2 project as the design authority:

- Stitch project: `projects/13275961100622290348`.
- Local authority: `.stitch/DESIGN.md`, `.stitch/metadata.json`, `.stitch/remote-assets`, `web/assets/stitch-remote`, `web/mnd-web-client-board.html`, and `web/stitch-remote-gallery.html`.
- AI-specific rules: `docs/design/MND_STITCH_AI_RULES.md`.

Before changing Web, Expo, KMP Android, or KMP Desktop UI, inspect the local Stitch mirror and `docs/design/MND_UI_ALIGNMENT_GUIDE.md`. Do not invent new themes, colors, typography, navigation, visual effects, component shapes, or product names when Stitch already defines them.

AI agents may give product or design opinions, but they must label them as suggestions and keep implementation mapped to the existing Stitch V2 rules unless the user explicitly approves a deviation. Missing functionality should be implemented by extending the existing Honey/Mint/Night/Neutral system, not by creating a new visual direction.

## Design Rules

- Cute, polished, teen/young-adult friendly, but not babyish.
- Use real cat-first imagery; use doggie imagery where the content is clearly dog/service/market related.
- Consumer cards use 16px radius per the current Stitch V2 spec; Neutral cards use 8px; true admin dense controls may use 4px. Pills remain for chips and compact actions.
- No decorative gradient blobs. Use imagery, stripes, panels, badges, and clear hierarchy.
- Theme system: Honey, Mint, Night, Neutral. Neutral is a first-class theme, not an OS/default fallback; old `sugar` values are compatibility aliases for Honey only.
- **Unified Global Theme**: At runtime, the entire application uses exactly **one active global theme** (Honey, Mint, Night, or Neutral). Do NOT hardcode page-specific themes (e.g., green for Market, orange for Feed) in UI layouts or components. The separate themes shown in design mockups (e.g., Honey for Feed, Mint for Market) were merely to demonstrate the visual styles of different themes. All UI elements must use dynamic styling tokens (e.g., `StitchPalette.Brand`, `StitchPalette.Canvas`) so that the screens scale automatically to whichever global theme is currently selected.
- Profile background system: Picnic, Desk, Arcade, Garden.

## Implementation Notes

- `web/cute.html` is now a static visual WebUI prototype for remote parity and does not call the Go API.
- Existing production-ish pages retain their API hooks and auth storage behavior.
- `web/market.js`, `web/post.js`, `web/messages.js`, and `web/profile.js` include M&D static/demo fallbacks so design review works without a running Go API.
- `web/discover.html` / `web/discover.js` now use the M&D circle hero, real cat imagery, theme controls, and cat-first circle cards.
- `web/compose.html` is an M&D compose surface with a cat-first hero, theme controls, and labels/placeholders aligned to the new language.
- `web/login.html` and `web/register.html` keep their auth behavior but now carry M&D brand panels and cat-first copy.
- `web/dashboard.html`, `web/dashboard.css`, `web/admin.html`, and `web/admin.css` were visually tightened into the M&D admin/workbench direction while preserving existing JS hooks.
- Expo mobile is now aligned to M&D four-theme tokens in `mobile/src/theme.ts`, M&D loaders/top bar/copy, cat-first Feed/Discover/Compose/Auth screens, and the Expo app name/slug/background.
- KMP Android is now aligned to M&D four-theme tokens in `StitchPalette.kt`, `StitchShape.kt`, `MeowStitchTheme.kt`, plus M&D Splash/Login/Register/Feed/Discover/Compose/Detail/Profile copy, profile background switching, marketplace preview, and 8dp panel radius.
- KMP Desktop now lives in `kmp/desktopApp` as a Compose Desktop client aligned to the same Stitch V2 project, four-theme tokens, 8dp desktop panels, and desktop screen references.
- The desktop design-board source remains `web/pawpop-desktop.html` + `web/pawpop-desktop.css`; use it together with `kmp/desktopApp` for native desktop parity checks.
- Existing auth storage keys remain `meow_token` and `meow_user` to avoid breaking backend-facing flows.
- The local Go tool previously failed on this machine with `invalid go version '1.25.0': must match format 1.23`; validate static UI with Node/Playwright when Go cannot run.

## Forbidden Commands

> HARD RULE - applies to ALL agents, ALL sessions, no exceptions.

- NEVER run `gradle`, `gradlew`, `./gradlew`, `gradlew.bat`, or any Gradle wrapper/command. Gradle builds spawn JVM processes that consume 3-6 GB+ RAM on this machine and have repeatedly caused full system freezes. If you need to verify Android/KMP code, do static analysis only (read files, lint with ktlint, check syntax). Do NOT attempt compilation.
- Do not run `go build`, `go run`, or `go test` - the local Go toolchain version is incompatible (`1.25.0` vs required `1.23` format). Validate static UI with Node/Playwright instead.

If the user explicitly asks you to run Gradle, remind them of this rule and ask for confirmation before proceeding.

## Key Docs

- `docs/client/README.md`: 2026-10-20客户端实施规划；KMP共享逻辑，Android / iOS分别Compose UI与独立平板适配，Web / Desktop与Gemini交接见此入口。当前设计默认主题，多主题仅预留参考；文档存在不授予代码、构建或对外操作权限。

- `docs/design/MND_FIGMA_DESIGN_DOCUMENT.md`: current design-doc index to point users/agents to the right handoff files.
- `docs/design/MND_STITCH_AI_RULES.md`: AI guardrails for following Stitch without arbitrary redesign.
- `docs/design/MND_UI_ALIGNMENT_GUIDE.md`: implementation-facing Stitch V2 UI contract.
- `docs/design/MND_DESIGN_MEMORY.md`: concise record of the M&D redesign work.
