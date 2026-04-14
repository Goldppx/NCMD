# PlaybackBar 手势快速参考

## 用户交互指南

### 基本操作

| 操作 | 说明 | 结果 |
|------|------|------|
| **左滑** | 快速向左滑动 PlaybackBar（> 100px） | 播放下一首 🎵 |
| **右滑** | 快速向右滑动 PlaybackBar（> 100px） | 播放上一首 🎵 |
| **上滑** | 快速向上滑动（> 100px） | 播放下一首 🎵 |
| **长按进度条** | 按住进度条位置并拖动 | 实时调整播放进度 ⏱️ |

### 手势规则

✓ **会触发的操作**
- 左滑距离 > 100px → 下一首
- 右滑距离 > 100px → 上一首
- 上滑距离 > 100px → 下一首
- 在进度条区域拖动 → 调整进度

✗ **不会触发的操作**
- 对角线滑动（不够纯水平/纯竖直）
- 微弱移动（< 50px）
- 下滑（保留功能）

---

## 代码快速查阅

### 文件位置
```
/app/src/main/java/com/gem/neteasecloudmd/MainActivity.kt
第 744-950 行：PlaybackBar Composable
第 952 行：GestureDirection 枚举
```

### 关键变量

```kotlin
// 进度调整状态
var isAdjustingProgress by remember { mutableStateOf(false) }
var adjustedProgress by remember { mutableStateOf(progress) }

// 手势状态
var gestureDirection by remember { 
    mutableStateOf<GestureDirection>(GestureDirection.NONE) 
}
```

### 修改进度阈值

```kotlin
// 调整滑动距离阈值（当前 100px）
if (kotlin.math.abs(accumulatedDeltaX) > 100) {
    player.next()  // ← 改这里的 100
}

// 调整方向识别阈值（当前 50px）
if (absX > 50 || absY > 50) {  // ← 改这里的 50
    hasReachedThreshold = true
}

// 调整比值严格度（当前 2.0）
if (absX > absY * 2.0f) {  // ← 改这里的 2.0
    gestureDirection = GestureDirection.LEFT
}
```

### 修改进度条高度

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)  // ← 改这里（便于点击的区域）
        .pointerInput(Unit) { ... }
)
```

---

## 调试技巧

### 查看手势识别过程

在 `onDrag` 回调中添加日志：
```kotlin
onDrag = { change, dragAmount ->
    accumulatedDeltaX += dragAmount.x
    accumulatedDeltaY += dragAmount.y
    
    // 调试输出
    Log.d("PlaybackBar", "X=$accumulatedDeltaX Y=$accumulatedDeltaY Direction=$gestureDirection")
}
```

### 临时禁用手势

```kotlin
if (false) {  // ← 改为 false 禁用
    .pointerInput(Unit) {
        detectDragGestures { ... }
    }
}
```

---

## 性能优化

当前实现已优化：
- ✓ 手势识别在 `onDrag` 中及时计算，不延迟
- ✓ `LaunchedEffect` 仅在进度变化时更新（防频繁更新）
- ✓ 进度条仅在调整时显示（减少 UI 绘制）
- ✓ 使用 `consume()` 防止事件冒泡

---

## 常见问题

**Q: 为什么对角线滑动不工作？**
A: 这是设计的特性，防止误操作。需要 `absX > absY * 2.0f` 的比值。

**Q: 如何快速跳转到特定位置？**
A: 按住进度条并拖动到目标位置，松开立即跳转。

**Q: 进度条什么时候显示？**
A: 仅在按住进度条拖动时显示。其他时间显示封面和文字。

**Q: 可以修改阈值吗？**
A: 可以，修改上面"关键参数"部分的数值即可。

---

## 相关文件

- `MainActivity.kt`: PlaybackBar Composable 实现
- `PlayerManager.kt`: 播放器控制逻辑
- `PLAYBACK_BAR_GESTURE_IMPROVEMENTS.md`: 详细技术说明

