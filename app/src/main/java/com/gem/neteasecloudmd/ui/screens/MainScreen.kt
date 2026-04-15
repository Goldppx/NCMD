package com.gem.neteasecloudmd.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToPlaylistList: () -> Unit,
    onNavigateToRecentPlays: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlaylistDetail: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    
    val player = rememberPlayerManager(context)
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    LaunchedEffect(Unit) {
        player.setApiService(mainViewModel.apiService)
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                DrawerContent(
                    onLoginClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSearch()
                    },
                    onRecentPlaysClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToRecentPlays()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "NCMD",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.isLoggedIn) {
                                Text(
                                    text = uiState.nickname,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { mainViewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            !uiState.isLoggedIn -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "请先登录",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(onClick = onNavigateToSearch) {
                                            Text("登录")
                                        }
                                    }
                                }
                            }
                            uiState.isLoading && uiState.playlists.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            uiState.errorMessage != null && uiState.playlists.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.errorMessage ?: "加载失败",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (uiState.recentPlays.isNotEmpty()) {
                                        item {
                                            SectionHeader(
                                                title = "最近播放",
                                                onExpandClick = onNavigateToRecentPlays
                                            )
                                        }
                                        
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                uiState.recentPlays.take(3).forEachIndexed { index, track ->
                                                    if (index == 0) {
                                                        PreviewRecentPlayLargeCard(
                                                            track = track,
                                                            onClick = {
                                                                player.setCookie(uiState.cookie)
                                                                player.setPlaylist(uiState.recentPlays, uiState.recentPlays.indexOf(track))
                                                                Toast.makeText(context, "播放: ${track.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    } else {
                                                        PreviewRecentPlaySmallCard(
                                                            track = track,
                                                            onClick = {
                                                                player.setCookie(uiState.cookie)
                                                                player.setPlaylist(uiState.recentPlays, uiState.recentPlays.indexOf(track))
                                                                Toast.makeText(context, "播放: ${track.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    item {
                                        SectionHeader(
                                            title = "我的歌单",
                                            onExpandClick = onNavigateToPlaylistList
                                        )
                                    }
                                    
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            uiState.playlists.take(5).forEach { playlist ->
                                                PreviewPlaylistCard(
                                                    playlist = playlist,
                                                    onClick = {
                                                        onNavigateToPlaylistDetail(playlist.id, playlist.name)
                                                    },
                                                    modifier = Modifier.width(140.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (uiState.playlists.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "暂无歌单",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerContent(
    onLoginClick: () -> Unit,
    onRecentPlaysClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager by remember { mutableStateOf(SessionManager(context)) }
    val isLoggedIn = sessionManager.isLoggedIn()
    val nickname = sessionManager.getNickname()
    val avatarUrl = sessionManager.getAvatarUrl()
    val userId = sessionManager.getUserId()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = if (isLoggedIn) nickname else "未登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = if (isLoggedIn) {
                { Text("UID: $userId") }
            } else null,
            leadingContent = {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUrl != null && isLoggedIn) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoggedIn) {
            FilledTonalButton(
                onClick = onRecentPlaysClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("最近播放")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        FilledTonalButton(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("设置")
        }

        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onExpandClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Expand"
            )
        }
    }
}

@Composable
private fun PreviewPlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (playlist.coverImgUrl != null) {
                    AsyncImage(
                        model = playlist.coverImgUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("♪", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.trackCount}首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewRecentPlayLargeCard(
    track: TrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (track.albumPicUrl != null) {
                    AsyncImage(
                        model = track.albumPicUrl,
                        contentDescription = track.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("♪", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewRecentPlaySmallCard(
    track: TrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = track.artists,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackBar(
    showPlayBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!showPlayBar) return

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val player = rememberPlayerManager(context)
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val disableCoverOverflow = sessionManager.isCoverOverflowDisabled()
    val enableCoverPalette = sessionManager.isCoverPaletteEnabled()

    val hasPlaylist = player.currentPlaylist.isNotEmpty()
    val currentTrack = player.currentTrack
    val currentPosition = player.currentPosition
    val duration = player.duration
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    val latestProgress by rememberUpdatedState(progress)
    val latestDuration by rememberUpdatedState(duration)

    val coverSize = if (disableCoverOverflow) 56.dp else 64.dp
    val coverOffset = 12.dp
    val coverAlignment = if (disableCoverOverflow) Alignment.CenterStart else Alignment.BottomStart
    val coverYOffset = if (disableCoverOverflow) 0.dp else (-8).dp

    var isAdjustingProgress by remember { mutableStateOf(false) }
    var adjustedProgress by remember { mutableFloatStateOf(progress) }
    var longPressStartProgress by remember { mutableFloatStateOf(progress) }

    var accumulatedDeltaX by remember { mutableFloatStateOf(0f) }
    var accumulatedDeltaY by remember { mutableFloatStateOf(0f) }
    var hasReachedThreshold by remember { mutableStateOf(false) }
    var gestureDirection by remember { mutableStateOf<GestureDirection>(GestureDirection.NONE) }
    var showQueueSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentTrack?.albumPicUrl, enableCoverPalette) {
        if (!enableCoverPalette) {
            player.setThemeSeedColor(0)
            return@LaunchedEffect
        }

        val coverUrl = currentTrack?.albumPicUrl
        if (coverUrl.isNullOrBlank()) {
            player.setThemeSeedColor(0)
            return@LaunchedEffect
        }

        try {
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
            if (bitmap == null) {
                player.setThemeSeedColor(0)
                return@LaunchedEffect
            }

            val palette = Palette.from(bitmap).generate()
            val pickedColor = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: palette.getDominantColor(0)
            player.setThemeSeedColor(pickedColor)
        } catch (_: Exception) {
            player.setThemeSeedColor(0)
        }
    }

    LaunchedEffect(progress) {
        if (!isAdjustingProgress) {
            adjustedProgress = progress
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isAdjustingProgress) 0f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "playbackBarContentAlpha"
    )

    val barShape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .zIndex(10f)
            .pointerInput(hasPlaylist, isAdjustingProgress) {
                detectDragGestures(
                    onDragStart = {
                        if (hasPlaylist && !isAdjustingProgress) {
                            accumulatedDeltaX = 0f
                            accumulatedDeltaY = 0f
                            hasReachedThreshold = false
                            gestureDirection = GestureDirection.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!hasPlaylist || isAdjustingProgress) return@detectDragGestures

                        change.consume()
                        
                        accumulatedDeltaX += dragAmount.x
                        accumulatedDeltaY += dragAmount.y

                        if (!hasReachedThreshold) {
                            val absX = kotlin.math.abs(accumulatedDeltaX)
                            val absY = kotlin.math.abs(accumulatedDeltaY)
                            
                            if (absX > 50 || absY > 50) {
                                hasReachedThreshold = true
                                when {
                                    absX > absY * 2.0f -> {
                                        gestureDirection = if (accumulatedDeltaX > 0) {
                                            GestureDirection.RIGHT
                                        } else {
                                            GestureDirection.LEFT
                                        }
                                    }
                                    absY > absX * 2.0f -> {
                                        gestureDirection = if (accumulatedDeltaY > 0) {
                                            GestureDirection.DOWN
                                        } else {
                                            GestureDirection.UP
                                        }
                                    }
                                    else -> {
                                        gestureDirection = GestureDirection.NONE
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (!hasPlaylist || isAdjustingProgress) return@detectDragGestures

                        if (hasReachedThreshold) {
                            when (gestureDirection) {
                                GestureDirection.LEFT -> {
                                    if (kotlin.math.abs(accumulatedDeltaX) > 100) {
                                        player.next()
                                    }
                                }
                                GestureDirection.RIGHT -> {
                                    if (kotlin.math.abs(accumulatedDeltaX) > 100) {
                                        player.previous()
                                    }
                                }
                                GestureDirection.UP -> {
                                    if (kotlin.math.abs(accumulatedDeltaY) > 100) {
                                        showQueueSheet = true
                                    }
                                }
                                GestureDirection.DOWN -> {}
                                GestureDirection.NONE -> {}
                            }
                        }
                        
                        accumulatedDeltaX = 0f
                        accumulatedDeltaY = 0f
                        hasReachedThreshold = false
                        gestureDirection = GestureDirection.NONE
                    }
                )
            }
            .pointerInput(hasPlaylist) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        if (!hasPlaylist) return@detectDragGesturesAfterLongPress
                        isAdjustingProgress = true
                        longPressStartProgress = latestProgress
                        adjustedProgress = latestProgress
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        if (!isAdjustingProgress || !hasPlaylist) return@detectDragGesturesAfterLongPress
                        change.consume()
                        adjustedProgress = (adjustedProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (isAdjustingProgress && hasPlaylist && latestDuration > 0) {
                            val newPosition = (adjustedProgress * latestDuration).toInt()
                            player.seekTo(newPosition)
                        }
                        isAdjustingProgress = false
                    },
                    onDragCancel = {
                        adjustedProgress = longPressStartProgress
                        isAdjustingProgress = false
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(barShape)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
                shape = barShape
            ) {}

            if (hasPlaylist && !isAdjustingProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        )
                )
            }
        }

        if (hasPlaylist) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(horizontal = 16.dp)
            ) {
                if (isAdjustingProgress) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(adjustedProgress)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        if (hasPlaylist) {
            Box(
                modifier = Modifier
                    .align(coverAlignment)
                    .offset(x = coverOffset + 16.dp, y = coverYOffset)
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
                .padding(start = if (hasPlaylist) coverOffset + coverSize + 24.dp else 32.dp, end = 28.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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

        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放队列",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${player.currentPlaylist.size} 首",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { player.updatePlayMode(com.gem.neteasecloudmd.api.PlayMode.SEQUENTIAL) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.SEQUENTIAL) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            contentColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.SEQUENTIAL) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = "顺序播放")
                    }
                    FilledTonalIconButton(
                        onClick = { player.updatePlayMode(com.gem.neteasecloudmd.api.PlayMode.SHUFFLE) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.SHUFFLE) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            contentColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.SHUFFLE) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "随机播放")
                    }
                    FilledTonalIconButton(
                        onClick = { player.updatePlayMode(com.gem.neteasecloudmd.api.PlayMode.REPEAT_ONE) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.REPEAT_ONE) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            contentColor = if (player.playMode == com.gem.neteasecloudmd.api.PlayMode.REPEAT_ONE) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Icon(Icons.Default.RepeatOne, contentDescription = "单曲循环")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            player.clearPlaylist()
                            showQueueSheet = false
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清空")
                    }
                }

                if (player.currentPlaylist.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无播放队列",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                )
                {
                    itemsIndexed(player.currentPlaylist) { index, track ->
                        val isCurrent = index == player.currentTrackIndex
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.width(28.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artists,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { player.removeTrackAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class GestureDirection {
    NONE, LEFT, RIGHT, UP, DOWN
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
