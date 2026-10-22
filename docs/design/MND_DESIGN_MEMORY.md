# M&D Design Memory

## 本地分支合并 — 2026-10-22

- 保留本地 Stitch V2 同步、Expo 交易／订单页面、Web API 接线、Desktop 业务页面和客户端打包入口；这些旧端成果不代表已迁移到新移动端 v2 合同。
- Android 旧入口沿用远程 Community／Adoption／Trade 导航和完整页面，接回本地 action-counts 角标；共享 v1 SDK 去除重复 DTO／方法并保留本地兼容调用。
- 当前新 Android／iOS 入口及服务端资格关卡以最新交接为准。历史主题说明仅适用旧端；全局主题由用户选择，不能按业务页硬编码。
- 本次仅静态核对；未运行编译、测试、迁移、服务或设备。

## Global Unread Design Supplement — 2026-10-20

- User requested global unread with 99+ above 99, post replies, DMs and groups, asked to find omissions, then explicitly limited this task to design and requested a handoff to the responsive-design chat.
- Added Figma page352:2, review352:3:8 phone states,1 boundary board,4 platform entry patterns; badge353:7 and message-row353:32 reuse current light fonts/tokens. Exact mapping and scope: figma-study/GLOBAL_UNREAD_HANDOFF_2026_10_20.md.
- Categories cover chat/requests, replies/mentions/reactions, relations/invitations, adoption/appointments/lost-pet progress and moderation/security. Unread differs from pending work. Example107 active +124 muted =231 for mark-all; mute/global inclusion is a design recommendation pending user confirmation.
- Native-layer, font, badge range/contrast and link-target checks passed; four composition groups visually reviewed and affected fixes rechecked. Review uses precise Figma-node URLs, not a verified running-app prototype. New full cross-platform state pages, API semantics, suppliers and app behavior remain unimplemented by this task.
- Follow the state ledger and final validation; do not rerun generators just to recover IDs. No application source, build, runtime tests, commit or push in this task.

## Android / iOS First Source Batch And Dependency Upgrade — 2026-10-20

- User authorized mobile / tablet code after the planning and full visual adaptation work. Added shared v2 client models / transport / state, separate Android Compose and iosUi Compose surfaces, a thin Swift host with Keychain, Android Keystore, window-size navigation, native photo pickers, ordered uploads and versioned cloud drafts. Existing SwiftUI / v1 sources are preserved. These are first-batch sources, not full F00–F14 delivery.
- Android compile / target 37 and minimum 26; dependencies upgraded to current stable artifacts within the documented KGP branches, including AGP 9.3.3 / Gradle 9.7.1 patch updates, Kotlin 2.4.21 and CMP 1.12.2. Exact patch compatibility, dependency resolution and native builds remain unverified. Read docs/client/DEPENDENCY_BASELINE_2026_10_20.md.
- New mobile default is the approved Figma light mode. Legacy Stitch themes / preferences remain; no selector or automatic dark mode. Both tablet layouts use actual window size and remain independent platform implementations.
- Server audit found B04 still absent for the newly confirmed three-identifier account and current-phone interaction rule. New login only exposes existing account-name / phone password behavior; email is not faked via username. Outward commands are blocked in the shared Store until a real server eligibility contract and write gate exist; reading / editing / private drafts remain usable at source level. User asked about WeChat-based posting eligibility; this was explained as a possible product rule, distinct from real-name verification, without silently changing the confirmed auth plan.
- Read docs/client/MOBILE_IMPLEMENTATION_2026_10_20.md and docs/server/CLIENT_READINESS_2026_10_20.md for completed scope, remaining features and blockers. Source-only checks passed; four network regression test sources were added but not executed. No compilation, dependency installation, server / simulator launch, commit, push or delegation.

## One Account, Three Login Identifiers — 2026-10-20

- User confirmed one stable M&D account with unique account name, email and phone login identifiers. Nicknames remain separate and may repeat; identifier changes keep user_id and all resources.
- Ordinary login does not force SMS. Verify the current phone before first posting/commenting/messaging and other outward interactions; reuse valid verification rather than sending SMS for every login or message. Phone changes require verification and preserve the old binding on failure.
- Updated MND_MOBILE_AUTH_PLAN.md as the rule source and synchronized cold-start, client F01/B04 and server handoff requirements. Email login, database constraints/migration and server-side interaction gates remain unimplemented; existing Figma and APIs were not changed.
- Documentation-only static review. No compilation, tests, provider calls, code, commit or push in this task.

## Full Cross-platform Visual Coverage — 2026-10-20

