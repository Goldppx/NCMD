# GEMINI.md - NCMD Project Context

This file provides essential context and instructions for AI agents working on the NCMD project.

## Project Overview

**NCMD** is a modern Android music client for Netease Cloud Music, built with Kotlin and Jetpack Compose. It follows a single-Activity architecture and adheres to Material 3 design principles.

- **Primary Technologies:** Kotlin, Jetpack Compose, Material 3, Navigation Compose.
- **Media Playback:** Media3 (ExoPlayer, Session, Notification).
- **Networking:** OkHttp + Kotlin Serialization.
- **Local Storage:** Room Database.
- **Architecture:** MVVM (Model-View-ViewModel) with a central `PlayerManager` for playback state.

## Project Structure

- **Entry Point:** `app/src/main/java/com/gem/neteasecloudmd/MainActivity.kt` -> calls `NCMDApp()` in `App.kt`.
- **Navigation:** `app/src/main/java/com/gem/neteasecloudmd/ui/navigation/NavGraph.kt` (Routes defined in `Screen.kt`).
- **UI Screens:** `app/src/main/java/com/gem/neteasecloudmd/ui/screens/`.
- **Playback Logic:** `app/src/main/java/com/gem/neteasecloudmd/api/PlayerManager.kt`.
- **API Client:** `app/src/main/java/com/gem/neteasecloudmd/api/NeteaseApiService.kt`.
- **Session & Settings:** `app/src/main/java/com/gem/neteasecloudmd/api/SessionManager.kt`.
- **Data Layer:** `app/src/main/java/com/gem/neteasecloudmd/data/` (Room entities, DAOs, and repositories).
- **Resources:** `app/src/main/res/` (Layouts, strings, drawables).

## Building and Running

Commands should be run from the project root.

- **Build Project:** `./gradlew build`
- **Compile Debug Kotlin:** `./gradlew :app:compileDebugKotlin`
- **Assemble Debug APK:** `./gradlew :app:assembleDebug`
- **Run Unit Tests:** `./gradlew :app:testDebugUnitTest`
- **Run Lint:** `./gradlew :app:lintDebug`

## Development Conventions

### Technical Integrity (Mandatory)
- **Mandatory Build:** After EVERY code modification, you MUST use the `mcp_jetbrains-android_build_project` tool (or equivalent Gradle command) to compile the project.
- **Error Resolution:** You MUST review all build errors and warnings. Any introduced errors MUST be fixed before the task is considered complete.
- **Validation:** Finality is only achieved when the project builds successfully and behavioral correctness is verified.

### UI & UX
- **Material 3:** Use Material 3 components and respect `MaterialTheme.colorScheme`.
- **Edge-to-Edge:** Support Edge-to-Edge UI; use `WindowInsets` or `Scaffold` padding to handle system bars.
- **Theming:** Ensure UI reacts to dynamic theming (Light/Dark and dynamic color) by avoiding hardcoded colors.
- **Localization:** **NEVER** hardcode user-facing strings in Kotlin files. Use `stringResource(R.string...)` and update all `strings.xml` files (`values/`, `values-en/`, `values-zh-rTW/`).

### Architecture & Logic
- **Single Activity:** Do not add new Activities unless explicitly requested.
- **Navigation:** All new screens must be added to `Screen.kt` and wired in `NavGraph.kt`. Encode/decode URI arguments for safety.
- **State Management:** Keep composables stateless where possible; use ViewModels for state and logic.
- **API Patterns:** Use `Result<T>` for network operations and handle errors gracefully.
- **Playback:** Maintain the `PlayerManager` singleton architecture for all playback-related tasks.

### Code Style
- **Kotlin:** Follow official Kotlin style. Use `val` by default, avoid wildcard imports, and prefer immutable data structures.
- **Naming:** `UpperCamelCase` for types, `lowerCamelCase` for functions/variables, `UPPER_SNAKE_CASE` for constants.
- **Cleanliness:** Remove unused imports and dead code. Ensure all new code is properly documented if non-trivial.

## Testing Strategy
- **Unit Tests:** Add unit tests for business logic in `app/src/test/`. Use JUnit, MockK, and Turbine (for Flow testing).
- **Validation:** Always run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest` before considering a task complete.

## Security
- **Secrets:** Do not log or commit sensitive information like cookies, tokens, or private user IDs.
- **Logging:** Use the project's `Logger` or `Log` class for diagnostics, keeping messages concise.
