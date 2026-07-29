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
- **Desktop available**: Windows and Linux support a local music library, metadata/artwork extraction, playback, artwork-derived color, and HiDPI.
- **Native Wayland**: Linux uses `WLToolkit` with JetBrains Runtime instead of falling back to XWayland.
- **Continuously verified**: CI builds Android, Linux, Windows, and Arch Linux thin packages and performs a Linux startup smoke test.

> The desktop client is still gaining online-library and login support; local music playback is ready to use.

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

### Desktop (Windows / Linux)
```bash
./gradlew :desktop:run
```

Released desktop bundles **do not include Java**. Install Java 21 or newer through the
system package manager. The Windows launcher opens the JetBrains Runtime download page
when a runtime is missing. Native Wayland on Linux requires JetBrains Runtime 21; ordinary
OpenJDK uses XWayland. Arch users should see [packaging/arch/README.md](../packaging/arch/README.md).

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
