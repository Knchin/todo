# Todo

A full-stack, cross-platform todo app built with Kotlin Multiplatform (Compose Multiplatform) and Ktor.

## Live apps

- **GitHub Pages:** <https://knchin.github.io/todo/>
- **Cloudflare Pages:** <https://CLOUDFLARE_PROJECT_NAME.pages.dev/>

## Badges

| Badge | Link |
| --- | --- |
| [![Build](https://github.com/Knchin/todo/actions/workflows/build.yml/badge.svg)](https://github.com/Knchin/todo/actions/workflows/build.yml) | CI build (Android, Web, iOS, Server) |
| [![Deploy](https://github.com/Knchin/todo/actions/workflows/deploy.yml/badge.svg)](https://github.com/Knchin/todo/actions/workflows/deploy.yml) | Deploy to GitHub Pages & Cloudflare Pages |

## Tech stack

- **Client:** Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Web/Wasm)
- **Server:** Ktor (Kotlin JVM)
- **Auth:** Session cookies with JWT (self-hosted) or Supabase Auth
- **Database:** Postgres via Supabase (Exposed ORM)
- **Realtime:** WebSockets (self-hosted) or Supabase Realtime

## Modules

- `composeApp` – the Compose Multiplatform UI (Android, iOS, Web)
- `server` – the Ktor backend and REST/WebSocket API
- `shared` – shared models, validation, and permission rules across client & server
- `db` – Supabase infrastructure (SQL schema / migrations)

## Getting started

```bash
# Build the web (Wasm) app
./gradlew :composeApp:wasmJsBrowserDistribution

# Run the server locally (uses Supabase env vars, see .github/workflows/build.yml)
./gradlew :server:installDist
./server/build/install/server/bin/server
```

> Replace `CLOUDFLARE_PROJECT_NAME` above with the value of the
> `CLOUDFLARE_PROJECT_NAME` repository variable to get the real Cloudflare URL.
