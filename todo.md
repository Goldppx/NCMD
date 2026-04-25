# NCMD 代码审查问题清单

> 创建日期: 2026-04-25
> 共发现 **38项** 问题（Critical: 5, Major: 10, Deprecation: 4, Code Quality: 12, Architecture: 7）

## 修复优先级说明

| 级别 | 含义 | 处理策略 |
|------|------|----------|
| 🔴 Critical | 运行时crash或严重功能缺陷 | 立即修复 |
| 🟠 Major | 潜在崩溃、数据丢失、安全风险 | 尽快修复 |
| 🟡 Deprecation | 编译已知废弃警告 | 随版本升级时修复 |
| 🔵 Code Quality | 代码风格、可维护性问题 | 穿插修复 |
| ⚪ Architecture | 架构设计考量 | 长期重构计划 |

---

## 🔴 Critical（5项 - 立即修复）

- [ ] **C1** `PlayerManager.kt:842-846` — `getInstance()` 未同步锁
  - 多线程并发可能创建多个 ExoPlayer 实例
  - 修复：添加 `@Volatile` + `synchronized` 双重检查锁
  
- [ ] **C2** `PlayerManager.kt:131-174` — ExoPlayer listener 回调可能不在主线程
  - Compose 状态从后台线程更新会导致 `IllegalStateException`
  - 修复：所有状态赋值前加 `mainHandler.post { }`
  
- [ ] **C3** `SessionManager.kt:138-139` — `logout()` 清空全部 SharedPreferences
  - 退出登录丢失主题/语言/睡眠定时器等所有偏好设置
  - 修复：只清除 auth 相关 key，保留 UI 偏好

- [ ] **C4** `PlayerManager.kt:178,181,846` — `exoPlayer!!` 强制解包
  - 竞态下 player 未初始化直接 crash
  - 修复：用 `?.let {}` 替代 `!!`

- [ ] **C5** `LogScreen.kt:68` — `filterLevel!!` 强制解包
  - Badge 渲染时 filterLevel 为 null 则 crash
  - 修复：用 `?.let {}` 处理

---

## 🟠 Major（10项 - 尽快修复）

- [ ] **M1** `NeteaseApiService.kt` 多处 — OkHttp Response 未用 `.use()` 关闭
  - 可能导致连接池耗尽
  - 修复：改为 `response.use { resp -> ... }` 模式

- [ ] **M2** `NeteaseApiService.kt:200,344` — 日志泄露 Cookie
  - 打印 cookie 长度和前100字符、Set-Cookie 头
  - 修复：移出生产代码或加 `BuildConfig.DEBUG` 守卫

- [ ] **M3** `MusicRepository.kt:56-71` — `saveCurrentPlaylist()` 非事务
  - clear/insert/update 中间 crash 导致数据不一致
  - 修复：合并为 `@Transaction` DAO 方法

- [ ] **M4** `PlayerManager.kt:111-112,139` — `duration.toInt()` 长歌溢出
  - 超过 ~35 分钟的歌 duration 变成负值
  - 修复：用 `toIntSafe()` 或保持 Long

- [ ] **M5** `CryptoUtil.kt:58-63` — RSA 使用 "NoPadding"
  - 服务器预期可能是 PKCS1，加解密可能失败
  - 修复：验证服务器规范的 padding 模式

- [ ] **M6** `NeteaseApiService.kt` 多处 — `encodedParams` 可能为 null
  - 请求体变成 `"params=null&..."` 导致请求失败
  - 修复：加 null 检查，提前 fail

- [ ] **M7** `PlayerManager.kt` — 混用 `android.util.Log` 和自定义 `Logger`
  - 修复：统一使用 `Logger`

- [ ] **M8** `PlayerManager.kt:767-768` — 空 catch 块 `// Ignore`
  - 修复：至少记录日志

- [ ] **M9** `Logger.kt:23,71` — `SimpleDateFormat` 非线程安全
  - 修复：每次使用新建实例或使用 `ThreadLocal`

- [ ] **M10** `CryptoUtil.kt:46` — AES 密钥生成用 `kotlin.random.Random`
  - 非密码学安全随机数
  - 修复：改用 `java.security.SecureRandom`

---

## 🟡 Deprecation Warning（4项 - 编译已知）

- [ ] **D1** `AppDatabase.kt:32` — `fallbackToDestructiveMigration()` 废弃
  - 修复：用新签名 `fallbackToDestructiveMigration(dropAllTables = ...)`

- [ ] **D2** `ToastExt.kt:37` — `Toast.view` 废弃
  - 修复：迁移到 Snackbar 或 Material3 样式

- [ ] **D3** `Theme.kt:111` — `window.statusBarColor` 废弃
  - 修复：用 `WindowInsetsControllerCompat`

- [ ] **D4** `Theme.kt:171` — `ColorScheme` 构造器废弃
  - 修复：新增 `fixed*` 容器颜色参数

---

## 🔵 Code Quality（12项 - 代码整洁）

- [ ] **Q1** 7个文件（22处） — 通配符 import（`.layout.*`、`.material3.*`、`.runtime.*` 等）
- [ ] **Q2** `Color.kt:5-11` — 6个未使用颜色常量（Purple80/Pink40 等模板遗留）
- [ ] **Q3** `Type.kt:18-33` — 注释掉的模板代码
- [ ] **Q4** `LoginScreen.kt` — 登录业务全在 Composable 中，无 ViewModel 层
- [ ] **Q5** `Theme.kt:124-210` — `animateColorScheme` 过于冗长
- [ ] **Q6** `PlayerManager.kt` — 状态暴露过宽，大量 Composable 直接读取 `mutableStateOf` 字段
- [ ] **Q7** `App.kt:39-40` — `themeMode`/`languageMode` 用 `mutableIntStateOf` 不与 SessionManager 同步
- [ ] **Q8** `NavGraph.kt` — 导航缺少 `launchSingleTop`
- [ ] **Q9** `NeteaseApiService.kt` — 大量重复代码（parameter encoding）
- [ ] **Q10** `PlayerManager.kt:775` — `managerScope.coroutineContext.cancel()` 应改为 `managerScope.cancel()`
- [ ] **Q11** `CurrentPlaylistDao.kt:21` — `updatePosition` SQL 使用表达式赋值
- [ ] **Q12** `PlayerManager.kt` — `managerScope.launch(IO)` 无错误处理

---

## ⚪ Architecture（7项 - 设计考量）

- [ ] **A1** `ApiProvider.kt` — `@Suppress("UNUSED_PARAMETER")` 表明 context 参数多余
- [ ] **A2** `PlayerManager` — 职责过重（播放+状态+通知+持久化）
- [ ] **A3** `MainViewModel.loadHomeData()` — 用 `AtomicInteger` 手动计数并发
- [ ] **A4** 所有 ViewModel 用 `AndroidViewModel` 仅为了 `getApplication()`
- [ ] **A5** `NeteaseApiService` 在 ViewModel/Composable 中多处创建
- [ ] **A6** `PlaylistDetailScreen.kt` — 大量 lambda 回调传递
- [ ] **A7** 测试覆盖率极低（仅2个模板测试）

---

## 已知工作区注意事项

- **Room 版本冲突**: `app/build.gradle.kts` 用 `room_version = "2.8.4"`，`gradle/libs.versions.toml` 声明 `room = "2.6.1"`
  - app module 实际使用 2.8.4（直接写版本号），不影响编译
  - 但建议统一样式
- `.idea/` 目录已删除并加入 `.gitignore`
- 编译使用 **JetBrains MCP**，禁止直接使用 Gradle CLI
