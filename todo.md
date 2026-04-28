# NCMD 代码审查问题清单

> 上次更新: 2026-04-26
> 原始 38 项 + 新发现 14 项 = 总计 52 项
> 已修复: **44 项** | 无需修复: **2 项** | 剩余待评估: **6 项**

## 已修复（44项）

**🔴 Critical (6/6)**: C1-C5 全部已修，C6 prefetchedNextUrl!! 已修  
**🟠 Major (17/19)**: M1-M4/M6-M15/M17-M19 已修；M5(RSA)无需修  
**🟡 Deprecation (4/4)**: D1-D4 全部已修  
**🔵 Code Quality (14/18)**: Q1(partial)/Q2/Q3/Q7/Q8/Q10/Q12-Q18 已修  
**⚪ Architecture (3/10)**: A1/A3 已修

## 无需修复

- **M5** `CryptoUtil.kt` — RSA "NoPadding"：weapi 标准实现，服务器预期如此
- **M16** `LoginScreen.kt` — 登录业务全在 Composable：项目约定（AGENTS.md §4）

## 待评估

- **Q1(remaining)** 2 文件（MainScreen, PlayerScreen）Compose 通配符 import
- **A4** 5 个 ViewModel 用 AndroidViewModel（均合法需要 Context）
- **A8** SessionManager 15+ 实例（SharedPreferences 底层单例，无实际影响）
- **A9** 4 个 composable 超过 300 行（设计模式取舍）
- **A10** composable 中 scope.launch 网络请求（部分已在 M17 修）
- **A2/A5/A6/A7** 架构设计考量（PlayerManager 职责、ApiService 创建、lambda 回调、测试覆盖率）

## 已知工作区注意事项

- **Room 版本冲突**: `app/build.gradle.kts` 用 `room_version = "2.8.4"`，`gradle/libs.versions.toml` 声明 `room = "2.6.1"`
- 编译使用 **JetBrains MCP**，禁止直接使用 Gradle CLI