- User requested all missing platform styles. Added 569 product/interaction states in four native editable layouts (Web, Desktop, tablet portrait and landscape), plus 32 narrow/split-window samples: 2,308 target roots. Ten phone review keys map to the unified index rather than extra product screens.
- All target roots have latest layout/content/font/basic-navigation checks with zero remaining issues and no missing source text; all 16 logical modules have screenshot samples. Chat bubbles now hug content with a width cap, conversation switching was added, and continuous emoji text overflow was corrected and rechecked.
- Full control restoration passed for 15 ordinary modules. Rebinding all dense sticker internals repeatedly exceeded Figma MCP timeouts; do not claim complete cross-platform expression interaction or device/runtime verification. The phone source prototype and existing expression handoff remain the detailed behavior reference.
- Current review root: 301:27; expression scene index: 343:2; file: 3OFOC3vu18Kx9sRPjummOC. Old 56 representative boards remain as historical references. Expression states were split into 16 context pages and a common-editing page; exact pageByKey and root IDs are preserved.
- Read figma-study/CROSS_PLATFORM_FULL_REVIEW_2026_10_20.md, CROSS_PLATFORM_FULL_MAPPING.md, CROSS_PLATFORM_ADAPTIVE_CONTRACT.md and cross-platform-full-validation.json. Default light only; old themes reserved. No client code, build or runtime tests in this design task.

## Cross-platform Coverage Correction — 2026-10-20

- User noticed missing new-platform pages. Live Figma read confirms only18Web /17Desktop /21tablet reference frames. Full mobile-flow adaptation is incomplete; earlier “completed56” wording was too broad.
- Tablet has12portrait834,8landscape1194 and1narrow744 sample. Only home/adopt/messages/compose have explicit paired orientations; the remaining task layouts and detailed states need mapping or additional design.
- Added figma-study/CROSS_PLATFORM_COVERAGE_AUDIT_2026_10_20.md and cross-platform-coverage-audit.json:579merged mobile logical keys/root IDs, grouped with conservative reference-only / unmapped statuses. Review hubs and parameter variants are not independent required screens; no coverage percentage asserted.
- Updated review/client entry points to distinguish representative designs, full design coverage and runtime adaptive implementation. This audit did not add missing Figma screens or change client code. Default light style remains the only current theme.

## Client Implementation Plan And Default Theme — 2026-10-20

- User requested detailed client completion/implementation documents before code, potentially for later Gemini collaboration. Created docs/client/README.md with architecture, feature matrix, design/adaptive acceptance, tasks and agent handoff. No client source/config changes, delegation or builds in this task.
- Target: shared KMP business/data/session/feature logic; separate Android Jetpack Compose and iOS Compose Multiplatform UI; Android tablet and iPad each have independent adaptive implementation. Existing SwiftUI shell is retained as a migration source, not the new main UI target.
- Proposed Desktop continues Compose Desktop/shared; Web uses a separate React/TypeScript/Vite app against the same v2 contract. Existing v1 SDK, static Desktop and Web demos are not v2 completion evidence.
- Latest user steering: current effective appearance is the design default light style. Preserve Honey/Mint/Night/Neutral as historical references and future extension points, keep old preferences, but no current selector, automatic system dark mode or four-theme delivery requirement.
- Plan records v1/v2 envelope/session migration, int64 browser precision, precise response types, browser auth/media and unauthenticated safety-report contract gaps. Public contracts and Gradle files have one modification owner when parallel work is explicitly authorized.
- Read docs/client/AGENT_HANDOFF.md for scoped Gemini prompts. No messages were sent to Gemini/other threads; future code/build/run authorization remains separate.

## Friendly Light Cross-platform Design — 2026-10-20

- User requested Web, Desktop and tablet designs based on the existing phone style; dark mode deferred. This authorizes Figma design work, not client implementation or builds.
- Added native Design pages 295:2–295:5 in file 3OFOC3vu18Kx9sRPjummOC. Review hub: 301:27. Web18, Desktop17, tablet21:56 page/state references, not56 independent complete flows or a copy of all579 mobile roots.
- Reused Mobile Light V2 mode56:0, Noto Sans SC / Manrope, coral primary actions,16-radius cards and48-high controls. Kept phone and old dark pages unchanged; production Stitch and clients remain unmigrated.
- Web uses capped content width; Desktop persistent navigation and conversation context; tablet834×1194 /1194×834 plus744-width single-reader fallback. Portrait/landscape specimens are static references, not runtime rotation support.
- Final structure check: all56 roots present and populated; no missing fonts, invalid navigation destinations or auto-layout box overflows. Discrete pet photographs only; native editable UI layers. Representative pages inspected and local component sizing corrected.
- Current handoff: figma-study/CROSS_PLATFORM_LIGHT_REVIEW_2026_10_20.md; state/config/nodes/validation files use cross-platform-light prefix. Preserve prior unread-badge work and all phone ledgers.
- No app compilation, tests, real device, screen reader, system keyboard, arbitrary input or cross-device sync verification. Follow global no-self-build rule.

