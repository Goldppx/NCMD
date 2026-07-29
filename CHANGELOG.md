# Changelog

All notable changes to NCMD are documented in this file.

## v0.0.6 — KMP foundation and verified releases

Released: 2026-07-29

- Added a Kotlin Multiplatform `shared` module with Android, Desktop JVM, and iOS targets.
- Moved platform-neutral lyrics, queue, playback-request, and sleep-timer policies into the shared core, with Desktop JVM tests.
- Added a GitHub Actions verification workflow for Android build, Android unit tests, Desktop tests, and Android lint.
- Added a signed, arm64-v8a Android Release workflow that publishes an update manifest for in-app update checks.
- Improved search request race handling, mini-player layout insets, player system-bar contrast, and GitHub update-check fallbacks.

## v0.0.5

- Previous Android release.
