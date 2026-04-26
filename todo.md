# NCMD 代码审查问题清单

> 上次更新: 2026-04-26
> 原始 38 项问题 + 本轮新发现 **14 项** = 总计 **52 项**
> 已修复: **28 项** | 剩余: **24 项**

## 修复优先级说明

| 级别 | 含义 | 处理策略 |
|------|------|----------|
| 🔴 Critical | 运行时crash或严重功能缺陷 | 立即修复 |
| 🟠 Major | 潜在崩溃、数据丢失、安全风险 | 尽快修复 |
| 🟡 Deprecation | 编译已知废弃警告 | 随版本升级时修复 |
| 🔵 Code Quality | 代码风格、可维护性问题 | 穿插修复 |
| ⚪ Architecture | 架构设计考量 | 长期重构计划 |

---

## 🔴 Critical（6项 — 已修5，新增1）

### 已修复 ✅
- [x] **C1** `PlayerManager.kt` — `getInstance()` 未同步锁
  - 已加 `@Volatile` + `synchronized` 双重检查锁
- [x] **C2** `PlayerManager.kt` — ExoPlayer listener 回调可能不在主线程
  - 全部状态赋值已加 `mainHandler.post { }`
- [x] **C3** `SessionManager.kt` — `logout()` 清空全部 SharedPreferences
  - 只清除 auth key，保留 UI 偏好
- [x] **C4** `PlayerManager.kt` — `exoPlayer!!` 强制解包
  - 用 `val existing = exoPlayer; if (existing != null) return existing`
- [x] **C5** `LogScreen.kt` — `filterLevel!!` 强制解包
  - 改用 `?.let {}`

### 待修复
- [x] **C6** `PlayerManager.kt:423` — `prefetchedNextUrl!!` 强制解包
  - fetchLyric 中强制解包预取URL，若竞态下为 null 则 crash
  - 修复：用 `?.let {}` 或 `?: return`

---

## 🟠 Major（19项 — 已修9，保留1，新增9）

### 已修复 ✅
- [x] **M1** `NeteaseApiService.kt` — OkHttp Response 未用 `.use()` 关闭
  - 全部 24+ 处已迁移到 `executeWithUse()` 辅助函数
- [x] **M2** `NeteaseApiService.kt` — 日志泄露 Cookie
  - 已删除 cookie preview + Set-Cookie 日志
- [x] **M3** `MusicRepository.kt` — `saveCurrentPlaylist()` 非事务
  - 已添加 `@Transaction replacePlaylist()` DAO 方法
- [x] **M4** `PlayerManager.kt` — `duration.toInt()` 长歌溢出
  - 已添加 `Long.toIntSafe()` 扩展函数
- [x] **M6** `NeteaseApiService.kt` — `encodedParams` 可能为 null
  - 已添加 `buildWeapiBody()` 统一处理
- [x] **M7** `PlayerManager.kt` — 混用 `android.util.Log` 和 `Logger`
  - 全部迁移到 `Logger`
- [x] **M8** `PlayerManager.kt` — 空 catch 块
  - 已添加日志
- [x] **M9** `Logger.kt` — `SimpleDateFormat` 非线程安全
  - 已用 `ThreadLocal` 包装
- [x] **M10** `CryptoUtil.kt` — AES 密钥生成用 `kotlin.random.Random`
  - 已改用 `java.security.SecureRandom`

### 无需修复
- [ ] ~~**M5** `CryptoUtil.kt` — RSA "NoPadding"~~
  - 经审查：此为 weapi 标准实现，Netease 服务器预期就是 NoPadding，无需改动
  - 标记为 won't fix

### 待修复（新增）
- [x] **M11** `PlayerScreen.kt:131` — `GlobalScope.launch(Dispatchers.IO)` 用于 like/unlike
  - 协程完全不受管理，可无限存活，泄漏资源
  - 修复：改用 `rememberCoroutineScope()` 或 ViewModel
