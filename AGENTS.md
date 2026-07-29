# NCMD Agent Guide

This guide is for coding agents working in the NCMD repository.

## 1) Project Overview

- Platform: Android (minSdk 31, targetSdk 36), with a Kotlin Multiplatform shared core targeting Android, iOS, and Desktop.
- Language: Kotlin (Kotlin 2.0.0).
- UI: Jetpack Compose + Material 3 (Compose BOM 2025.12.00).
- Theme: Dynamic color (Material You) / custom seed color via `material-kolor`, with animated `ColorScheme` transitions.
- Navigation: Navigation Compose, single-Activity architecture.
- Entry point: `app/src/main/java/com/gem/neteasecloudmd/MainActivity.kt` — configures locale then calls `NCMDApp()`.
- App root composable: `NCMDApp()` in `app/src/main/java/com/gem/neteasecloudmd/App.kt`.
- Navigation graph: `app/src/main/java/com/gem/neteasecloudmd/ui/navigation/NavGraph.kt`.
- Route definitions: `app/src/main/java/com/gem/neteasecloudmd/ui/navigation/Screen.kt` (sealed class).
- Screens: `app/src/main/java/com/gem/neteasecloudmd/ui/screens/*`:
  - `LoginScreen.kt` — password/captcha/cookie login modes.
  - `MainScreen.kt` ~1241 lines — home, playlist queue, playback bar, drag gestures.
  - `PlayerScreen.kt` ~810 lines — full-screen player with lyrics, pager, landscape support.
  - `PlaylistDetailScreen.kt` / `PlaylistListScreen.kt` / `RecentPlaysScreen.kt` / `SearchScreen.kt` / `SettingsScreen.kt` / `LogScreen.kt`
- Shared UI components: `app/src/main/java/com/gem/neteasecloudmd/ui/components/*`:
  - `PlaybackQueueSheet.kt` / `SongLongPressMenu.kt` / `TrackCollectionScaffold.kt`
- Custom Toast: `app/src/main/java/com/gem/neteasecloudmd/ui/common/ToastExt.kt`
- Playback core: `app/src/main/java/com/gem/neteasecloudmd/api/PlayerManager.kt` — singleton with ExoPlayer + MediaSession + notification.
- Background playback: `app/src/main/java/com/gem/neteasecloudmd/api/PlaybackService.kt` — `MediaSessionService` that owns the Android foreground media-service lifecycle.
- API client: `app/src/main/java/com/gem/neteasecloudmd/api/NeteaseApiService.kt` — Netease Cloud Music weapi.
- Crypto: `app/src/main/java/com/gem/neteasecloudmd/api/CryptoUtil.kt` — AES/RSA encryption for weapi.
- Session/settings persistence: `app/src/main/java/com/gem/neteasecloudmd/api/SessionManager.kt`.
- Sleep timer: `app/src/main/java/com/gem/neteasecloudmd/api/SleepTimerPolicy.kt`.
- ViewModels: `app/src/main/java/com/gem/neteasecloudmd/ui/viewmodel/*`:
  - `MainViewModel.kt` / `PlaylistDetailViewModel.kt` / `PlaylistListViewModel.kt` / `RecentPlaysViewModel.kt` / `SearchViewModel.kt`
- Local storage: Room (`AppDatabase.kt`) with tables `recent_plays` and `current_playlist`, DAOs in `data/local/dao/`, entities in `data/local/entity/`.
- Repository: `com.gem.neteasecloudmd.data.repository.MusicRepository.kt`.
- Shared KMP core: `shared/src/commonMain/kotlin/com/gem/neteasecloudmd/core/` — cross-platform models, lyrics, queue rules, playback contract, and request policies.
- Utilities: `Logger.kt` (file + in-memory log) and `LyricParser.kt` (LRC parser).
- i18n: 3 locales — `values/` (zh-CN), `values-zh-rTW/`, `values-en/`.
- Dark theme colors for icons: `app/src/main/res/values-night/colors.xml`.

