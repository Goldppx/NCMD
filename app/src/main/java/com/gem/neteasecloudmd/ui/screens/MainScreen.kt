package com.gem.neteasecloudmd.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.api.rememberPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToPlaylistList: () -> Unit,
    onNavigateToRecentPlays: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylistDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val player = rememberPlayerManager(context)

    val hasPlaylist = player.currentPlaylist.isNotEmpty()
    val currentTrack = player.currentTrack
    val duration = player.duration
    val progress = if (duration > 0) player.currentPosition.toFloat() / duration.toFloat() else 0f

    var isAdjustingProgress by remember { mutableStateOf(false) }
    var adjustedProgress by remember { mutableStateOf(progress) }

    LaunchedEffect(progress) {
        if (!isAdjustingProgress) {
            adjustedProgress = progress
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isAdjustingProgress) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "contentAlpha"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("NCMD") },
                navigationIcon = {
                    IconButton(onClick = { /* Open drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 当前播放信息
            if (hasPlaylist) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = currentTrack?.name ?: "未播放",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentTrack?.artists ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { adjustedProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 导航按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onNavigateToPlaylistList) {
                    Text("播放列表")
                }
                Button(onClick = onNavigateToRecentPlays) {
                    Text("最近播放")
                }
                Button(onClick = onNavigateToSearch) {
                    Text("搜索")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部播放栏
            PlaybackBar(
                isAdjustingProgress = isAdjustingProgress,
                onAdjustingProgressChange = { isAdjustingProgress = it },
                adjustedProgress = adjustedProgress,
                onAdjustedProgressChange = { adjustedProgress = it }
            )
        }
    }
}

@Composable
fun PlaybackBar(
    isAdjustingProgress: Boolean,
    onAdjustingProgressChange: (Boolean) -> Unit,
    adjustedProgress: Float,
    onAdjustedProgressChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val player = rememberPlayerManager(context)

    val hasPlaylist = player.currentPlaylist.isNotEmpty()
    val currentTrack = player.currentTrack
    val duration = player.duration
    val progress = if (duration > 0) player.currentPosition.toFloat() / duration.toFloat() else 0f

    val coverSize = 56.dp
    val coverOffset = 12.dp

    val contentAlpha by animateFloatAsState(
        targetValue = if (isAdjustingProgress) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "contentAlpha"
    )

    val barCornerRadius = 28.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(10f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onAdjustedProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                        onAdjustingProgressChange(true)
                    },
                    onDrag = { change, dragAmount ->
                        if (!isAdjustingProgress) return@detectDragGestures
                        change.consume()
                        onAdjustedProgressChange((adjustedProgress + dragAmount.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        if (isAdjustingProgress) {
                            val newPosition = (adjustedProgress * duration).toInt()
                            player.seekTo(newPosition)
                            onAdjustingProgressChange(false)
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(barCornerRadius))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shape = RoundedCornerShape(barCornerRadius)
            ) {}

            if (hasPlaylist && !isAdjustingProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(barCornerRadius))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                )
            }
        }

        if (hasPlaylist) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = coverOffset)
                    .size(coverSize)
                    .zIndex(11f)
                    .alpha(contentAlpha)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    if (currentTrack?.albumPicUrl != null) {
                        AsyncImage(
                            model = currentTrack.albumPicUrl,
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("♪", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(start = if (hasPlaylist) coverOffset + coverSize + 12.dp else 16.dp, end = 12.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPlaylist) currentTrack?.name ?: "未播放" else "未播放",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasPlaylist) currentTrack?.artists ?: "" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasPlaylist) {
                IconButton(onClick = { player.togglePlayPause() }) {
                    Icon(
                        if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (player.isPlaying) "Pause" else "Play"
                    )
                }
            }
        }
    }
}