## Unread Badge Alignment — 2026-10-20

- User reported left-aligned unread numbers on the phone message list. Fixed shared component105:369: badge105:375 centers both axes; text105:376 centers horizontally and vertically.
- All5 phone conversation instances inherited the correction with zero center offsets. Numeric values, size, theme bindings and prototype routes were preserved.
- Evidence: figma-study/unread-badge-alignment-2026-10-20.json; screenshot: figma-study/messages-unread-centered-2026-10-20.png.
- Figma and design records only. No client code, builds, tests or device runs.

## Current Mobile Review And Authentication — 2026-10-19

- User explicitly requires Figma review before client implementation. No client source, production Stitch, build or test execution in this task.
- Native phone design completion is ready for user review: hub 238:371769, editable six-screen board 251:375655, file 3OFOC3vu18Kx9sRPjummOC, page 56:25.
- Current mobile-completion-state.json tracks213 new/reworked states; mobile-completion-content.json is current. Static merge with historical ledgers found579 unique reference root IDs, none missing, empty or on another page. Counts include variants/overlays.
- Phone, WeChat, QQ, iPhone Apple, first account/linking, recovery and account management now have native frames; no SMS/OAuth integration.
- Friend pre-confirmation, found/reclaim, appointment V1/V2/V3, trial/handover,30-day private updates, group transfer/dissolution, recipient scopes, notifications, material outcomes, multi-photo recovery and commercial visit checks are supplemented.
- Expressions retain128 Noto SVGs,24 cat stickers,16 live prototype contexts and23 composers. Added fixed continuous/cursor/delete/recent/favorite examples plus320-width and160% typography specimens; no arbitrary editor/device acceptance claim.
- Final polish uses primary filled coral footers, neutral disabled borders,16-radius fields/cards. Field master controls56:190/194/198 now FILL their instance width;320 root yields288 field/control widths. Original role/entry guards must be preserved when reconstructing.
- Read figma-study/MOBILE_FIGMA_REVIEW_2026_10_19.md for latest source, scope and remaining runtime/operational limits. Earlier13-item audit is a pre-completion baseline.

Created: 2026-05-26

Last updated: 2026-10-16

## Pet Friends Circle and Own-pet Missing Flow — 2026-10-16 extension

- User requested a carefully designed Moments-like pet friends circle and an
  own-pet missing-status flow with downstream linkage. Continue design only;
  application implementation remains gated on complete design confirmation.
- Created two four-screen raster proposals in figma-study: pet-friends-circle-
  concept-v2.png and pet-lost-linkage-concept-v2.png, with two standalone SVG
  flow diagrams and detailed briefs. Built-in image_gen; illustrative photos
  and data, not real pet records. Approved Friendly direction retained.
- Circle defaults are review recommendations: mutual friends, chronological feed,
  no contact import, separate following, exclusive per-entry audiences, shared
  reaction/sticker patterns, no video. Public-post permission never opens a
  private pet profile. Comment/history rules remain reviewable proposals.
- Missing flow separates private status, public case, active sharing and receiver
  subscription. Private leads are not comments or verified matches. Owner confirms
  reunion, stops this case's alerts and updates linked cards; other closure is not
  reunion. A repeat incident gets a new case. No background tracking by default;
  the 5–100 km location model and caretaker scope remain pending decisions.
- Figma editing transport repeatedly failed. No new native frames or prototype
  routes from this extension. Existing 134 native states remain the prior range;
  22 circle + 18 missing-flow scenarios are PLANNED, not completed frames.
  Source/status: figma-study/pet-social-lost-design-status.json.

## Social, DM, Groups and Phone Prototype — 2026-10-16

- Later explicit scope: video is deferred. MVP pet diaries are text + multiple still
  images; no video upload / playback / video messages / in-app verification-video
  requirement. Static stickers first is a proposal, animated media remains deferred.
  Images need compression / thumbnails / lazy loading when implemented; this does
  not impose a business limit on the number of pets.
