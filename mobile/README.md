# Meow Circle Mobile

A cross-platform (iOS + Android + Web) client for the Meow Circle Go
backend, built with **Expo SDK 52**, **React Native 0.76** (new
architecture), **TypeScript** and **Expo Router**.

## Why Expo and not "bare" React Native

| Feature                  | Expo                            | Bare RN              |
| ------------------------ | ------------------------------- | -------------------- |
| iOS builds from Windows  | ✅ via EAS Build (cloud)         | ❌ need a Mac         |
| OTA (over-the-air) fixes | ✅ `eas update`                  | 🟡 DIY CodePush      |
| File-based routing       | ✅ Expo Router                   | 🟡 DIY / react-navigation |
| Web output               | ✅ same codebase → `expo export --platform web` | 🟡 RNW manual setup |
| Typed routes             | ✅ `experiments.typedRoutes`     | 🟡                    |
| SecureStore / Fonts      | ✅ first-party modules           | 🟡 community pkgs     |

You can always "eject" (`npx expo prebuild`) later if you hit a limit.

## Structure

```
mobile/
├── app/                     # Expo Router — files = routes
│   ├── _layout.tsx          # Root stack + <AuthProvider>
│   ├── index.tsx            # Auth-gate redirect
│   ├── (auth)/              # Public group
│   │   ├── _layout.tsx
│   │   ├── login.tsx
│   │   └── register.tsx
│   └── (tabs)/              # Private group (tab bar)
│       ├── _layout.tsx
│       ├── index.tsx        # Feed
│       ├── market.tsx
│       ├── messages.tsx
│       └── profile.tsx
└── src/
    ├── api.ts               # typed fetch client + token storage
    ├── auth.tsx             # <AuthProvider> + useAuth()
    ├── components.tsx       # Screen, Card, Button, Input, Txt, Pill
    └── theme.ts             # Cursor Design System tokens
```

## Setup

```bash
cd mobile
npm install          # or: pnpm i / bun i / yarn
cp .env.example .env # point EXPO_PUBLIC_API_URL at your Go backend
npm start            # opens the Expo DevTools
```

Then:

- Press **`i`** to launch the iOS simulator (macOS only).
- Press **`a`** to launch an Android emulator.
- Press **`w`** to launch the web build in your browser.
- Scan the QR code with the [Expo Go](https://expo.dev/client) app on
  your phone for hot-reload on-device.

### Pointing at a local backend

The app reads `EXPO_PUBLIC_API_URL` at bundle-build time. If you omit it,
`src/api.ts` falls back to `http://<lan-host>:8080` so Expo Go on your
phone can hit your dev machine. For **Android emulator** specifically
use `http://10.0.2.2:8080`.

To run the backend from the repo root:

```bash
# repo root
make run           # in-memory store, no external deps
# …or with Postgres + Redis:
make up            # docker compose: postgres + redis
DATABASE_URL=postgres://meow:meowpass@localhost:5432/meow?sslmode=disable \
REDIS_URL=redis://localhost:6379/0 \
make run
```

## Design system

`src/theme.ts` mirrors `web/theme.css`: warm-cream surfaces, near-black
ink, crimson hover/destructive, diffused shadows. The three-font system
currently resolves to platform defaults (`System` / `Georgia` / `Menlo`);
swap in the Cursor trio (Söhne / Source Serif / JetBrains Mono) via
`expo-font` when the assets are ready.

## Building for stores

Once you have an [EAS](https://expo.dev) account:

```bash
npm i -g eas-cli
eas login
eas build:configure
eas build --platform android  # cloud build, no local Gradle needed
eas build --platform ios      # cloud build, no Mac needed
```

Shipping updates without re-submitting to the stores:

```bash
eas update --branch main --message "fix: typo in feed"
```

## Scripts

| Script            | What it does                               |
| ----------------- | ------------------------------------------ |
| `npm start`       | Launch Expo dev server                     |
| `npm run android` | Dev server + target Android                |
| `npm run ios`     | Dev server + target iOS                    |
| `npm run web`     | Dev server + target web                    |
| `npm run typecheck` | `tsc --noEmit`                           |
| `npm run fix-deps` | `expo install --fix` — realign SDK peers  |
