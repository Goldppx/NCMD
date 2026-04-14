# PlaybackBar 手势改进说明

## 概述
完全重写了 `PlaybackBar` Composable 的手势检测系统，实现了5个核心功能需求。

---

## 改进内容详解

### 1. 直接左右滑动切换歌曲 ✓
**实现位置**: 外层 Box 的 `pointerInput(Unit) { detectDragGestures(...) }`

```kotlin
GestureDirection.LEFT -> {
    if (kotlin.math.abs(accumulatedDeltaX) > 100) {
        player.next()  // 左滑播放下一首
        isTransitioning = true
    }
}
GestureDirection.RIGHT -> {
    if (kotlin.math.abs(accumulatedDeltaX) > 100) {
        player.previous()  // 右滑播放上一首
        isTransitioning = true
    }
}
```

- 无需长按，直接左右滑动即可
- 滑动距离需 > 100px 才触发
- 自动识别左右方向

### 2. 长按进度条后拖动调整进度 ✓
**实现位置**: 进度条区域的独立 `pointerInput(Unit) { detectDragGestures(...) }`

```kotlin
Box(
    modifier = Modifier
        .align(Alignment.CenterStart)
        .fillMaxWidth()
        .height(8.dp)  // 可长按的区域
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    isAdjustingProgress = true
                    adjustedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                },
                onDrag = { change, dragAmount ->
                    if (!isAdjustingProgress) return@detectDragGestures
                    change.consume()
                    // 实时拖动调整
                    adjustedProgress = (adjustedProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                },
                onDragEnd = {
                    // 松开时跳转
                    val newPosition = (adjustedProgress * duration).toInt()
                    player.seekTo(newPosition)
                    isAdjustingProgress = false
                }
            )
        }
)
```

- 长按进度条区域（高度8dp，便于点击）
- 实时拖动显示进度条
- 松开立即跳转

### 3. 进度条根据圆角左右裁剪 ✓
**实现位置**: 进度条 Box 的 `.clip(RoundedCornerShape(2.dp))`

```kotlin
if (isAdjustingProgress) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .align(Alignment.Center)
            .clip(RoundedCornerShape(2.dp))  // ← 关键：圆角裁剪
    ) {
        // 背景条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(...)
        )
        // 前景条
        Box(
            modifier = Modifier
                .fillMaxWidth(adjustedProgress)
                .fillMaxHeight()
                .background(...)
        )
    }
}
```

效果：
- 进度条两端圆角自动裁剪
- 与外层圆角完全协调
- 内层条形也应用圆角（确保对齐）

### 4. 松开长按恢复原样式 ✓
**实现方式**: 使用 `isAdjustingProgress` 状态控制

```kotlin
val contentAlpha by animateFloatAsState(
    targetValue = if (isAdjustingProgress) 0f else 1f,
    animationSpec = tween(durationMillis = 200),
    label = "contentAlpha"
)
```

- 调整中：`contentAlpha = 0` → 封面和文字淡化
- 松开后：`contentAlpha = 1` → UI 平滑恢复（200ms 动画）
- 进度层：调整中隐藏，松开后显示原进度

### 5. 明确区分手势方向防止误操作 ✓
**实现位置**: 严格的方向识别算法

```kotlin
if (!hasReachedThreshold) {
    val absX = kotlin.math.abs(accumulatedDeltaX)
    val absY = kotlin.math.abs(accumulatedDeltaY)
    
    if (absX > 50 || absY > 50) {  // 阈值：50px
        hasReachedThreshold = true
        when {
            absX > absY * 2.0f -> {  // 比值 > 2.0
                gestureDirection = if (accumulatedDeltaX > 0) 
                    GestureDirection.RIGHT 
                else 
                    GestureDirection.LEFT
            }
            absY > absX * 2.0f -> {
                gestureDirection = if (accumulatedDeltaY > 0) 
                    GestureDirection.DOWN 
                else 
                    GestureDirection.UP
            }
            else -> {
                gestureDirection = GestureDirection.NONE  // 对角线，忽略
            }
        }
    }
}
```

防误操作机制：
| 滑动类型 | absX | absY | 比值判定 | 结果 |
|---------|------|------|--------|------|
| 纯左滑 | 150 | 30 | 150/30=5.0 > 2.0 | ✓ LEFT |
| 纯上滑 | 30 | 150 | 150/30=5.0 > 2.0 | ✓ UP |
| 对角线 | 80 | 60 | 80/60=1.33 < 2.0 | ✗ NONE |
| 微弱横移 | 40 | 50 | 40/50=0.8 < 2.0 | ✗ NONE |

---

## 架构改变