- Newly stated requirements (not yet designed): each user can create multiple pets
  without an artificial count cap; each pet has a diary and configurable profile
  visibility; each entry has its own visibility; emoji / sticker UI shared across
  posts, comments, adoption discussion, DM and groups. Private-profile / public-entry
  interaction and caretaker permissions are still unconfirmed. Do not claim the
  existing 134-state prototype implements these extensions.

- User approved social / comments / likes / reports and one-to-one first, groups next,
  and explicitly prohibited application-code work until all designs are confirmed.
  User then requested true pill ends and a clickable phone preview.
- Same learning Design, pages 10–11: six of the original 59 roots updated in place,
  75 new states = 134 phone states. Added 12 reusable social mains. Cat-first feed,
  dog / rabbit branches, context-specific reports and message-request privacy.
- Buttons: 48px high / 24px radius, smoothing 0; cards and inputs remain 16px.
  Token VariableID:104:314; focus keeps 3px gap / 2px stroke, 54h / 27r.
  Minimal comparison and old 00–09 preserved. Do not revert to earlier 16px buttons.
- [Current review + three flows](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=109-3347),
  [handoff](figma-study/SOCIAL_CHAT_HANDOFF.md), current ledger social-chat-state.json.
  Native routes / scrolling / like variants; preset drafts / outcomes, no real service.
  Eight prototype entries include exceptions. No sharing-permission change.
- Readback: 134 roots, no placeholders / missing fonts / checked horizontal overflows /
  broken NODE targets. 589 NODE actions include CHANGE_TO and exclude BACK; 166
  checked primary / secondary instances 48h / 24r. Final native export inspected.
  Physical-phone / keyboard / reader acceptance and browser preview check incomplete.
  No full WCAG claim. Production Stitch / application code unchanged.
- Source: social-chat-content.json, components / allocation / batch-01…13, final-validation,
  prototype-flows, polish and review-sync. Earlier 59-screen exports below are history.

## Phone-first Mainland-use Redesign — earlier 2026-10-16 stage

- Latest user request: apply the selected Friendly direction to ALL ordinary
  phone screens. Completed on the existing 59 roots, keeping their IDs/positions.
  Shared Action styling, field/panel corners and phone-only brand/accent aliases
  unified; error semantics preserved. Original 329 navigation sources preserved,
  plus one chat CTA = 330, no invalid destinations. Old Light/Dark unchanged.
  Current [review](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=81-192),
  [rollout](figma-study/FRIENDLY_MOBILE_ROLLOUT.md),
  [validation](figma-study/friendly-mobile-validation.json).
  Reviewed the full contact sheet and representative output; review button copies
  now share the phone Action family. Other platforms and production code/Stitch
  are unchanged. This is not runtime/usability acceptance.
- Earlier workflow correction: agree phone-wide controls/styles with them BEFORE
  generating further frames. Current 59 screens are structural drafts, not
  approved visual design. Pause canvas expansion; confirm the style foundation,
  then a compact control board and a few key screens before scaling the set.
- User then rejected the existing buttons and requested BOTH earlier directions.
  Built [01 friendly / 02 minimal button comparison](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=70-209)
  on existing page 11: 14 isolated variants, common text/context, pressed/loading/
  disabled/focus examples. No 59-screen replacement. User subsequently selected Friendly.
  [Ledger](figma-study/button-comparison-state.json), [scope](figma-study/BUTTON_COMPARISON_BRIEF.md).
- User then requested adjustment of BOTH focus outlines shown in the screenshot.
  Replaced touching 3px blue strokes with #626873 neutral 2px rings, 3px offset,
  corner-following geometry and stretch constraints. Updated both source variants
  and inherited review instances; normal/pressed/loading/disabled and 59 screens
  unchanged. Current export: button-comparison-focus-preview.png. Static only.
- Before full rollout, selected 01 Friendly buttons were applied to three isolated
  [phone key screens](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=81-192): home, adoption detail, application.
  Detail uses secondary chat plus primary apply; coral/white, 16px radius, 48px height.
  247 native nodes, 6 discrete photo fills, Noto Sans SC/Manrope, no missing fonts.
  Full-board export inspected. Visual-only board, no prototype routes or real forms.
  That stage retained the 59 source drafts; the later user request above supersedes
  that limitation. Production remains untouched. Ledger: friendly-key-screens-state.json.
- User rejected the uncomfortable dark-green/terracotta combination and asked
  for a redesign grounded in mainland pet-owner contexts. Finish the ordinary
  phone experience before expanding Pad, desktop, Web or dark mode.
- Current entry: [mobile V2 review](https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=61-12).
  Same file, new pages 10–11; prior 00–09 preserved. Ordinary learning Design,
  not native Specs. No client or Stitch migration.
