# M&D Mobile

A cross-platform (iOS + Android + Web) client for **M&D = meow & doggie**,
built with **Expo SDK 52**, **React Native 0.76** (new architecture),
**TypeScript**, and **Expo Router**.

The product is cat-first and doggie-friendly. Runtime UI follows the Stitch V2
contract: one active global theme at a time, chosen from Honey, Mint, Night, or
Neutral.

## Why Expo and not bare React Native

| Feature | Expo | Bare RN |
| --- | --- | --- |
| iOS cloud builds | EAS Build | Usually needs a Mac |
| OTA fixes | `eas update` | DIY |
| File-based routing | Expo Router | DIY / react-navigation |
| Web output | `expo export --platform web` | RNW setup |
| Typed routes | `experiments.typedRoutes` | Manual |
| SecureStore / media picker | First-party modules | Community packages |

You can eject later with `npx expo prebuild` if a native limitation appears.

## Structure

```text
mobile/
├── app/
│   ├── _layout.tsx
│   ├── index.tsx
│   ├── (auth)/
│   │   ├── _layout.tsx
│   │   ├── login.tsx
│   │   └── register.tsx
│   └── (tabs)/
│       ├── _layout.tsx
│       ├── index.tsx
│       ├── market.tsx
│       ├── listing/[id].tsx
│       ├── messages/
│       ├── orders.tsx
│       ├── order/[id].tsx
│       ├── compose.tsx
│       └── profile.tsx
└── src/
    ├── api.ts
    ├── auth.tsx
    ├── components.tsx
    ├── href.ts
    └── theme.ts
```

## Setup

```bash
cd mobile
npm install
cp .env.example .env
npm start
```

Then press `i` for iOS simulator on macOS, `a` for Android emulator, or `w` for
web.

### API base URL

The app reads `EXPO_PUBLIC_API_URL` at bundle-build time. If omitted,
`src/api.ts` falls back to Expo's LAN host for devices and
`http://10.0.2.2:8080` for Android emulators.

## Design system

`src/theme.ts` mirrors the Stitch V2 M&D token system:

- Themes: Honey, Mint, Night, Neutral.
- Runtime rule: exactly one active global theme; do not hardcode page-specific
  Feed/Market colors.
- Radius: compact Stitch panels/cards use 8px on this client.
- Copy: visible brand stays `M&D`, supporting copy stays `meow & doggie`.
- Content: cats lead feed/profile identity; doggie content belongs in services,
  activities, marketplace, adoption, and secondary stories.

Compose supports post creation, listing creation, and image/video selection via
`expo-image-picker`. Uploaded media is sent to `POST /api/v1/media` and linked
by `media_ids`.

## Building

Once you have an EAS account:

```bash
npm i -g eas-cli
eas login
eas build:configure
eas build --platform android
eas build --platform ios
```

From the repo root, the Makefile aliases are:

```bash
make package-rn-web
make package-rn-android
make package-rn-android-eas
make package-rn-ios        # macOS/Xcode local build
make package-rn-ios-eas
```

Shipping updates without re-submitting to stores:

```bash
eas update --branch main --message "fix: mobile copy"
```

## Scripts

| Script | What it does |
| --- | --- |
| `npm start` | Launch Expo dev server |
| `npm run android` | Dev server + target Android |
| `npm run ios` | Dev server + target iOS |
| `npm run web` | Dev server + target web |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run fix-deps` | `expo install --fix` to realign SDK peers |

## Local verification notes

This repo's agent rules forbid running local Go build/run/test and Gradle on
the current machine. For mobile work here, prefer:

```bash
npm --prefix mobile run typecheck
```
