# NCMD

> 中文在前（默认）。English version is below.

---

## 中文版

NCMD 是一个基于 Kotlin + Jetpack Compose + Material 3 的 Android 音乐客户端。

### 核心特性

- 多种登录方式：账号密码 / 短信验证码 / Cookie
- 首页聚合：私人 FM、最近播放、我的歌单
- 搜索：单曲 / 歌单 / 专辑 + 搜索建议
- 全局播放控制栏（登录页隐藏）
- 播放队列管理：顺序/随机/单曲循环、单曲移除、清空队列
- 系统通知媒体控制（上一首 / 播放暂停 / 下一首）
- 主题能力：浅色 / 深色 / 跟随系统 + 封面动态取色
- 语言切换：跟随系统 / 简体中文 / 繁体中文 / English

### 技术栈

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose（单 Activity 架构）
- OkHttp + Kotlin Serialization
- Media3 (ExoPlayer / Session / Notification)
- Room

### 项目结构

- 入口：`MainActivity` -> `NCMDApp()`
- 导航：`ui/navigation/NavGraph.kt`
- 页面：`ui/screens/*`
- 播放管理：`api/PlayerManager.kt`
- 会话与设置：`api/SessionManager.kt`
- 本地数据：`data/local/*` + `data/repository/*`

### 截图展示（预留）

> 请将图片放入：`app/src/main/assets/images/`

![首页](images/home.png)
![播放队列](images/queue.png)
![搜索](images/search.png)
![设置](images/settings.png)

### 构建

```bash
./gradlew build
```

---

## English

NCMD is an Android music client built with Kotlin + Jetpack Compose + Material 3.

### Key Features

- Multiple login modes: password / SMS captcha / cookie
- Home aggregation: Personal FM, recent plays, and playlists
- Search: songs / playlists / albums + suggestions
- Global playback bar (hidden on login screen)
- Queue controls: sequential/shuffle/repeat-one, remove track, clear queue
- Native media notification controls (prev / play-pause / next)
- Theme support: light / dark / system + cover-based dynamic color
- Language switcher: system / Simplified Chinese / Traditional Chinese / English

### Tech Stack

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose (single-Activity architecture)
- OkHttp + Kotlin Serialization
- Media3 (ExoPlayer / Session / Notification)
- Room

### Project Structure

- Entry: `MainActivity` -> `NCMDApp()`
- Navigation: `ui/navigation/NavGraph.kt`
- Screens: `ui/screens/*`
- Playback manager: `api/PlayerManager.kt`
- Session and settings: `api/SessionManager.kt`
- Local data: `data/local/*` + `data/repository/*`

### Screenshots (Placeholders)

> Put your images into: `app/src/main/assets/images/`

![Home](images/home.png)
![Queue](images/queue.png)
![Search](images/search.png)
![Settings](images/settings.png)

### Build

```bash
./gradlew build
```