- Current clear neutral surfaces, dark Chinese text, sparse coral/white
  actions, photo-led community, comparable adoption facts and left-aligned forms.
  Recent iResearch/KPMG sources inform context, not a universal Chinese color preference.
- 59 main reference screens: community 8, adoption 12, publishing 7, lost/help 8,
  messages/trust 9, account 7, secondary paid listings 4, shared states 4.
  A private-sighting receipt was added so a witness does not enter case-owner management.
- Navigation proposal: four destinations plus central publishing action;
  sales move to a secondary entry. Cat example is the main illustrated journey;
  dog cards do not open cat identity. Non-cat field matrices remain follow-up.
- 330 static navigation/back actions, no invalid destination in structural readback.
  All roots 390×844 / Mobile Light V2; old Light/Dark actual values unchanged.
  21 discrete photo nodes; 955 text / 546 instance / 451 vector / 1142 frame nodes.
  No missing/wrong fonts. The initial mobile build reused/added 34 local mains,
  including 22 Lucide icons and 12 controls; current ledgers cover later button
  assets separately. No whole-UI raster substitution.
- Twelve specified color pairs pass selected thresholds; primary-action white
  text 4.60:1, control boundary 3.46:1. Not full WCAG acceptance.
- Reviewed whole contact sheet and representative board; fixed transparent SVG
  wrapper fills, header placement, quiet-action weight, active indicator and
  verification method/date copy. Current exports are friendly-mobile-all-preview.png
  and friendly-mobile-final-preview.png; original mobile-v2 exports are historical.
- [Scope](figma-study/MOBILE_V2_BRIEF.md), [current ledger](figma-study/mobile-v2-state.json),
  [readback](figma-study/friendly-mobile-validation.json), [handoff](figma-study/README.md).
- No real crawling, map, SMS, input/submission/payment, device, build or test run.
  Formal policy, detailed review outcomes, species matrices and accessibility
  runtime acceptance remain follow-up; other platform expansion is deferred.

## Four-platform Dark Design — 2026-10-16

- User approved ordinary Figma Design after the native Spec workflow stopped:
  this education team reports `specs_enabled: false`. No Spec API was used.
- Same file `3OFOC3vu18Kx9sRPjummOC`; kept 00–04, added 05–09.
  21 unique dark screens: phone 14, Pad 2, desktop client 2, Web 3.
  Four editable comparison snapshots are not additional unique screens or auto-synced.
- Reused all 59 variables, 7 text styles and old 10 main components;
  added 5 local mains. Noto Sans SC / Manrope, existing Dark semantic mode.
  No production code or Stitch contract migration.
- Added honest cold-start, lost-list / ungeocodable-map empty states, auth / OTP
  illustrations, item-scoped verification and reporting / acknowledgement screens.
  No actual scraping, map, SMS, input, submission or payment integration.
- Inspected platform exports and fixed instance child fill, narrow navigation
  labels and comparison-row Hug sizing. Readback: all 21 roots explicitly Dark,
  no missing fonts, 13 discrete pet-photo layers, 49 safe phone navigation/back
  actions with structurally valid same-page top-level destinations.
- Nine specified dark color pairs meet selected thresholds; this is not full
  WCAG, device, screen-reader, keyboard, zoom or click-through acceptance.
- [Current dark ledger](figma-study/night-state.json),
  [final readback](figma-study/night-validation.json),
  [scope and remaining work](figma-study/NIGHT_DESIGN_BRIEF.md).
- Earlier entries below are dated stage history, not the current Figma inventory.
  No build, tests, account/seat/share changes or deletions in this pass.

## Cold Start, Lost Pets And Auth — 2026-10-16

- User requested initial content supply, potential collection of lost-pet
  information with map viewing, and improved login/platform rules.
- User explicitly selected Wuhan and Shanghai. Keep city coverage, source
  queues, filters and maintenance distinct; do not silently substitute other-city
  posts as nearby content. Specific districts/sources/hours remain undecided.
- Proposed: self-owned editorial baseline + authorized partner supply;
  allowlisted ingestion only after source-level rights review. No collection ran.
- Lost/found/sighting locations are timed event locations, not live tracking.
  Preserve public/private coordinate boundaries, uncertain locations and
  source/update distinctions; no precise pins invented from district names.
- Proposed: guest reading, authentication at publishing/contact/private-sighting
  boundaries, then return to the original draft without auto-submission.
  Phone OTP sign-in requires new backend/provider work; current module says no
  real SMS provider, and Expo currently gates the entry on login.
