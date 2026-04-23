# NCMD — 纯粹的某音乐客户端

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.10-orange.svg)](https://developer.android.com/jetpack/compose)

[English Version Available Here](assets/README_EN.md)

NCMD 是一款专为 Android 打造的现代音乐客户端。不追求功能的堆砌，只为带给你最纯粹、最沉浸的听歌体验。基于最新的 **Material 3** 设计语言。

---

## ✨ 核心亮点

### 🎨 灵动的视觉语言
- **全 Material 3 实现**：遵循最新的设计规范，带来丝滑的过渡动画与通透的界面布局。
- **封面动态取色**：界面背景与色彩随专辑封面的变化而流动，让音乐不仅好听，更好看。
- **深色模式支持**：完美适配系统深色/浅色切换，夜晚听歌更护眼。

### 🎭 极速沉浸的横屏播放器
这是 NCMD 的灵魂所在。当你旋转手机，一个全新的视界将为你展开：
- **手势驱动**：上滑显示控制栏，下滑收起，点击切换歌曲信息显示模式。
- **极致视野**：去掉了所有冗余的边框与按钮，歌词与封面交相辉映。
- **边缘延伸**：视觉效果直接延伸至屏幕边缘，充分利用每一寸显示空间。

### 🚀 强劲的内核
- **智能播放管理**：基于 Media3 构建，支持全局通知控制、锁屏播放与耳机控制。
- **轻量且快速**：单 Activity 架构配合 Navigation Compose，响应极速。
- **离线持久化**：记住你的播放列表、UI 偏好与登录状态。

---

## 🛠️ 技术细节

NCMD 采用了 Android 开发领域的前沿技术栈：
- **UI**: Jetpack Compose, Material 3, Navigation Compose
- **媒体**: Media3 (ExoPlayer, Session)
- **网络**: OkHttp, Kotlin Serialization
- **存储**: Room Database, SharedPreferences
- **架构**: MVVM (Model-View-ViewModel)

---

## 🚀 快速开始

### 环境要求
- Android 12 (API 31) 或更高版本
- JDK 17+

### 构建与安装
```bash
git clone https://github.com/goldppx/NCMD.git
cd NCMD
./gradlew assembleDebug
```

---

## 📸 预览（预留）

> 更多精美截图请查看 [Screenshots](assets/images/)

---

## 🤝 贡献与支持

欢迎通过 Issue 或 Pull Request 来完善这个项目。如果你喜欢这个作品，请给它一个 ⭐️！

---

## 📄 开源协议

本项目采用 MIT 协议开源。

---

*“让音乐回归本质。”*