### 原架构
```
Box (外层)
  ├─ pointerInput: detectDragGestures (滑动)
  ├─ pointerInput: detectTapGestures (长按)
  └─ 进度条显示 (固定)
```
问题：难以区分是切换歌曲还是调整进度

### 新架构
```
Box (外层) - 手势优先级：LOW
  ├─ pointerInput: detectDragGestures
  │   └─ 只处理非调整状态的左右滑动
  │   └─ 严格的方向识别
  │
  └─ Box (进度条区域) - 手势优先级：HIGH
      ├─ pointerInput: detectDragGestures
      │   └─ 长按进度条直接拖动调整
      │   └─ 松开立即跳转
      │
      └─ 进度条 (只在调整中显示)
          └─ 实时显示 adjustedProgress
```

---

## 状态管理

```kotlin
// 调整进度的标志和当前值
var isAdjustingProgress by remember { mutableStateOf(false) }
var adjustedProgress by remember { mutableStateOf(progress) }

// 手势状态
var dragStartX by remember { mutableStateOf(0f) }
var dragStartY by remember { mutableStateOf(0f) }
var accumulatedDeltaX by remember { mutableStateOf(0f) }
var accumulatedDeltaY by remember { mutableStateOf(0f) }
var hasReachedThreshold by remember { mutableStateOf(false) }
var gestureDirection by remember { 
    mutableStateOf<GestureDirection>(GestureDirection.NONE) 
}

// 同步进度：非调整时自动跟随播放进度
LaunchedEffect(progress) {
    if (!isAdjustingProgress) {
        adjustedProgress = progress
    }
}
```

---

## 工作流程示例

### 场景 A: 左滑切换歌曲
```
1. 用户在 PlaybackBar 上快速左滑
2. onDragStart: dragStartX=100, dragStartY=100, accumulatedDeltaX=0
3. onDrag (1st): accumulatedDeltaX=-50, accumulatedDeltaY=-10
   → absX=50, absY=10, 未达阈值
4. onDrag (2nd): accumulatedDeltaX=-120, accumulatedDeltaY=-20
   → absX=120, absY=20, 达到阈值
   → 120 > 20*2.0 → gestureDirection = LEFT
5. onDragEnd: 
   → LEFT 且 distance > 100px
   → player.next() ✓
```

### 场景 B: 长按进度条调整进度
```
1. 用户按住进度条区域
2. onDragStart (进度条): 
   → isAdjustingProgress = true
   → adjustedProgress = 0.3 (点击位置)
3. onDrag: 
   → change.consume() (阻止外层处理)
   → adjustedProgress += dragAmount.x / width
   → 进度条实时显示
4. onDragEnd:
   → newPosition = adjustedProgress * duration
   → player.seekTo(newPosition) ✓
   → isAdjustingProgress = false
   → UI 恢复
```

### 场景 C: 对角线滑动（防误操作）
```
1. 用户斜向滑动（从左上到右下）
2. onDrag: accumulatedDeltaX=60, accumulatedDeltaY=50
   → absX=60, absY=50, 达到阈值
   → 60 > 50*2.0? NO (60/50=1.2 < 2.0)
   → gestureDirection = NONE ✓
3. onDragEnd:
   → gestureDirection = NONE
   → 不执行任何操作
```

---

## 枚举定义

```kotlin
private enum class GestureDirection {
    NONE,   // 不确定（对角线、微弱移动等）
    LEFT,   // 左滑
    RIGHT,  // 右滑
    UP,     // 上滑
    DOWN    // 下滑（保留给未来功能）
}
```

---

## 关键改进点

| 项目 | 修改前 | 修改后 |
|------|--------|--------|
| 进度条裁剪 | 无（超出圆角） | `.clip(RoundedCornerShape(2.dp))` |
| 方向识别 | `absX > absY` 简单比较 | `absX > absY * 2.0f` 严格判定 |
| 阈值 | 30px | 50px |
| 长按方式 | `detectTapGestures.onLongPress` | 直接在进度条区域拖动 |
| 进度条显示 | 一直显示 | 仅在调整时显示 |
| 状态控制 | `showProgressMode` | `isAdjustingProgress` + `adjustedProgress` |

---

## 编译状态
✓ 编译成功，无错误

---

## 测试检查清单

- [ ] 快速左滑 → 播放下一首
- [ ] 快速右滑 → 播放上一首
- [ ] 上滑 → 播放下一首
- [ ] 长按进度条 → 进度条出现，UI 淡化
- [ ] 长按状态下拖动左右 → 实时调整进度
- [ ] 松开 → 立即跳转，UI 恢复
- [ ] 对角线滑动 → 不触发任何操作
- [ ] 微弱横移（< 50px）→ 不触发切换
- [ ] 播放中 → 实时进度更新流畅