- Delivered [rules addendum](MND_COLD_START_LOST_PETS_AND_AUTH_SPEC.md) and
  [four static flow diagrams](flows/cold-start-and-auth.html). Source/SDK/legal
  references were checked. No app code, map/SMS integration, scraping, Figma
  edits, automation, build or tests in this pass. New screens remain backlog.
- User additionally requested adoption authenticity and effective reporting.
  Extended [trust rules](MND_TRUST_AND_DISCLOSURE_SPEC.md) to 1.1: item-scoped
  evidence/expiry, publishing alternatives, immutable report context, protected
  reporter channel, proportional temporary protection, independent review and
  appeal. No universal truthful/healthy badge, no report-count guilt, and no
  unsupported-report = malicious inference. Received means acknowledgement,
  not a completed human review. Metrics cover sampling, response, inconsistency,
  appeal changes and recurrence; actual verification/report cases were not run.

## Figma Learning Exploration — 2026-10-16

- User explicitly approved a new visual direction, not necessarily the old UI,
  and inclusion of dogs and other pets while keeping cats primary.
- Noncommercial learning only, in the existing kitty-circle education folder.
- Created one Design file: `3OFOC3vu18Kx9sRPjummOC`, now named
  `M&D｜产品与交互设计 V1｜非商业学习稿`. Five internal pages (00–04),
  12 review screens plus a separate 12-screen demonstration snapshot.
- New proposal: 社区 / 领养 / 找宠 / 消息 / 我的; independent Light/Dark,
  warm paper/terracotta colors and Noto Sans SC/Manrope typography.
- Followed Figma native design/component guidance: 59 variables, 7 text styles,
  10 main components, editable text/layout and discrete pet photos; no whole-UI raster.
- Final exports were inspected; component overflow and added-text clipping fixed.
  32 navigation/back actions have structurally valid targets; not a complete
  interaction or accessibility acceptance test. No real submissions simulated.
- Existing kitty-circle and Team project folders were not created here. Three
  other Untitled files (one Slides, two FigJam) have no creation record here
  and were not modified. No deletion or sharing/seat changes performed.
- Source of truth and scope: [learning brief](MND_FIGMA_STUDY_BRIEF.md),
  [handoff index](MND_FIGMA_DESIGN_DOCUMENT.md), [node ledger](figma-study/state.json).
  Production remains on Stitch V2 until a separate implementation decision.
- No app code, build, tests, or devices were run in this design task.

## Current Product Decision — 2026-10-15

Current documents: [strategy](../bmad/research-report.md),
[PRD 2.0](../bmad/prd.md), [UX 2.0](../bmad/ux-design.md),
[trust/disclosure rules](MND_TRUST_AND_DISCLOSURE_SPEC.md), and
[seven-flow overview](flows/index.html).

- Community and adoption remain central; cat-sale/paid-transfer information
  and appointments are a supporting capability.
- Individuals, businesses, catteries, and disclosed authorized agents may
  participate. Household keeping is not a trust prerequisite; hidden roles,
  impersonation, misleading locations, bait-and-switch cats/prices are the concern.
- Identity, the relationship to each cat, and verified evidence are distinct.
  Legitimate staff, foster homes, and hospital meeting locations need not match.
- Proposed navigation: 社区 / 领养 / 找猫 / 消息 / 我的. This remains a
  documented proposal pending Stitch mapping; clients were not changed here.
- Appointments keep immutable confirmation versions; key changes need renewed
  agreement. Refusal is an exit, not a buyer no-show.
- Verification discloses scope/method/time; video does not prove ownership,
  household origin, no intermediary, or future health.
- Initial no-payment/no-guarantee scope is a planning assumption, not a user
  decision to provide payment services. Adoption and buyer-visit metrics remain separate.
- The 2026-08 audit is historical evidence, not the current release backlog.

### Scope And Validation Of This Pass

Documentation and independent diagrams only, based on main at e4164ff and the
existing working tree. Unrelated Android/iOS changes were preserved. No builds,
project execution, tests, or device checks; no claims that new product flows
are already implemented. Static results are reported in the task handoff.

Static documentation checks found 34 unique functional requirements, 8 unique
non-functional requirements and 14 candidate stories; every functional
requirement has acceptance criteria and a story reference. Local Markdown
links, UTF-8/no-BOM and whitespace checks passed across the 11 task documents.
The seven SVG files parsed as XML; the overview's local targets, IDs and
JavaScript syntax passed static inspection. Browser rendering remains unverified.

## Historical Implementation Notes

The records below describe earlier work. Old Sugar/crimson, 8dp consumer-card,
three-mode, and commerce-first statements are historical, superseded by the
current PRD and Stitch V2 contract where they conflict.

