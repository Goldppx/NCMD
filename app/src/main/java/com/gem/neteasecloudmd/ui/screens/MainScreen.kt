package com.gem.neteasecloudmd.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.rememberPlayerManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToPlaylistList: () -> Unit,
    onNavigateToRecentPlays: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlaylistDetail: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val sessionManager by remember { mutableStateOf(SessionManager(context)) }
    val apiService = remember { NeteaseApiService() }
    val player = rememberPlayerManager(context)
    
    val isLoggedIn = sessionManager.isLoggedIn()
    val nickname = sessionManager.getNickname()
    val avatarUrl = sessionManager.getAvatarUrl()
    val userId = sessionManager.getUserId()
    val cookie = sessionManager.getCookie()
    
    var playlists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var recentPlays by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    LaunchedEffect(Unit) {
        player.setApiService(apiService)
    }
    
    fun loadRecentPlays() {
        scope.launch {
            recentPlays = player.getRecentPlays().take(3)
        }
    }
    
    fun loadPlaylists(showToast: Boolean = false) {
        if (isLoggedIn && userId > 0 && cookie.isNotEmpty()) {
            scope.launch {
                val result = withTimeoutOrNull(10000L) {
                    apiService.getUserPlaylists(userId, cookie)
                }
                result?.fold(
                    onSuccess = { response ->
                        playlists = response.playlist ?: emptyList()
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "刷新成功，共${playlists.size}个歌单", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "刷新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) ?: run {
                    isLoading = false
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, "请求超时", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            loadPlaylists()
            loadRecentPlays()
        }
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
                    onLogoutClick = {
                        sessionManager.logout()
                        scope.launch { drawerState.close() }
                        playlists = emptyList()
                        recentPlays = emptyList()
                    },
                    onCopyCookieClick = {
                        val clip = android.content.ClipData.newPlainText("cookie", cookie)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Cookie已复制", Toast.LENGTH_SHORT).show()
                        scope.launch { drawerState.close() }
                    },
                    onNavigate = {
                        scope.launch { drawerState.close() }
                    },
                    onRecentPlaysClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToRecentPlays()
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> }
                },
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "NCMD",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLoggedIn) {
                                Text(
                                    text = nickname,
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
                        IconButton(onClick = {
                            isRefreshing = true
                            loadPlaylists(showToast = true)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
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
                        isRefreshing = isRefreshing,
                        onRefresh = { loadPlaylists(showToast = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            !isLoggedIn -> {
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
                            isLoading && playlists.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (recentPlays.isNotEmpty()) {
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
                                                recentPlays.take(3).forEachIndexed { index, track ->
                                                    if (index == 0) {
                                                        PreviewRecentPlayLargeCard(
                                                            track = track,
                                                            onClick = {
                                                                player.setCookie(cookie)
                                                                player.setPlaylist(recentPlays, recentPlays.indexOf(track))
                                                                Toast.makeText(context, "播放: ${track.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    } else {
                                                        PreviewRecentPlaySmallCard(
                                                            track = track,
                                                            onClick = {
                                                                player.setCookie(cookie)
                                                                player.setPlaylist(recentPlays, recentPlays.indexOf(track))
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
                                            playlists.take(5).forEach { playlist ->
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
                                    
                                    if (playlists.isEmpty()) {
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
    onLogoutClick: () -> Unit,
    onCopyCookieClick: () -> Unit,
    onNavigate: () -> Unit,
    onRecentPlaysClick: () -> Unit = {}
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

        if (!isLoggedIn) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
        } else {
            FilledTonalButton(
                onClick = onCopyCookieClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("复制Cookie")
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
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
    val player = rememberPlayerManager(context)

    val hasPlaylist = player.currentPlaylist.isNotEmpty()
    val currentTrack = player.currentTrack
    val currentPosition = player.currentPosition
    val duration = player.duration
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    val coverSize = 56.dp
    val coverOffset = 12.dp

    var isAdjustingProgress by remember { mutableStateOf(false) }
    var adjustedProgress by remember { mutableStateOf(progress) }

    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }
    var accumulatedDeltaX by remember { mutableStateOf(0f) }
    var accumulatedDeltaY by remember { mutableStateOf(0f) }
    var hasReachedThreshold by remember { mutableStateOf(false) }
    var gestureDirection by remember { mutableStateOf<GestureDirection>(GestureDirection.NONE) }

    LaunchedEffect(progress) {
        if (!isAdjustingProgress) {
            adjustedProgress = progress
        }
    }

    val barShape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .zIndex(10f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!isAdjustingProgress) {
                            dragStartX = offset.x
                            dragStartY = offset.y
                            accumulatedDeltaX = 0f
                            accumulatedDeltaY = 0f
                            hasReachedThreshold = false
                            gestureDirection = GestureDirection.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isAdjustingProgress) return@detectDragGestures
                        
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
                                        player.next()
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
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isAdjustingProgress = true
                                adjustedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDrag = { change, dragAmount ->
                                if (!isAdjustingProgress) return@detectDragGestures
                                change.consume()
                                adjustedProgress = (adjustedProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                if (isAdjustingProgress) {
                                    val newPosition = (adjustedProgress * duration).toInt()
                                    player.seekTo(newPosition)
                                    isAdjustingProgress = false
                                    adjustedProgress = progress
                                }
                            }
                        )
                    }
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
                    .align(Alignment.BottomStart)
                    .offset(x = coverOffset + 16.dp, y = (-8).dp)
                    .size(coverSize)
                    .zIndex(11f)
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
                .padding(start = if (hasPlaylist) coverOffset + coverSize + 28.dp else 32.dp, end = 28.dp),
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