## 2) Rules Discovery (Cursor/Copilot)

- Checked `.cursor/rules/`: not present.
- Checked `.cursorrules`: not present.
- Checked `.github/copilot-instructions.md`: not present.
- Therefore, this AGENTS.md is the primary agent instruction source in this repo.

## 3) Build, Lint, and Test Commands

**IMPORTANT: Always use JetBrains MCP tools for compilation. Never use Gradle CLI directly.**

### Build (via JetBrains MCP)

- Use `jetbrains_build_project` tool to compile.
- Use `jetbrains_get_file_problems` to inspect file-level errors/warnings.
- Build confirmed passing with 0 errors (4 deprecation warnings as of last audit).

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

1. `jetbrains_build_project` (via MCP, NO Gradle CLI)
2. `./gradlew :app:testDebugUnitTest`
3. `./gradlew :app:lintDebug`

## 4) Architecture Constraints

- Keep single-Activity architecture; do not add new Activities unless explicitly requested.
- New pages must be wired through `Screen.kt` and `NavGraph.kt`.
- Route arguments that can contain special characters must be URI-encoded/decoded.
- Keep business/network logic out of large composables; prefer ViewModel or manager classes.
- Maintain existing playback architecture around `PlayerManager` singleton.
- Keep platform media engines behind the shared `PlaybackController` contract. Android uses Media3 through `PlayerManager`; future iOS/Desktop implementations must not leak platform playback APIs into shared code.
- LoginScreen is an exception — all login logic is in the composable, not a ViewModel.
- `ApiProvider` is a simple singleton factory for `NeteaseApiService`.

## 5) Compose and UI Guidelines

- Use Material 3 components and `MaterialTheme.colorScheme` roles.
- Avoid hardcoded UI strings in Kotlin files; use `stringResource(R.string...)`.
- Keep composables as stateless as practical; pass state and callbacks from upper layers.
- Prefer small private composables for repeated UI blocks.
- Preserve existing UX behavior unless task explicitly changes it.
- Theme supports dynamic color (Material You), cover palette seed color, animated transitions, and dark/light/system mode.
- PlayerScreen supports immersive landscape mode (status bar + navigation bar hidden).

### Important Existing UX Behavior

- Playback bar is a global overlay and hidden on login route.
- Playback bar supports swipe gestures: left/right to skip, up to open queue sheet, long-press + drag to seek.
- Queue sheet (`PlaybackQueueSheet`) supports play mode switching (sequential/shuffle/repeat-one), item removal, and clear queue.
- Personal FM entry exists on home and should remain visible as designed.
- Theme and language settings are user-configurable and persisted.
- Sleep timer supports presets (15/30/45/60 min), custom (1-240 min), and wait-for-queue-end mode.
- PlayerScreen has a pager (Now Playing / Lyrics) and a bottom queue sheet.

## 6) Kotlin Style Guidelines