## Decision

The product is now positioned as **M&D = meow & doggie**.

The emphasis is **cat-first**: cats lead the emotional identity, default imagery, feed content, profile examples, and community language. Doggie content is still part of the system, especially in marketplace, services, activities, and adoption.

## What Changed

- Rebranded the cute WebUI from the older Pawpop/MEOW direction to M&D.
- Updated the three design-board entries:
  - `web/pawpop.html`
  - `web/pawpop-mobile.html`
  - `web/pawpop-desktop.html`
  - `web/cute.html`
- Added missing desktop Figma-detail surfaces:
  - Search/results/empty state
  - Admin review detail with audit/action layout
- Reworked `web/stitch-theme-bridge.css` so existing functional pages use M&D colors, typography, radius, and overall cute token direction.
- Updated key existing page visible branding from MEOW/Kitty Circle/喵友圈 to M&D.
- Updated README/onboarding/docs to explain M&D and the cat-first/doggie-friendly rule.
- Added this memory doc and root `AGENTS.md` so future agents inherit the design context.
- Continued production Web migration after the redesign:
  - `web/market.html` / `web/market.js` now have M&D market hero, search/type filters, image cards, and static fallback listings.
  - `web/post.js` now renders an M&D demo post detail when opened without an id or backend.
  - `web/messages.js` now renders demo conversations when logged out or when conversation APIs are unavailable.
  - `web/profile.html` / `web/profile.js` now support M&D profile background switching and demo profile content.
- Second alignment pass:
  - `web/stitch-theme-bridge.css` now includes Honey/Mint/Night/Neutral M&D tokens, stronger card/button/form overrides, hidden prior decorative blur backgrounds, and shared M&D helpers.
  - `web/discover.html` / `web/discover.js` now have an M&D circle hero, real cat imagery, theme controls, and cat-first circle cards.
  - `web/compose.html` now has an M&D compose hero, theme controls, and cat-first placeholders/labels.
  - `web/login.html` and `web/register.html` now carry M&D brand panels and cat-first/doggie-friendly copy.
  - `web/dashboard.html`, `web/dashboard.css`, `web/admin.html`, and `web/admin.css` now use the M&D workbench/admin direction while preserving existing JS hooks and route behavior.
- Third alignment pass:
  - Expo mobile tokens now use the M&D palette, 8px panel radius, zero letter spacing, and M&D shadow color.
  - Expo visible surfaces now use M&D copy and cat-first defaults across splash/loading, auth, top bar, feed, discover, compose, market borders, messages, profile, and post detail.
  - The old `KittyLoader` export remains as a compatibility alias, but the visible loader is `MndLoader` and renders `M&D`.
  - Expo app metadata now uses the `M&D` name, `mnd` slug, and M&D cream splash/adaptive icon background.
  - KMP Android tokens now use M&D colors, 8dp shapes, zero letter spacing, and M&D pink shadows.
  - KMP Android visible surfaces now use M&D copy across manifest label, top bar, splash, login, register, feed/discover, compose, and post detail fallback identity.
  - Web design-board radii were tightened so ordinary mobile/desktop cards, inputs, panels, and chat bubbles use 8px; pills and device mock shells remain intentionally rounded.
- Documentation pass:
  - Added `docs/README.md` as the docs hub.
  - Added `docs/design/MND_UI_ALIGNMENT_GUIDE.md` as the current implementation-facing UI contract.
  - Added `docs/PROJECT_STRUCTURE.md` to map backend, Web, Expo, KMP, infra, migrations, and common edit entry points.
  - Added `docs/API_AND_DATA_FLOW.md` to map API routes, auth/admin behavior, core models, order state, clients, and persistence notes.
  - Added `docs/FRONTEND_SURFACE_MAP.md` to map production Web, WebUI, design boards, Expo routes, and KMP screens.
  - Added `docs/RUNBOOK.md` to capture local run modes, env vars, upload storage, verification matrix, deployment notes, and known local blockers.
  - Expanded `docs/design/MND_FIGMA_DESIGN_DOCUMENT.md` with current status and immediate Figma work order.
  - Historical design containers were later removed during the Stitch V2 reset.
- Runtime client interaction pass:
  - Added `docs/design/MND_CLIENT_INTERACTION_SPEC.md` as the page-by-page runtime client interaction contract.
  - Started KMP Android home/feed realignment from the Figma-ready M&D direction: compact top bar, editorial home header, restrained warm search field, better feed loading/empty/error states, less candy-pink Sugar palette, stronger middle compose tab, and image/placeholder-led feed cards.
  - KMP shared SDK now tolerates `items: null` for posts and conversations by treating it as an empty list, preventing Kotlin JSON serializer messages from appearing in the feed.

