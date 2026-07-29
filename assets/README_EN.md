# NCMD — A Pure & Elegant Netease Music Client

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Desktop%20Foundation-green.svg)](https://www.jetbrains.com/compose-multiplatform/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.10-orange.svg)](https://developer.android.com/jetpack/compose)

[Back to Chinese Version](../README.md)

NCMD is a modern, third-party Netease Cloud Music client. We don't chase feature bloat; we focus on bringing you the most pure and immersive listening experience. The Android client uses **Material 3**, while the project is moving to Kotlin Multiplatform to support Windows and Linux desktop clients.

---

## ✨ Key Highlights

### 🎨 Fluid Visual Language
- **Full Material 3**: Adheres to the latest design standards with smooth transitions and a transparent layout.
- **Dynamic Theming**: The interface background and colors flow and change according to the album art.
- **Dark Mode Support**: Perfectly adapts to system themes, protecting your eyes during late-night sessions.

### 🎭 Immersive Landscape Player
The soul of NCMD. When you rotate your phone, a whole new vision unfolds:
- **Gesture-Driven**: Swipe up to show controls, swipe down to hide, and tap to toggle song info display modes.
- **Borderless Vision**: Removed redundant borders and buttons, letting lyrics and cover art shine.
- **Edge-to-Edge**: The visual experience extends to the very edges of your screen, making use of every pixel.

### 🚀 Powerful Core
- **Smart Playback**: Built on Media3 with support for global notification controls, lock screen playback, and media keys.
- **Lightweight & Fast**: Single-Activity architecture with Navigation Compose for lightning-fast responsiveness.
- **Persistent State**: Remembers your playlists, UI preferences, and login session.

### 🌐 Kotlin Multiplatform migration status
- **Shared now**: lyric parsing, playback queue and request policies, sleep timer policy, domain models, and the playback-controller contract.
- **Verified now**: the shared module is tested on Android and Desktop JVM in continuous integration.
- **Next**: build a Compose Desktop client for Windows and Linux, then progressively move screen state and reusable UI into the shared module.

> The Android client is the current end-user product. The Desktop client is under development and has not been released yet.

---

## 🛠️ Technical Stack

NCMD leverages the cutting edge of Android development:
- **UI**: Jetpack Compose, Material 3, Navigation Compose
- **Media**: Media3 (ExoPlayer, Session)
- **Network**: OkHttp, Kotlin Serialization
- **Storage**: Room Database, SharedPreferences
- **Architecture**: MVVM (Android) + Kotlin Multiplatform shared core

---

## 🚀 Quick Start

### Requirements
- Android 12 (API 31) or higher
- JDK 17+

### Build & Install
```bash
git clone https://github.com/your-username/NCMD.git
cd NCMD
./gradlew assembleDebug
```

### Desktop preview (Windows / Linux)
```bash
./gradlew :desktop:run
```

Use `:desktop:packageMsi` on Windows, or `:desktop:packageDeb` / `:desktop:packageRpm`
on Linux. The Desktop client currently previews the shared state and UI; account login,
networking, and the audio engine will be connected in subsequent migration work.

---

## 📸 Preview (Coming Soon)

> Check out more screenshots in [assets/images/](../app/src/main/assets/images/)

---

## 🤝 Contribution

Feel free to open an Issue or submit a Pull Request. If you love this project, please give it a ⭐️!

---

## 📄 License

This project is licensed under the MIT License.

---

*"Bringing music back to its essence."*