- [x] **M12** `PlayerManager.kt:158` — `exoPlayer?.duration?.toInt() ?: 0` 未使用 toIntSafe
  - 修复：改用 `duration?.toIntSafe() ?: 0`
- [x] **M13** `PlayerScreen.kt:829` — `line.time.toInt()` 歌词时间戳溢出
  - `LyricLine.time` 是 `Long`，直接 toInt() 可能溢出
  - 修复：用 `toIntSafe()` 或先 `coerceIn`
- [x] **M14** `SleepTimerPolicy.kt:18` — `remainingMs.toInt()` 溢出
  - 虽然实际值通常较小，但理论上有溢出风险
  - 修复：用 `coerceIn` 或 `toIntSafe()`
- [ ] **M15** `PlayerScreen.kt` — 14 处硬编码英文 `contentDescription` 未本地化
  - 行号：220, 228, 322, 330, 337, 373, 396, 402, 697, 716, 726, 746, 751
  - 修复：提取到 string resources
- [ ] **M16** `LoginScreen.kt` — 全部登录业务在 Composable 中（原 Q4 升级）
  - 4 处 `scope.launch` 直接调用网络 API，无 ViewModel
  - 修复：抽取到 LoginViewModel
- [ ] **M17** `MainScreen.kt:834,1189,1220,1256` + `PlayerScreen.kt:143,480` — 6 处 `scope.launch` 在 Composable 中直接调用网络 API
  - `rememberCoroutineScope()` 只能跟随 composable 生命周期，不跟随 Activity
  - 修复：提取到 ViewModel 或至少用生命周期感知方式
- [ ] **M18** `NeteaseApiService.kt` 多处 — 仍混用 `Log` 和 `Logger`
  - 方法内大量使用 `Log.d(TAG, ...)` 而非 `Logger.d(TAG, ...)`
  - 修复：统一到 `Logger`
- [ ] **M19** `NeteaseApiService.kt:318` — `Log.d(TAG, "Cookie length: ${cookie.length}")`
  - 虽然只打长度，但也属于信息泄露（攻击者可据此判断 cookie 格式）
  - 修复：移除或加 `BuildConfig.DEBUG` 守卫

---

## 🟡 Deprecation Warning（4项 — 全部已修）

- [x] **D1** `AppDatabase.kt:32` — `fallbackToDestructiveMigration()` 废弃
  - 已加 `dropAllTables = true` 参数
- [x] **D2** `ToastExt.kt:37` — `Toast.view` 废弃
  - 已加 `@Suppress("DEPRECATION")`
- [x] **D3** `Theme.kt:111` — `window.statusBarColor` 废弃
  - 已移除
- [x] **D4** `Theme.kt:171` — `ColorScheme` 构造器废弃
  - 已补全 `fixed*` 参数

---

## 🔵 Code Quality（18项 — 已修1，新增6）

### 已修复 ✅
- [x] **Q10** `PlayerManager.kt` — `managerScope.coroutineContext.cancel()` 应改为 `managerScope.cancel()`

### 待修复
- [ ] **Q1** 7 个文件（22 处） — 通配符 import
  - 涉及：`LoginScreen.kt`, `LogScreen.kt`, `MainScreen.kt`, `PlayerScreen.kt`, `PlaybackQueueSheet.kt`, `PlaylistListScreen.kt`, `Logger.kt`
- [x] **Q2** `Color.kt:5-11` — 6 个未使用颜色常量（Purple80/Pink40 等模板遗留）
- [x] **Q3** `Type.kt:18-33` — 注释掉的模板代码
- [ ] **Q5** `Theme.kt` — `animateColorScheme` 过于冗长（~80行样板代码）
- [ ] **Q6** `PlayerManager.kt` — 状态暴露过宽（`mutableStateOf` + `internal get`）
- [ ] **Q7** `App.kt:39-40` — `themeMode`/`languageMode` 用 `mutableIntStateOf` 不与 SessionManager 同步
- [ ] **Q8** `NavGraph.kt` — 导航缺少 `launchSingleTop`
- [ ] **Q9** `NeteaseApiService.kt` — 大量重复代码（parameter encoding — 部分已用 `buildWeapiBody` 缓解）
- [ ] **Q11** `CurrentPlaylistDao.kt:21` — `updatePosition` SQL 使用表达式赋值
- [x] **Q12** `PlayerManager.kt` — `managerScope.launch(IO)` 无错误处理

