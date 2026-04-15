# NCMD Agent Guidelines

Guidelines for coding agents working on NCMD (Android, Kotlin, Compose, Material 3).

## 1. Project Snapshot

- Architecture: single-Activity + Compose Navigation
- Entry: `MainActivity` -> `NCMDApp()`
- Navigation host: `ui/navigation/NavGraph.kt`
- Core screens: `ui/screens/*`
- Playback state: `api/PlayerManager.kt` (singleton)
- Session/theme settings: `api/SessionManager.kt`
- Local data: Room (`data/local/*`, `data/repository/*`)

## 2. Build / Test / Run

- Preferred build: `jetbrains_build_project`
- Gradle build: `./gradlew build`
- Unit tests: `./gradlew testDebugUnitTest`
- Single test: `./gradlew testDebugUnitTest --tests com.gem.neteasecloudmd.YourTest`

Use JetBrains tools for build/run/test when available.

## 3. Current UX Rules (Do Not Break)

- Playback bar is global overlay and hidden only on login route.
- Playback gestures:
  - Horizontal swipe on bar: switch songs
  - Vertical swipe: no-op
  - Long-press + hold: haptic feedback, then horizontal drag to seek
  - While long-press seeking: cover/info/controls fade out; progress guide shows
- Cover overflow behavior is configurable in Settings.

## 4. Coding Conventions

- Follow official Kotlin style (`kotlin.code.style=official`).
- Use Material 3 components and `MaterialTheme.colorScheme`.
- Keep composables stateless where possible; move screen state/network to ViewModel.
- Use `Result<T>` for API outcomes and show user-facing errors.
- Keep imports clean; no wildcard imports.

## 5. Architecture Rules

- Keep single-Activity architecture. Do not reintroduce multi-Activity flow.
- New screens must be added through `Screen.kt` and `NavGraph.kt`.
- Route params that may contain special characters must be URI-encoded.
- Avoid business/network logic directly inside large composables.

## 6. Security / Data

- Never log full cookies or secrets.
- Session data lives in `SessionManager`.
- If adding settings, persist in `SessionManager` and wire to UI reactively.

## 7. PR / Commit Checklist

- Build passes (`jetbrains_build_project`).
- Navigation works for changed routes.
- Playback bar behavior unchanged unless requested.
- No obsolete/dead files from old architecture.
- README and AGENTS updated when behavior/architecture changes.