- Follow official Kotlin style (`kotlin.code.style=official`).
- Use explicit visibility/modifiers when it improves readability for non-trivial APIs.
- **Avoid wildcard imports** — current codebase has ~22 wildcard imports that should be eliminated.
- Keep imports sorted and remove unused imports.
- Use `val` by default; use `var` only when mutation is required.
- Prefer immutable collections and data classes for state models.
- Use descriptive names:
  - Types: `UpperCamelCase`
  - Functions/properties/variables: `lowerCamelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Use nullable types intentionally; **avoid force unwrap (`!!`) patterns** — current codebase has 4 force unwraps in `PlayerManager.kt` and `LogScreen.kt` that should be fixed.

## 7) Error Handling and Logging

- API operations should return `Result<T>` where that pattern is already used.
- Surface user-facing errors via localized strings/resources.
- Avoid leaking secrets in logs (cookies, tokens, private IDs).
- Use custom `Logger` utility (in-memory StateFlow + file persistence) in addition to `android.util.Log`.
- Avoid mixing `Log` and `Logger` in the same class.
- Empty catch blocks are not acceptable; at minimum log the exception.
- Handle timeout and fallback paths explicitly when networking can fail.

## 8) Internationalization (i18n)

- Primary string resources live in:
  - `app/src/main/res/values/strings.xml` (default/zh-CN in this project) — 183 entries
  - `app/src/main/res/values-zh-rTW/strings.xml` — 183 entries
  - `app/src/main/res/values-en/strings.xml` — 183 entries
- Any new user-facing text must be added to all supported locales.
- Preserve placeholder formatting consistency (`%1$s`, `%1$d`) across locales.
- Dark theme launcher icon colors: `app/src/main/res/values-night/colors.xml`.

## 9) Media, Assets, and Docs

- Chinese README default in repo root (`README.md`) with link to English version.
- English README is at `assets/README_EN.md` (project root).

## 10) Dependencies and Build Config

- Build script: `app/build.gradle.kts`.
- Version catalog: `gradle/libs.versions.toml`.
- Add dependencies through version catalog when practical; keep consistency with existing style.
- Do not modify signing/release process unless explicitly asked.
- Key dependencies: Compose BOM 2025.12.00, OkHttp 4.12.0, Coil 2.6.0, Media3 1.3.0, Room 2.8.4, material-kolor 3.0.1.
- Room uses `fallbackToDestructiveMigration()` (deprecated variant) — data is lost on schema changes.

## 10.1) GitHub Actions

- `.github/workflows/ci.yml` verifies pull requests and `main` with Android debug build, JVM tests, desktop shared tests, and lint.
- `.github/workflows/release.yml` publishes only version tags (`v*`) or an explicit manual release. It creates a signed arm64-v8a APK, SHA-256 sidecar, and `update.json` release asset.
- Release signing is supplied only through GitHub Actions Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Never commit a keystore or credential file.
- `update.json` is the preferred update-check source; it is a CDN-backed Release asset and avoids GitHub REST API rate limits. The app falls back to API/web lookup for older releases without this asset.

## 11) Git and Change Hygiene

- Do not commit generated caches (e.g., `.kotlin/`, `build/`).
- Keep commits scoped and meaningful.
- Before commit, run at least compile + relevant tests.
- If lint is enabled in the task, resolve introduced lint issues.
- `.idea/` directory files have been deleted and added to `.gitignore`.

## 12) Pre-PR / Pre-Commit Checklist

- [ ] Build passes for touched module(s) — via `jetbrains_build_project` (JetBrains MCP, NOT Gradle CLI).
- [ ] Unit tests pass for affected logic.
- [ ] No new hardcoded UI strings.
- [ ] No unused imports or dead code from refactors.
- [ ] No wildcard imports introduced.
- [ ] No force unwrap (`!!`) introduced.
- [ ] Navigation and route args still work.
- [ ] Playback behavior unchanged unless requested.
- [ ] Documentation updated when behavior changes.

## 13) Quick Single-Test Examples

- JVM single class:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest`
- JVM single method:
  - `./gradlew :app:testDebugUnitTest --tests com.gem.neteasecloudmd.ExampleUnitTest.addition_isCorrect`
- AndroidTest single class:
  - `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gem.neteasecloudmd.ExampleInstrumentedTest`

## 14) Known Deprecation Warnings (from last clean build)

| File | Warning | Fix |
|------|---------|-----|
| `AppDatabase.kt:31` | `fallbackToDestructiveMigration()` deprecated | Use overload with `dropAllTables` parameter |
| `ToastExt.kt:37` | `Toast.view` deprecated in Java | Migrate to Snackbar or Material3 style |
| `Theme.kt:111` | `window.statusBarColor` deprecated | Use `WindowInsetsControllerCompat` API |
| `Theme.kt:171` | `ColorScheme` constructor needs `fixed*` container roles | Add `fixedDim`/`fixedBright` etc. to constructor call |

Keep this file in sync with architecture/tooling changes.
