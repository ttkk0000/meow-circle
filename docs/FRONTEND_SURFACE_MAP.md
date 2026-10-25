# Frontend Surface Map

Last updated: 2026-10-25

This document maps all user-visible frontend surfaces across the **2026-10-25 v2 multi-platform clients** (Figma V2 Default Light Contract, 740 cross-platform states, `kmp/clientUi` 34-file shared Compose UI, `42` `FeaturePage` routes) and the legacy Stitch V2 static/v1 surfaces.

## 0. Current v2 Consumer, Operator & Public Surfaces (2026-10-25 Baseline)

| Surface | Path / Entry | Tech Stack & Auth | Current Scope |
| --- | --- | --- | --- |
| **Shared KMP Compose UI (`kmp/clientUi`)** | `kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/` (`34` common files: `ConsumerApp.kt`, `FeaturePages.kt`, `PageComponents.kt`, `StatusComponents.kt`, etc.) | Compose Multiplatform 1.10.2 + `kmp/shared/.../client` (`MviStore`, `MobileStore`, `MobileFeatureStore`) | Shared across Android, iOS, and Desktop: 5 primary tabs + `42` `FeaturePage` routes + adaptive `Compact` / `Medium` / `Expanded` layouts + `ClientUiPlatform` port. |
| **Android Phone & Tablet (v2)** | `kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/` (`AndroidMobileApp.kt`, `AndroidMviBase.kt`, `AndroidClientPlatform.kt`, `AndroidNativeMap.kt`, `AndroidProviderAuth.kt`) | Jetpack Compose + `kmp/clientUi` + AMap 3D SDK `11.3.100`, Opaque Bearer pair | Full 5 primary tabs + `42` `FeaturePage`s + native AMap `MapView` + WeChat/QQ native provider auth + EncryptedSharedPreferences vault (API 26–37). |
| **iPhone & iPad (v2)** | `kmp/iosUi/` (`IosClientPlatform.kt`, `IosNativeMap.kt`, `IosProviderAuth.kt`) + `kmp/iosApp/ComposeApp/` (`AmapNativeMap.swift`, `NativeProviderAuth.swift`) | Compose Multiplatform for iOS + `kmp/clientUi` + CocoaPods `AMap3DMap 11.2.100` | Full 5 primary tabs + `42` `FeaturePage`s + native `MAMapView` + Sign in with Apple / WeChat / QQ bridge + Keychain vault. |
| **Desktop App (v2)** | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/` (`Main.kt`, `DesktopClientPlatform.kt`, `DesktopStorage.kt`, `LostMapScreen.kt`) | Compose Desktop (JVM 21) + `kmp/clientUi` + `java-keyring:1.0.4` AES-256-GCM | Full desktop multi-pane workspace (`Expanded` / `Medium` / `Compact`), OS keyring encrypted command vault, and `/app/map` browser coordinate handoff. |
| **Web Consumer App (v2)** | `web-client/` (`src/App.tsx`, `src/maps/AmapView.tsx`, `src/features/ProviderSignIn.tsx`, `src/features/GuardianPages.tsx`, `src/features/AgePages.tsx`) served at `/app/` | React 19 + TypeScript 5.8 + Vite 6, Browser `HttpOnly Cookie + X-MND-CSRF`, lossless string IDs | Full v2 consumer web parity across 740 tracked states, AMap JS API 2.0 via `/maps/amap/_AMapService/`, browser OAuth redirect/callback, and IndexedDB AES-GCM vault. |
| **Web Operator Workbench (v2)** | `web-client/src/operator/OperatorApp.tsx` served at `/app/operator` | React + in-memory named operator credentials (`credentials: 'omit'`) | Isolated operator console (`ops_reviewer`, `safety_specialist`, `privacy_officer`, `identity_verifier`) for moderation, safety holds, guardian invites, and adult relationship claim reviews. |
| **Guest Safety & Public Share Web (v2)** | `web/safety.html` (`web/safety.js`, `web/safety.css`) & `internal/platform/mobileapi/public_web.go` | Vanilla HTML/JS + Go SSR share metadata | Unauthenticated emergency pet safety / privacy / minor protection reporting (`/api/v2/safety-reports`) and HTTPS deep-link handoff cards. |

> For detailed v2 code logic, UML diagrams, UI components, and Figma 740-state trace, see [Code Logic & UML Reference](CODE_LOGIC_AND_UML_REFERENCE.md), [PlantUML Diagrams](uml/README.md), [Client README](client/README.md), and [All Figma Implementation Handoff (2026-10-24)](client/ALL_FIGMA_IMPLEMENTATION_2026_10_24.md). The sections below document the earlier Stitch V2 static and `/api/v1` surfaces.

## 1. Surface Families

| Family | Implementation | Status |
| --- | --- | --- |
| Production Web | Static HTML/CSS/JS under `web/` served by Go | M&D-aligned through page updates and `stitch-theme-bridge.css`. |
| WebUI prototype | `web/cute.html`, `web/cute-ui.css`, `web/cute-ui.js` | Independent M&D browser product experience. |
| Stitch V2 mirror | `.stitch/remote-assets`, `web/assets/stitch-remote`, `web/mnd-web-client-board.html`, `web/stitch-remote-gallery.html` | Full-size remote screenshots, source files, and local parity boards. |
| Mobile design board | `web/pawpop-mobile.html` | Figma source board, not runtime app code. |
| Desktop design board | `web/pawpop-desktop.html` | Figma source board and current desktop deliverable. |
| Expo app | `mobile/app`, `mobile/src` | Runtime mobile app aligned to M&D. |
| KMP Android app | `kmp/androidApp` + `kmp/shared` | Runtime Android Compose app aligned to M&D in source. |
| KMP Desktop app | `kmp/desktopApp` + `kmp/shared` | Runtime Compose Desktop app aligned to M&D in source. |

## 2. Production Web Pages

| Page | Script | Styles | API Dependencies | Notes |
| --- | --- | --- | --- | --- |
| `/` / `index.html` | `app.js` | `theme.css`, `stitch-theme-bridge.css`, `styles.css` | posts, listings, auth state, search/profile preview | Public home/feed; static fallback behavior exists for review. |
| `/discover.html` | `discover.js` | `theme.css`, `stitch-theme-bridge.css` | mostly static, links to posts/market | M&D circles page with real cat imagery. |
| `/compose.html` | `compose.js` | bridge + toast | auth, posts, media | Redirects unauthenticated users to login. |
| `/market.html` | `market.js` | bridge + toast | listings, optional static fallback | M&D market hero, filters, listing cards. |
| `/post.html` | `post.js` | bridge + toast | post detail, comments, follow, report | Demo post when opened without backend/id. |
| `/messages.html` | `messages.js` | bridge + toast | conversations, messages | Demo conversations when logged out/API unavailable. |
| `/profile.html` | `profile.js` | bridge + toast | user profile, my posts, update profile | Supports profile background picker. |
| `/login` / `/login.html` | `auth.js` | bridge + toast | login | Pretty route served by Go; static html also works. |
| `/register` / `/register.html` | `auth.js` | bridge + toast | register, phone code | Pretty route served by Go; static html also works. |
| `/dashboard` / `/dashboard.html` | `dashboard.js` | bridge, `styles.css`, `dashboard.css` | auth, posts, listings, media, orders, messages, notifications | Logged-in creator/seller workbench. |
| `/admin` / `/admin.html` | `admin.js` | bridge, `admin.css` | admin summary/posts/comments/listings/media/reports/orders/audit | Requires `X-Admin-Key`. |
| `/architecture.html` | `architecture.js` | bridge, `architecture.css` | `architecture-data.json` | Visual architecture page. |

Shared Web helpers:

- `web/shared.js`: i18n, theme/language storage, toast helpers, auth helpers, static screen IDs.
- `web/tw-stitch.js`: Tailwind CDN token config, now mapped to M&D.
- `web/stitch-theme-bridge.css`: visual compatibility layer for old pages.
- `web/stitch-toast.css`: toast styling.

## 3. WebUI Prototype

Files:

- `web/cute.html`
- `web/cute-ui.css`
- `web/cute-ui.js`

Core flows represented:

- Home/feed and search.
- Post detail.
- Discover/circles.
- Market/listings.
- Messages.
- Auth.
- Profile/background customization.
- Compose post/listing.
- Media/orders/notifications/safety concepts.

Behavior:

- Static-safe by default.
- Uses live backend only with `?live=1` where implemented.
- Persists theme with `mnd_cute_theme`.
- Persists profile background with `mnd_cute_profile_bg`.

## 4. Design Boards

| Board | Purpose |
| --- | --- |
| `web/pawpop.html` | Overview of the three UI sets. |
| `web/mnd-web-client-board.html` | Current Stitch V2 sync board with mobile, theme, component, and implementation contract references. |
| `web/stitch-remote-gallery.html` | Full-size remote screenshot gallery with local source links. |
| `web/pawpop-mobile.html` | Native mobile-like screen inventory for Figma. |
| `web/pawpop-desktop.html` | Desktop client/workbench design inventory for Figma. |

Rules:

- Do not treat mobile board as responsive Web.
- Do not treat desktop board as widened mobile.
- Use `docs/design/MND_UI_ALIGNMENT_GUIDE.md` for tokens/copy/QA.

## 5. Expo Route Map

| Route | File | Purpose |
| --- | --- | --- |
| Root layout | `mobile/app/_layout.tsx` | Font loading, splash/loading, `AuthProvider`, route stack. |
| Auth gate | `mobile/app/index.tsx` | Opens M&D loading state and redirects by auth status. |
| Login | `mobile/app/(auth)/login.tsx` | M&D login with cat-first copy. |
| Register | `mobile/app/(auth)/register.tsx` | M&D onboarding/register. |
| Tabs layout | `mobile/app/(tabs)/_layout.tsx` | Bottom tab bar. |
| Feed | `mobile/app/(tabs)/index.tsx` | Feed, search, hero, filter tabs. |
| Discover | `mobile/app/(tabs)/discover.tsx` | Circles and market entry. |
| Compose | `mobile/app/(tabs)/compose.tsx` | Create post. |
| Market | `mobile/app/(tabs)/market.tsx` | Market/listings. |
| Messages list | `mobile/app/(tabs)/messages/index.tsx` | Conversation list. |
| Conversation | `mobile/app/(tabs)/messages/[peerId].tsx` | Message thread. |
| Post detail | `mobile/app/(tabs)/post/[id].tsx` | Post detail/comments/actions. |
| Profile | `mobile/app/(tabs)/profile.tsx` | Profile and user content. |

## 6. KMP Android Screen Map

| Screen | File | Purpose |
| --- | --- | --- |
| App shell | `MeowApp.kt` | Session restore, auth switch, compose/detail/feed navigation. |
| Splash | `StitchSplashScreen.kt` | M&D loading state. |
| Login | `StitchLoginScreen.kt` | M&D login. |
| Register | `StitchRegisterScreen.kt` | M&D registration. |
| Feed/Discover tabs | `StitchFeedScreen.kt` | Main authenticated app. |
| Compose | `StitchComposeScreen.kt` | Create post. |
| Post detail | `StitchPostDetailScreen.kt` | Detail, media pager, comments, follow. |
| Loading | `StitchLoadingScreen.kt` | Shared loading state. |
| Components | `ui/components/*` | Top bar, bottom nav, feed card, FAB, search field. |

Internal file names still include `Stitch` because they began as imported
reference surfaces. The visible UI is now M&D.

## 7. KMP Desktop Screen Map

| Surface | File | Purpose |
| --- | --- | --- |
| Desktop app shell | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/Main.kt` | Dense desktop feed/workspace layout with theme switching and Stitch refs. |
| Desktop tokens | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/MndDesktopTheme.kt` | Honey, Mint, Night, and Neutral token set aligned to Stitch V2. |
| Desktop Stitch refs | `kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/StitchDesktopReferences.kt` | Project, desktop screen, and theme screen IDs. |

## 8. Visual QA Matrix

| Surface | Desktop | Mobile | Notes |
| --- | --- | --- | --- |
| Web production pages | `1440x1000` | `390x844` | Smoke test routes listed in `MND_UI_ALIGNMENT_GUIDE.md`. |
| WebUI prototype | `1440x1000` | responsive check | Verify theme/profile switching. |
| Mobile board | browser desktop | browser mobile optional | Board is a Figma source, not app runtime. |
| Desktop board | browser desktop | not required | Verify dense desktop panes, search states, admin detail. |
| Expo | simulator/device | device-first | Static source review and visual QA when a safe Node environment is available. |
| KMP Android | emulator/device | Android | Static source review only on this machine; do not run Gradle. |
| KMP Desktop | desktop JVM | desktop | Static source review only on this machine; do not run Gradle. |

## 9. Common Frontend Risks

- Static Web pages may call `/api/v1/*` and receive 404 when served by a plain
  static server. That is acceptable if demo fallback keeps the page usable.
- Pretty routes like `/login` and `/dashboard` are served by Go. In static
  server mode, use `.html` paths or tolerate redirect 404s during smoke checks.
- Tailwind classes like `rounded-2xl` may remain in source, but
  `stitch-theme-bridge.css` must make non-action surfaces render at 8px.
- Do not remove static fallbacks unless the local Go backend is guaranteed
  during design review.
- Do not rename Web auth storage keys without migrating all page scripts.
