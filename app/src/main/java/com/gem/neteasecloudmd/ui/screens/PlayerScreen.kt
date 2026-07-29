package com.gem.neteasecloudmd.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.PlaybackQueueContent
import com.gem.neteasecloudmd.ui.components.PlaybackQueueSheet
import com.gem.neteasecloudmd.ui.components.SongLongPressMenu
import com.gem.neteasecloudmd.ui.viewmodel.MainViewModel
import com.gem.neteasecloudmd.utils.LyricParser
import com.gem.neteasecloudmd.utils.LyricLine
import com.gem.neteasecloudmd.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val player = rememberPlayerManager(context)
    val apiService = remember { ApiProvider.get() }
    val track = player.currentTrack
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity

    // The player always draws a dark, cover-derived background. Keep system icons light even
    // when the application uses the light Material theme.
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    
    // Immersive mode and Auto-rotation support
    LaunchedEffect(isLandscape) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Set orientation to follow system settings (Auto-rotate)
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val sessionManager = remember { SessionManager(context) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSideQueue by remember { mutableStateOf(false) }
    var showLandscapeControls by remember { mutableStateOf(sessionManager.isLandscapeControlsVisible()) }
    var infoDisplayMode by remember { mutableStateOf(sessionManager.getLandscapeInfoMode()) } // 0: None, 1: Text, 2: Text + Shadow
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }
    var playlistsForMenu by remember { mutableStateOf(emptyList<com.gem.neteasecloudmd.api.PlaylistItem>()) }

    val playerCookie = sessionManager.getCookie()
    val playerUserId = sessionManager.getUserId()
    val onLikeToggle: (Long) -> Unit = { songId ->
        val currentlyLiked = player.likedSongIds.contains(songId)
        scope.launch(Dispatchers.IO) {
            val result = apiService.setSongLiked(songId, !currentlyLiked, playerCookie, playerUserId)
            result.onSuccess {
                if (playerUserId > 0L && playerCookie.isNotBlank()) {
                    val ids = apiService.getLikedSongIds(playerUserId, playerCookie).getOrNull() ?: emptySet()
                    player.updateLikedSongIds(ids)
                }
            }
        }
    }

    fun loadMenuPlaylists() {
        scope.launch {
            mainViewModel.apiService.getUserPlaylists(
                uiState.userId,
                uiState.cookie
            )
                .onSuccess {
                    playlistsForMenu = it.playlist ?: emptyList()
                }
                .onFailure { e ->
                    Logger.e("PlayerScreen", "Failed to load playlists: ${e.message}")
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -20) { // Swipe from right
                        showSideQueue = true
                    } else if (dragAmount > 20) { // Swipe from left
                        showSideQueue = false
                    }
                }
            }
    ) {
        // Blurred Background with Crossfade
        AnimatedContent(
            targetState = track?.albumPicUrl,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(1000)) togetherWith 
                fadeOut(animationSpec = androidx.compose.animation.core.tween(1000))
            },
            label = "BackgroundCrossfade"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Dark Overlay / Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (!isLandscape) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (pagerState.currentPage == 0) stringResource(R.string.player_now_playing) else stringResource(R.string.player_lyrics),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.common_back),
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* More options */ }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.common_more), tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                if (isLandscape) {
                    // Hidden or shown via AnimatedVisibility
                }
            }
        ) { paddingValues ->
            if (isLandscape) {
                val bottomBarHeight = 84.dp
                val animatedBottomPadding by animateDpAsState(
                    targetValue = if (showLandscapeControls) bottomBarHeight else 0.dp,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    label = "BottomBarPadding"
                )

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main Content Area (Cover + Lyrics)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = animatedBottomPadding)
                            .displayCutoutPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        infoDisplayMode = (infoDisplayMode + 1) % 3
                                        sessionManager.setLandscapeInfoMode(infoDisplayMode)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        if (dragAmount < -15) {
                                            showLandscapeControls = true // Swipe Up
                                            sessionManager.setLandscapeControlsVisible(true)
                                        }
                                        if (dragAmount > 15) {
                                            showLandscapeControls = false // Swipe Down
                                            sessionManager.setLandscapeControlsVisible(false)
                                        }
                                    }
                                }
                        ) {
                            PlayerMainPage(
                                player = player,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onShowQueue = { showSideQueue = true },
                                isLandscape = true,
                                showOnlyInfo = true,
                                infoDisplayMode = infoDisplayMode,
                                onLikeToggle = onLikeToggle
                            )
                        }
                        Box(modifier = Modifier.weight(1.2f)) {
                            PlayerLyricsPage(player, isLandscape = true)
                        }
                    }

                    // Bottom Bar (Overlay)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        AnimatedVisibility(
                            visible = showLandscapeControls,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(bottomBarHeight)
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 16.dp)
                                    .navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Playback Controls (Left)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { player.previous() }) {
                                        Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.player_skip_previous), tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                    IconButton(onClick = { player.togglePlayPause() }) {
                                        if (player.isLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(
                                                imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = stringResource(R.string.player_play_pause),
                                                tint = Color.White,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { player.next() }) {
                                        Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.player_skip_next), tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Slider (Center)
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatTime(player.currentPosition),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Slider(
                                        value = player.currentPosition.toFloat(),
                                        onValueChange = { player.seekTo(it.toInt()) },
                                        valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                    Text(
                                        formatTime(player.duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Extra Actions (Right)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showSideQueue = true }) {
                                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.main_queue_title), tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Top Overlay (Independent of Column)
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.common_back),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { /* More */ }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.common_more), tint = Color.White)
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { page ->
                    when (page) {
                        0 -> PlayerMainPage(
                            player = player,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onShowQueue = { showQueueSheet = true },
                            isLandscape = false,
                            onLikeToggle = onLikeToggle
                        )
                        1 -> PlayerLyricsPage(player, isLandscape = false)
                    }
                }
            }
        }

        // Side Queue Drawer (Landscape/Portrait)
        AnimatedVisibility(
            visible = showSideQueue,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                PlaybackQueueContent(
                    player = player,
                    onDismiss = { showSideQueue = false },
                    onTrackLongClick = { track -> selectedTrackForMenu = track },
                    likedSongIds = player.likedSongIds,
                    onLikeToggle = onLikeToggle,
                    modifier = Modifier.fillMaxSize(),
                    maxHeight = false
                )
            }
        }

    if (showQueueSheet) {
        PlaybackQueueSheet(
            player = player,
            onDismiss = { showQueueSheet = false },
            likedSongIds = player.likedSongIds,
            onLikeToggle = onLikeToggle,
                onTrackLongClick = { track -> selectedTrackForMenu = track }
            )
        }

        SongLongPressMenu(
            track = selectedTrackForMenu,
            playlists = playlistsForMenu,
            onDismiss = { selectedTrackForMenu = null },
            onRequestLoadPlaylists = { loadMenuPlaylists() },
            onPlayTrack = { track ->
                val index = player.currentPlaylist.indexOfFirst { it.id == track.id }
                if (index >= 0) {
                    player.seekToTrack(index)
                    player.play()
                }
            },
            onAddToQueue = { track ->
                player.appendToQueue(listOf(track))
            },
            onAddToPlaylist = { trackId, playlistId ->
                scope.launch {
                    mainViewModel.apiService.addTrackToPlaylist(playlistId, trackId, uiState.cookie)
                        .onSuccess {
                            Toast.makeText(context, resources.getString(R.string.song_menu_add_success), Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { e ->
                            Logger.e("PlayerScreen", "Failed to add to playlist: ${e.message}")
                        }
                }
            },
            onCopyShareLink = { track ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val link = "https://music.163.com/#/song?id=${track.id}"
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Link", link))
            },
            onRemoveFromCurrent = { track ->
                val index = player.currentPlaylist.indexOfFirst { it.id == track.id }
                if (index >= 0) {
                    player.removeTrackAt(index)
                }
            },
            showCopyShareLink = true,
            showRemoveFromCurrent = true,
            isLiked = selectedTrackForMenu?.let { player.likedSongIds.contains(it.id) } ?: false,
            onLikeToggle = { track -> onLikeToggle(track.id) }
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlayerMainPage(
    player: PlayerManager,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowQueue: () -> Unit,
    isLandscape: Boolean,
    showOnlyInfo: Boolean = false,
    infoDisplayMode: Int = 2, // Default to Text + Shadow
    onLikeToggle: (Long) -> Unit = {}
) {
    val track = player.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(500))
            .padding(horizontal = if (isLandscape) 16.dp else 32.dp)
            .padding(vertical = if (isLandscape) 16.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.SpaceEvenly
    ) {
        // Cover Section (60% weight in landscape)
        Box(
            modifier = if (isLandscape) Modifier.weight(0.6f) else Modifier.wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Cover Art with Info Overlay
            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                        .sharedElement(
                            rememberSharedContentState(key = "cover"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .clip(RoundedCornerShape(if (isLandscape) 12.dp else 24.dp)),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        AnimatedContent(
                            targetState = track?.albumPicUrl,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "CoverCrossfade"
                        ) { url ->
                            if (url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isLandscape) 60.dp else 100.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isLandscape && infoDisplayMode > 0) {
                        // Gradient Overlay and Info
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (infoDisplayMode == 2) {
                                        Modifier.background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                startY = 100f
                                            )
                                        )
                                    } else Modifier
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text(
                                    text = track?.name ?: stringResource(R.string.main_unplayed),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track?.artists ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Info and Slider Section (20% weight in landscape)
        if (!showOnlyInfo) {
            Box(
                modifier = if (isLandscape) Modifier.weight(0.2f) else Modifier.wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    if (!isLandscape) {
                        with(sharedTransitionScope) {
                            Text(
                                text = track?.name ?: stringResource(R.string.main_unplayed),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "title"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                            Text(
                                text = track?.artists ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "artists"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Progress Slider
                    Column {
                        Slider(
                            value = player.currentPosition.toFloat(),
                            onValueChange = { player.seekTo(it.toInt()) },
                            valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(player.currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                formatTime(player.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Playback Controls Section (20% weight in landscape)
        if (!showOnlyInfo) {
            Box(
                modifier = if (isLandscape) Modifier.weight(0.2f) else Modifier.wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.previous() }) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.player_skip_previous),
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 28.dp else 48.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 48.dp else 80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { player.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (player.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(if (isLandscape) 20.dp else 32.dp))
                        } else {
                            Icon(
                                imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.player_play_pause),
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscape) 32.dp else 56.dp)
                            )
                        }
                    }

                    IconButton(onClick = { player.next() }) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.player_skip_next),
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 28.dp else 48.dp)
                        )
                    }
                }
            }
        }
        
        if (!isLandscape) {
            // Bottom Actions (Portrait only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val currentTrackId = track?.id ?: 0L
                val isCurrentLiked = currentTrackId != 0L && player.likedSongIds.contains(currentTrackId)
                IconButton(onClick = { if (currentTrackId != 0L) onLikeToggle(currentTrackId) }) {
                    Icon(
                        imageVector = if (isCurrentLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isCurrentLiked) stringResource(R.string.playlist_detail_liked) else stringResource(R.string.playlist_detail_unliked),
                        tint = if (isCurrentLiked) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = onShowQueue) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = stringResource(R.string.main_queue_title), tint = Color.White)
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerLyricsPage(player: PlayerManager, isLandscape: Boolean) {
    val lyrics = player.currentLyric
    val lyricLines = remember(lyrics) { LyricParser.parse(lyrics) }
    val listState = rememberLazyListState()
    
    val currentPosition = player.currentPosition
    val currentLineIndex by remember(currentPosition, lyricLines) {
        derivedStateOf {
            val index = lyricLines.indexOfLast { it.time <= currentPosition }
            if (index == -1) 0 else index
        }
    }

    LaunchedEffect(currentLineIndex) {
        if (lyricLines.isNotEmpty()) {
            listState.animateScrollToItem(currentLineIndex, scrollOffset = if (isLandscape) -100 else -200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 16.dp else 32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricLines.isEmpty()) {
            Text(
                text = if (lyrics.isNullOrBlank()) stringResource(R.string.player_no_lyrics) else lyrics,
                style = if (isLandscape) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.15f to Color.Black,
                                0.85f to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(vertical = if (isLandscape) 50.dp else 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    val alpha by animateFloatAsState(if (isCurrent) 1f else 0.4f)
                    val scale by animateFloatAsState(if (isCurrent) 1.1f else 1f)
                    
                    Text(
                        text = line.text,
                        style = (if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall).copy(
                            lineHeight = if (isLandscape) 32.sp else 40.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = ((if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall).fontSize.value * scale).sp
                        ),
                        color = Color.White.copy(alpha = alpha),
                        textAlign = if (isLandscape) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                player.seekTo(line.time.toInt().coerceIn(0, Int.MAX_VALUE))
                            }
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