### 新增
- [x] **Q13** `strings.xml`（全部3个语言） — 15 个未使用的 string resource
  - `common_collapse`, `common_unknown_error`, `nav_default_detail_name`,
    `search_detail_empty`, `search_detail_load_failed`, `search_detail_start_play_all`,
    `settings_cookie_copied`, `settings_cookie_key`, `settings_cookie_length`,
    `settings_copy_cookie`, `settings_no_cookie`, `settings_section_account`,
    `settings_section_debug`, `settings_section_feature`, `settings_use_local_recent`
  - 处理：清理或标记待废弃
- [ ] **Q14** 7 个文件（14 处） — 使用 `mutableIntStateOf`（实验性 API）
  - 涉及：`PlayerManager.kt`(4), `MainScreen.kt`(2), `SubSettingsScreens.kt`(4), `App.kt`(2), `PlayerScreen.kt`(1)
  - 虽当前可用，但非 stable API，可能在未来版本变更
- [ ] **Q15** `SubSettingsScreens.kt:720` — 硬编码 `"GitHub"`（品牌名，可接受但建议抽离）
- [ ] **Q16** `PlayerManager.kt` — `@OptIn(UnstableApi::class)` 未解释具体原因
- [ ] **Q17** `LogScreen.kt:106` — 硬编码 `"Share Logs"` 未本地化
- [ ] **Q18** 大量 composable 硬编码英文 contentDescription（已在 M15 中覆盖）

---

## ⚪ Architecture（7项 — 新增3）

### 待处理
- [ ] **A1** `ApiProvider.kt` — `@Suppress("UNUSED_PARAMETER")` 表明 context 参数多余
- [ ] **A2** `PlayerManager` — 职责过重（播放+状态+通知+持久化+歌单管理）
- [ ] **A3** `MainViewModel.loadHomeData()` — 用 `AtomicInteger` 手动计数并发请求
- [ ] **A4** 5 个 ViewModel 用 `AndroidViewModel` 仅为了 `getApplication()`
  - 实际只有 `MainViewModel` 有 1 处合法 `getString()` 调用；其余 4 个可改为普通 `ViewModel`
- [ ] **A5** `NeteaseApiService` 在 ViewModel/Composable 中多处创建
- [ ] **A6** `PlaylistDetailScreen.kt` — 大量 lambda 回调传递
- [ ] **A7** 测试覆盖率极低（仅 2 个模板测试）

### 新增
- [ ] **A8** `SessionManager` 非单例 — 全项目约 15+ 处独立实例化
  - 虽 SharedPreferences 是单例，但 15+ 实例仍浪费
  - 建议：改为单例或依赖注入
- [ ] **A9** 4 个 Composable 超 300 行
  - `MainScreen.kt:81` ~727 行
  - `PlaybackBar()` ~458 行
  - `PlayerScreen.kt:78` ~678 行
  - `TrackCollectionScaffold.kt:74` ~407 行
  - 建议：抽取内部 private composable
- [ ] **A10** 17 处 `scope.launch` 在 Composable 中直接做网络请求（部分已在 M17 覆盖）
  - 大多数应迁移到 ViewModel

---

## 已知工作区注意事项（不变）

- **Room 版本冲突**: `app/build.gradle.kts` 用 `room_version = "2.8.4"`，`gradle/libs.versions.toml` 声明 `room = "2.6.1"`
  - app module 实际使用 2.8.4（直接写版本号），不影响编译
  - 但建议统一样式
- `.idea/` 目录已删除并加入 `.gitignore`
- 编译使用 **JetBrains MCP**，禁止直接使用 Gradle CLI
