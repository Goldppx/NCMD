# NCMD

For the Chinese document, see: `README_ZH.md`

NCMD is an Android music client built with Kotlin + Jetpack Compose + Material 3.

## Key Features

- Multiple login modes: password / SMS captcha / cookie
- Home aggregation: Personal FM, recent plays, and playlists
- Search: songs / playlists / albums + suggestions
- Global playback bar (hidden on login screen)
- Queue controls: sequential/shuffle/repeat-one, remove track, clear queue
- Native media notification controls (prev / play-pause / next)
- Theme support: light / dark / system + cover-based dynamic color
- Language switcher: system / Simplified Chinese / Traditional Chinese / English

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose (single-Activity architecture)
- OkHttp + Kotlin Serialization
- Media3 (ExoPlayer / Session / Notification)
- Room

## Project Structure

- Entry: `MainActivity` -> `NCMDApp()`
- Navigation: `ui/navigation/NavGraph.kt`
- Screens: `ui/screens/*`
- Playback manager: `api/PlayerManager.kt`
- Session and settings: `api/SessionManager.kt`
- Local data: `data/local/*` + `data/repository/*`

## Screenshots (Placeholders)

> Put your images into: `app/src/main/assets/images/`

![Home](images/home.png)
![Queue](images/queue.png)
![Search](images/search.png)
![Settings](images/settings.png)

## Build

```bash
./gradlew build
```
