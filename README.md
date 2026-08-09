[![Build Android](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20Android)] [![Build Web](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20Web)] [![Build iOS](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20iOS)] [![Deploy GitHub Pages](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Deploy%20GitHub%20Pages)] [![Deploy Cloudflare](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Deploy%20Cloudflare)]

A full-stack, cross-platform todo app built with Kotlin Multiplatform (Compose Multiplatform) and Ktor.

## Stages

| Stage | Job | Status |
|-------|-----|--------|
| Build | ⬜ Build Android | [![Build Android](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20Android)] |
| Build | ⬜ Build Web | [![Build Web](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20Web)] |
| Build | ⬜ Build iOS | [![Build iOS](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Build%20iOS)] |
| Deploy | ⬜ Deploy GitHub Pages | [![Deploy GitHub Pages](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Deploy%20GitHub%20Pages)] |
| Deploy | ⬜ Deploy Cloudflare | [![Deploy Cloudflare](https://github.com/Knchin/todo/actions/workflows/ci-cd.yml/badge.svg?branch=main&label=Deploy%20Cloudflare)] |

## Tech Stack

- **Client:** Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Web/Wasm)
- **Auth:** Supabase Auth
- **Database:** Postgres via Supabase (Exposed ORM)
- **Deploy:** GitHub Pages + Cloudflare Pages

## Getting Started

```bash
# Build the web (Wasm) app
./gradlew :composeApp:wasmJsBrowserDistribution

# Build Android APK
./gradlew :composeApp:assembleDebug

# Build iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64 :composeApp:linkDebugFrameworkIosArm64
```

> The `server` module has been removed. Backend uses Supabase Edge Functions (`supabase/functions/`).