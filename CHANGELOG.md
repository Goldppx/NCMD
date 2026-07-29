# Changelog

All notable changes to NCMD are documented in this file.

## v0.0.8 — Desktop playback and installable packages

Released: 2026-07-29

- Added desktop local-library playback with seeking, previous/next track controls, and cover metadata.
- Added MP3, FLAC, Ogg Vorbis, WAV, AIFF, and AU decoding for the desktop local library.
- Unified desktop music and folder selection behind the system file-picker integration.
- Added a live light/dark theme toggle to the desktop navigation rail.
- Added thin Windows MSI packaging, alongside verified Linux and Arch Linux desktop packages.

## v0.0.7 — KMP foundation and verified releases

Released: 2026-07-29

- Added a Kotlin Multiplatform `shared` module with Android, Desktop JVM, and iOS targets.
- Moved platform-neutral lyrics, queue, playback-request, and sleep-timer policies into the shared core, with Desktop JVM tests.
- Added a GitHub Actions verification workflow for Android build, Android unit tests, Desktop tests, and Android lint.
- Added a signed, arm64-v8a Android Release workflow that publishes an update manifest for in-app update checks.
- Improved search request race handling, mini-player layout insets, player system-bar contrast, and GitHub update-check fallbacks.

## v0.0.5

- Previous Android release.
