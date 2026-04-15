# NCMD

NCMD is an Android music client built with Kotlin + Jetpack Compose + Material 3.

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose (single-Activity)
- OkHttp + Kotlin Serialization
- ExoPlayer (Media3)
- Room

## Architecture

- `MainActivity` hosts `NCMDApp()`
- App navigation is handled in `ui/navigation/NavGraph.kt`
- Screen UIs are in `ui/screens/*`
- Playback is managed by `api/PlayerManager.kt`
- Session + theme/settings persistence is in `api/SessionManager.kt`

## Main Features

- Multi-mode login: password / SMS captcha / cookie
- Playlist list and playlist detail playback
- Recent plays
- Global playback bar (hidden on login screen)
- Settings:
  - Theme mode (Light / Dark / Follow system)
  - Copy cookie
  - Logout
  - Toggle cover overflow in playback bar

## Playback Bar Gestures

- Swipe left/right: previous/next track
- Swipe up/down: no action
- Long-press and hold: haptic feedback, then drag left/right to seek
- During long-press seek: cover, metadata, and controls fade out

## Build

```bash
./gradlew build
```

Recommended in this repo: use JetBrains build tooling (`jetbrains_build_project`) when available.