## Implementation Notes

- 2026-06-06 Stitch remote reset:
  - Current source of truth is Stitch project `projects/13275961100622290348`.
  - Downloaded remote assets now live in `.stitch/remote-assets`; browser-visible full-size screenshot and source copies live in `web/assets/stitch-remote`.
  - `.stitch/DESIGN.md` mirrors `MD_GLOBAL_STITCH_DESIGN_SYSTEM_V2_NEUTRAL_FIXED.md`.
  - Root `DESIGN.md` and `docs/design/MND_UI_ALIGNMENT_GUIDE.md` were rewritten around the Stitch V2 rules.
  - `web/pawpop.html`, `web/mnd-web-client-board.html`, `web/stitch-remote-gallery.html`, `web/cute.html`, `web/pawpop-desktop.html`, and `web/stitch-theme-bridge.css` now use the remote Honey/Mint/Night/Neutral system.
  - Earlier handmade reference exports, old Stitch maps, and prior-generation design docs were removed. Do not restore them as design sources.

- 2026-06-06 remote asset clarity fix:
  - `scripts/sync-stitch-remote-assets.ps1` now requests Googleusercontent screenshots with `=s0`, so local screenshots keep original Stitch canvas dimensions instead of 512px thumbnails.
  - Stitch HTML/SVG/Markdown sources are copied into both `.stitch/remote-assets/sources` and `web/assets/stitch-remote/sources`.
  - Current themes: Honey for Feed/Profile/Messages/Orders, Mint for Market/Product flows, Night for dark mode, Neutral for restrained docs, creator tools, dashboards, and explicit admin/moderation surfaces.
- 2026-06-06 AI handoff rule:
  - Added `docs/design/MND_STITCH_AI_RULES.md`, plus `CLAUDE.md` and `GEMINI.md`, so other AI agents know the current Stitch project and must follow the local Stitch mirror instead of inventing a new UI direction. Agents may suggest improvements, but implementation must map to the existing Stitch V2 rules unless the user approves a deviation.
- `web/cute.html` is now a static remote-aligned WebUI prototype for visual parity. It does not call the Go API.
- Auth localStorage keys in production scripts remain `meow_token` and `meow_user`; the current static WebUI prototype does not depend on auth.
- Theme localStorage keys for the current WebUI prototype are `mnd_theme` and `mnd_profile_bg`.
- Production-ish static fallbacks are intentional for design review. Use `?live=1` on pages that support it when validating against the Go API.
- Latest static UI verification used `python3 -m http.server 4173` from `web/` plus Playwright desktop/mobile checks for discover, compose, login, register, dashboard, admin, home, market, messages, profile, and post pages. No horizontal overflow or unhandled page errors were found in the checked states.
- Latest follow-up verification also checked 14 Web/UI sources at desktop `1440x1000` and mobile `390x844`: `/`, discover, compose, market, messages, profile, login, register, dashboard, admin, post, mobile board, desktop board, and WebUI. Checks passed for page errors, horizontal overflow, removed-brand visible terms, residual blur backgrounds, oversized non-action radii, and negative letter spacing.
- Do not run Gradle or Go build/run/test on this machine. Use static source review and browser/Web smoke checks for UI work.
- Current repository has WebUI, HTML desktop design-board sources, Expo mobile, KMP Android, and a KMP Compose Desktop client in `kmp/desktopApp`.

## How Future Agents Should Continue

- Start with `docs/README.md`.
- Use `docs/RUNBOOK.md` when you need to run, validate, or deploy the project.
- Use `docs/design/MND_UI_ALIGNMENT_GUIDE.md` as the source of truth for UI alignment.
- Preserve behavior before renaming internal compatibility identifiers.
- Prefer adding repeatable smoke-test scripts instead of relying only on manual browser checks.
- Keep this memory file updated after any meaningful design, implementation, or validation pass.

## Figma Handoff

There is no active `use_figma` tool in this session, so the repo contains Figma-ready source artifacts instead of a directly-written Figma file.

Recommended Figma pages:

- `00 Cover`
- `01 Product Map`
- `02 M&D Tokens`
- `03 Components`
- `04 Mobile App`
- `05 Desktop App`
- `06 WebUI`
- `07 Profile Customization`
- `08 Trust & Commerce`
- `09 Admin Console`
- `10 Prototypes`
- `11 Future Concepts`

Use the local HTML pages as visual source for Figma frame creation.
