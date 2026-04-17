# NCMD Agent Guide

This guide is for coding agents working in the NCMD repository.

## 1) Project Overview

- Platform: Android (minSdk 31, targetSdk 36).
- Language: Kotlin.
- UI: Jetpack Compose + Material 3.
- Navigation: Navigation Compose, single-Activity architecture.
- Entry point: `app/src/main/java/com/gem/neteasecloudmd/MainActivity.kt`.
- App root composable: `NCMDApp()` in `app/src/main/java/com/gem/neteasecloudmd/App.kt`.
- Navigation graph: `app/src/main/java/com/gem/neteasecloudmd/ui/navigation/NavGraph.kt`.
- Screens: `app/src/main/java/com/gem/neteasecloudmd/ui/screens/*`.
- Playback core: `app/src/main/java/com/gem/neteasecloudmd/api/PlayerManager.kt`.
- API client: `app/src/main/java/com/gem/neteasecloudmd/api/NeteaseApiService.kt`.
- Session/settings persistence: `app/src/main/java/com/gem/neteasecloudmd/api/SessionManager.kt`.
- Local storage: Room in `app/src/main/java/com/gem/neteasecloudmd/data/*`.

## 2) Rules Discovery (Cursor/Copilot)

- Checked `.cursor/rules/`: not present.
- Checked `.cursorrules`: not present.
- Checked `.github/copilot-instructions.md`: not present.
- Therefore, this AGENTS.md is the primary agent instruction source in this repo.

## 3) Build, Lint, and Test Commands

Run commands from repo root (`/NCMD`).

### Build

- Full build:
  - `./gradlew build`
- Debug Kotlin compile only:
  - `./gradlew :app:compileDebugKotlin`
- Assemble debug APK:
  - `./gradlew :app:assembleDebug`
- Assemble release APK:
  - `./gradlew :app:assembleRelease`

### Lint

- Run lint for debug variant:
  - `./gradlew :app:lintDebug`
- Lint report location:
  - `app/build/reports/lint-results-debug.html`

### Unit Tests (JVM)

- Run all debug unit tests:
  - `./gradlew :app:testDebugUnitTest`
- Run a single test class:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest`
- Run a single test method:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest.addition_isCorrect`

### Instrumentation Tests (device/emulator)

- Run all connected tests:
  - `./gradlew :app:connectedDebugAndroidTest`
- Run a single instrumentation test class:
  - `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gem.neteasecloudmd.ExampleInstrumentedTest`

### Recommended Dev Loop

1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest`
3. `./gradlew :app:lintDebug`

## 4) Architecture Constraints

- Keep single-Activity architecture; do not add new Activities unless explicitly requested.
- New pages must be wired through `Screen.kt` and `NavGraph.kt`.
- Route arguments that can contain special characters must be URI-encoded/decoded.
- Keep business/network logic out of large composables; prefer ViewModel or manager classes.
- Maintain existing playback architecture around `PlayerManager` singleton.

## 5) Compose and UI Guidelines

- Use Material 3 components and `MaterialTheme.colorScheme` roles.
- Avoid hardcoded UI strings in Kotlin files; use `stringResource(R.string...)`.
- Keep composables as stateless as practical; pass state and callbacks from upper layers.
- Prefer small private composables for repeated UI blocks.
- Preserve existing UX behavior unless task explicitly changes it.

### Important Existing UX Behavior

- Playback bar is a global overlay and hidden on login route.
- Queue sheet supports play mode switching, item removal, and clear queue.
- Personal FM entry exists on home and should remain visible as designed.
- Theme and language settings are user-configurable and persisted.

## 6) Kotlin Style Guidelines

- Follow official Kotlin style (`kotlin.code.style=official`).
- Use explicit visibility/modifiers when it improves readability for non-trivial APIs.
- Avoid wildcard imports.
- Keep imports sorted and remove unused imports.
- Use `val` by default; use `var` only when mutation is required.
- Prefer immutable collections and data classes for state models.
- Use descriptive names:
  - Types: `UpperCamelCase`
  - Functions/properties/variables: `lowerCamelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Use nullable types intentionally; avoid force unwrap patterns.
- Prefer expression bodies for simple functions.

## 7) Error Handling and Logging

- API operations should return `Result<T>` where that pattern is already used.
- Surface user-facing errors via localized strings/resources.
- Avoid leaking secrets in logs (cookies, tokens, private IDs).
- Use `Log` for diagnostics but keep messages concise and non-sensitive.
- Handle timeout and fallback paths explicitly when networking can fail.

## 8) Internationalization (i18n)

- Primary string resources live in:
  - `app/src/main/res/values/strings.xml` (default/zh-CN in this project)
  - `app/src/main/res/values-zh-rTW/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
- Any new user-facing text must be added to all supported locales.
- Preserve placeholder formatting consistency (`%1$s`, `%1$d`) across locales.

## 9) Media, Assets, and Docs

- Chinese README default in repo root (`README.md`) with link to English version.
- Assets docs are under `app/src/main/assets/`:
  - `README_ZH.md`
  - `README_EN.md`
- Screenshot placeholders are under `app/src/main/assets/images/`.

## 10) Dependencies and Build Config

- Build script: `app/build.gradle.kts`.
- Version catalog: `gradle/libs.versions.toml`.
- Add dependencies through version catalog when practical; keep consistency with existing style.
- Do not modify signing/release process unless explicitly asked.

## 11) Git and Change Hygiene

- Do not commit generated caches (e.g., `.kotlin/`, `build/`).
- Keep commits scoped and meaningful.
- Before commit, run at least compile + relevant tests.
- If lint is enabled in the task, resolve introduced lint issues.

## 12) Pre-PR / Pre-Commit Checklist

- Build passes for touched module(s).
- Unit tests pass for affected logic.
- No new hardcoded UI strings.
- No unused imports or dead code from refactors.
- Navigation and route args still work.
- Playback behavior unchanged unless requested.
- Documentation updated when behavior changes.

## 13) Quick Single-Test Examples

- JVM single class:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest`
- JVM single method:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest.addition_isCorrect`
- AndroidTest single class:
  - `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gem.neteasecloudmd.ExampleInstrumentedTest`

Keep this file in sync with architecture/tooling changes.
