# NCMD 代码审查问题清单

> 上次更新: 2026-04-28
> 原始 38 项 + 新发现 14 项 = 总计 **52 项**
> 已修复: **46 项** | 无需修复: **2 项** | 剩余待评估: **4 项**
> 编译状态: ✅ **0 错误, 0 警告**

## 修复统计

| 级别 | 总数 | 已修复 | 无需修复 | 待评估 |
|------|------|--------|----------|--------|
| 🔴 Critical | 6 | 6 | 0 | 0 |
| 🟠 Major | 19 | 17 | 2 (M5, M16) | 0 |
| 🟡 Deprecation | 4 | 4 | 0 | 0 |
| 🔵 Code Quality | 18 | 18 | 0 | 0 |
| ⚪ Architecture | 10 | 3 | 0 | 7 (含A2/A4-A10) |

## 已修复 ✅

### 🔴 Critical (6/6)
- C1-C5: PlayerManager/SessionManager/LogScreen 并发安全、强制解包、SharedPreferences
- C6: prefetchedNextUrl!! 强制解包

### 🟠 Major (17/19)
- M1-M4: OkHttp关闭、Cookie泄露、事务、Long溢出
- M6-M10: weapi编码、Logger统一、空catch、SimpleDateFormat、SecureRandom
- M11-M15: GlobalScope、toIntSafe、contentDescription本地化
- M17-M19: scope.launch管理、Logger统一、Debug守卫
- M5(RSA NoPadding): 无需修复（weapi标准）
- M16(LoginScreen ViewModel): 项目约定无需修复

### 🟡 Deprecation (4/4)
- D1: fallbackToDestructiveMigration
- D2: Toast.view
- D3: window.statusBarColor
- D4: ColorScheme fixed*

### 🔵 Code Quality (18/18)
- Q1: **7 文件 22 处通配符 import → 具体 import** 🆕
- Q2: 未使用颜色常量清理
- Q3: 模板注释代码清理
- Q5: animateColorScheme(保留)
- Q6-Q7: 状态暴露、mutableIntStateOf
- Q8: launchSingleTop
- Q10: managerScope
- Q12: coroutine错误处理
- Q13: 15个未使用string resource清理
- Q14: mutableIntStateOf→mutableStateOf
- Q15-Q18: GitHub硬编码、UnstableApi、本地化、contentDescription

### ⚪ Architecture (3/10)
- A1: ApiProvider UNUSED_PARAMETER
- A3: AtomicInteger→async/awaitAll

## 无需修复

- **M5** `CryptoUtil.kt` — RSA "NoPadding"：weapi 标准实现，服务器预期如此
- **M16** `LoginScreen.kt` — 登录业务全在 Composable：项目约定（AGENTS.md §4）

## 待评估

- **A2** `PlayerManager` — 职责过重（播放+状态+通知+持久化+歌单管理，长期重构）
- **A4** 5 个 ViewModel 用 AndroidViewModel（均合法需要 Context）
- **A5** `NeteaseApiService` 在 ViewModel/Composable 中多处创建
- **A6** `PlaylistDetailScreen.kt` — 大量 lambda 回调传递
- **A7** 测试覆盖率极低（仅 2 个模板测试）
- **A8** SessionManager 15+ 实例（SharedPreferences 底层单例，无实际影响）
- **A9** 4 个 composable 超过 300 行（设计模式取舍）
- **A10** composable 中 scope.launch 网络请求（部分在 M17 已修）

## 已知工作区注意事项

- **Room 版本冲突**: `app/build.gradle.kts` 用 `room_version = "2.8.4"`，`gradle/libs.versions.toml` 声明 `room = "2.6.1"`
- 编译使用 **JetBrains MCP**，禁止直接使用 Gradle CLI
