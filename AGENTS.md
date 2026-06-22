# Repository Guidelines

## Project Structure & Module Organization
This repository contains two independent apps plus planning assets:

- `android/`: native Android app built with Kotlin, Compose, Hilt, and Gradle.
- `server/`: NestJS backend with Prisma, MySQL, Redis, MinIO, and Swagger.
- `docs/`: architecture notes, development plan, test plan, and phase task files.
- `prd/`: product requirement documents.
- `prototype/`: HTML prototype assets.

Android source lives under `android/app/src/main/java/com/healthcare/family/...`. Server source lives under `server/src`, with Prisma schema in `server/prisma/schema.prisma` and tests in `server/test` plus `*.spec.ts`.

## Build, Test, and Development Commands
- `cd server && npm run start:dev`: run the backend locally on port `3000`; Swagger is at `/api/docs`.
- `cd server && npm run build`: compile NestJS to `server/dist`.
- `cd server && npm run lint`: run ESLint with auto-fix on TypeScript files.
- `cd server && npm run test`: run Jest unit tests.
- `cd server && npm run test:e2e`: run backend end-to-end tests.
- `cd server && npx prisma generate && npx prisma db push`: regenerate Prisma client and sync schema.
- `cd android && .\gradlew assembleDebug`: build the Android debug APK.
- `cd android && .\gradlew test`: run JVM unit tests.

## Coding Style & Naming Conventions
Use 2-space indentation in TypeScript and follow Prettier with `singleQuote` and trailing commas. Server lint rules live in `server/eslint.config.mjs`. Because the server uses `module: nodenext`, relative TypeScript imports must include `.js`.

Kotlin code should use `PascalCase` for screens and view models (`LoginScreen`, `AuthViewModel`), `camelCase` for members, and feature-first packages such as `ui/family` or `data/repository`.

## Testing Guidelines
Backend tests use Jest. Keep unit tests as `*.spec.ts`; keep e2e tests in `server/test/*e2e-spec.ts`. Run `npm run test:cov` when changing auth, Prisma, or shared response behavior. Android coverage is still light; add unit tests for non-trivial view model or repository logic.

## Commit & Pull Request Guidelines
Recent history follows short conventional commits such as `feat: ...`, `fix: ...`, and `ci: ...`, sometimes with a task marker like `P0-6:` or `(P0-7)`. Prefer `type: concise summary`, with the phase/task ID when relevant.

PRs should state scope, affected area (`android`, `server`, `docs`), linked task or issue, and verification steps. Include screenshots for Compose UI changes and request/response notes for API changes.

## Repository-Specific Notes
Treat `android/` and `server/` as separate build roots. Do not “clean up” the known `include ':helath'` typo in `android/settings.gradle` unless the task requires it.

## 交互语言

- **时区**：GMT+8
- **语言**：简体中文