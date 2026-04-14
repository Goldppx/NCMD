# NCMD Agent Guidelines

Guidelines for agentic coding agents working on the NCMD Android music player (Jetbrains-based).

## 1. Build & Test Commands

### Building
- **Build project**: `jetbrains_build_project` (preferred)
- **Gradle**: `./gradlew build` or `./gradlew clean build`
- **Rebuild specific file**: `jetbrains_build_project` with `filesToRebuild` parameter

### Testing  
- **All unit tests**: `jetbrains_execute_run_configuration` or `./gradlew testDebugUnitTest`
- **Single test class**: `./gradlew testDebugUnitTest --tests com.gem.neteasecloudmd.ClassName`
- **Single test method**: `./gradlew testDebugUnitTest --tests com.gem.neteasecloudmd.ClassName.methodName`
- **Instrumented tests**: `jetbrains_execute_run_configuration` with Android test config
- **With coverage**: `./gradlew testDebugUnitTestCoverage`

### Running & Debugging
- **Install & run app**: `jetbrains_execute_run_configuration` with "app" config
- **View logs**: `jetbrains_execute_terminal_command` + `adb logcat` (NOT bash)
- **Filter logs**: `adb logcat com.gem.neteasecloudmd:I`

**NOTE**: Use JetBrains tools for all build/run/test operations. Avoid bash for these.

---

## 2. Code Style Guidelines

### 2.1 Kotlin Conventions (Official Style)
- **Naming**: camelCase for variables/functions, PascalCase for classes
- **Singletons**: Use `object CryptoUtil` for utility classes
- **Imports**: Organized in groups (kotlin.*, android.*, androidx.*, third-party, project), alphabetically sorted, no wildcards
- **Strings**: Hardcoded for now; use triple quotes or `.trimIndent()` for multi-line strings
- **Lambdas**: Single-line on same line; multi-line on separate lines

### 2.2 Compose UI Conventions
- **Modifiers**: Always last parameter, use dot notation
- **Spacing**: Material Design 3 (4dp, 8dp, 12dp, 16dp, 24dp, 32dp)
- **Colors**: Use `MaterialTheme.colorScheme.*` instead of hardcoding
- **State**: Prefer `remember { mutableStateOf(...) }` for local state
- **Annotations**: Use `@Composable`, `@OptIn(ExperimentalMaterial3Api::class)` as needed

### 2.3 Type System
- Explicit types for public API functions; infer for obvious local variables
- Nullable types: Use `.let { }` or `?.` operator over explicit null checks
- Data classes: Add `@Serializable` for API responses
- Sealed classes for exhaustive when expressions

### 2.4 Error Handling
- Use `Result<T>` pattern: `Result.success(value)` / `Result.failure(exception)`
- Log errors with `Log.e(tag, message, exception)` before propagating
- Meaningful error messages always
- Network timeouts handled gracefully with user-facing messages

### 2.5 API & Networking
- All API responses: `@Serializable` data classes
- Use `withContext(Dispatchers.IO)` for network calls
- Wrap with `try/catch` and convert to Result types
- Cookies via headers: `Cookie: <cookie-string>`
- Tag OkHttpClient requests for debugging

### 2.6 Coroutines
- `CoroutineScope(Dispatchers.Main)` for UI operations
- `withContext(Dispatchers.IO)` for I/O operations
- `withTimeoutOrNull(timeoutMs)` for operations with timeouts
- Cancel scopes to prevent memory leaks (auto-handled in Compose)

### 2.7 File Organization
```
app/src/main/java/com/gem/neteasecloudmd/
├── LoginActivity.kt              # Login UI
├── MainActivity.kt               # Home/player UI
├── PlaylistDetailActivity.kt     # Playlist view
├── PlaylistListActivity.kt       # Playlist browse
├── RecentPlaysActivity.kt        # Recently played
├── api/
│   ├── CryptoUtil.kt            # AES + RSA encryption
│   ├── NeteaseApiService.kt     # HTTP API client
│   ├── SessionManager.kt         # Cookies & persistence
│   └── PlayerManager.kt          # ExoPlayer integration
├── data/
│   ├── local/entity/             # Room entities
│   ├── local/dao/                # Room DAOs
│   └── repository/               # Data repositories
└── ui/theme/
    ├── Theme.kt
    ├── Color.kt
    └── Type.kt
```

### 2.8 Naming Conventions
- **API methods**: camelCase (e.g., `getUserPlaylists()`)
- **State variables**: Present tense (e.g., `isLoading`, `currentTrack`)
- **Event callbacks**: "on" prefix (e.g., `onLoginClick`, `onRefresh`)
- **Colors**: Semantic names (e.g., `primary`, `onSurfaceVariant`)
- **Log tags**: Use class name (e.g., `Log.e("PlayerManager", ...)`)

---

## 3. Project-Specific Guidelines

### 3.1 Netease API
- **Base URL**: `https://music.163.com/weapi/*`
- **Encryption**: All request bodies must use `CryptoUtil.weapi(jsonData)`
- **Response format**: Wrap in `LoginResult`, `PlaylistResponse`, etc.
- **Error codes**: 200 = success, 400 = security verification, others = display error

### 3.2 Authentication
- **Login methods**: 1) Phone + password 2) SMS captcha (phone only)
- **Session storage**: Use `SessionManager` for cookie & profile persistence
- **Logout**: Clear SharedPreferences and reset UI state

### 3.3 ExoPlayer Integration
- **Thread safety**: Access ExoPlayer only on main thread
- **State tracking**: Update `currentPosition` and `duration` every 1 second
- **Lifecycle**: Release player in `onDestroy()` to prevent memory leaks

### 3.4 UI/UX Principles
- Follow Material Design 3 specifications
- Use MD3 components (Scaffold, TopAppBar, Surface, etc.)
- Implement proper loading states (progress indicators)
- Show error messages via Toast or inline text

---

## 4. Common Patterns

### 4.1 API Call Pattern
```kotlin
suspend fun fetchData(): Result<DataType> = withContext(Dispatchers.IO) {
    return@withContext try {
        val response = apiClient.makeRequest()
        Result.success(response.data)
    } catch (e: Exception) {
        Log.e("Tag", "Error fetching data", e)
        Result.failure(e)
    }
}
```

### 4.2 State Management Pattern
```kotlin
var data by remember { mutableStateOf<List<Item>>(emptyList()) }
var isLoading by remember { mutableStateOf(false) }

LaunchedEffect(refreshKey) {
    isLoading = true
    val result = fetchData()
    result.onSuccess { data = it }
    isLoading = false
}
```

### 4.3 Error Handling in UI
```kotlin
result.fold(
    onSuccess = { value -> /* update UI */ },
    onFailure = { e -> errorMessage = e.message }
)
```

---

## 5. Code Review Checklist

Before committing changes:
- [ ] Compiled successfully with `jetbrains_build_project`
- [ ] Imports organized and unused imports removed
- [ ] Used Material 3 components and themes
- [ ] Error messages are user-friendly
- [ ] Coroutines use correct Dispatchers
- [ ] No hardcoded strings longer than reasonable
- [ ] Null safety handled explicitly
- [ ] Comments added for complex logic
- [ ] Follows naming conventions
