package com.gem.neteasecloudmd.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.PlaybackQueueContent
import com.gem.neteasecloudmd.ui.components.PlaybackQueueSheet
import com.gem.neteasecloudmd.ui.components.SongLongPressMenu
import com.gem.neteasecloudmd.ui.viewmodel.MainViewModel
import com.gem.neteasecloudmd.utils.LyricParser
import com.gem.neteasecloudmd.utils.LyricLine
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
    val player = rememberPlayerManager(context)
    val track = player.currentTrack
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity
    
    // Immersive mode for landscape
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

    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSideQueue by remember { mutableStateOf(false) }
    var showLandscapeControls by remember { mutableStateOf(true) }
    var infoDisplayMode by remember { mutableIntStateOf(2) } // 0: None, 1: Text, 2: Text + Shadow
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }
    var playlistsForMenu by remember { mutableStateOf(emptyList<com.gem.neteasecloudmd.api.PlaylistItem>()) }

    fun loadMenuPlaylists() {
        scope.launch {
            val result = mainViewModel.apiService.getUserPlaylists(
                uiState.userId,
                uiState.cookie
            )
            result.onSuccess {
                playlistsForMenu = it.playlist ?: emptyList()
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
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.ScreenRotation,
                                    contentDescription = "Rotate",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { /* More options */ }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(animationSpec = tween(500))
                ) {
                    // Top Content (Cover + Lyrics)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .displayCutoutPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        infoDisplayMode = (infoDisplayMode + 1) % 3
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        if (dragAmount < -15) showLandscapeControls = true // Swipe Up
                                        if (dragAmount > 15) showLandscapeControls = false // Swipe Down
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
                                infoDisplayMode = infoDisplayMode
                            )
                        }
                        Box(modifier = Modifier.weight(1.2f)) {
                            PlayerLyricsPage(player, isLandscape = true)
                        }
                    }

                    // Dynamic Bottom Bar
                    AnimatedVisibility(
                        visible = showLandscapeControls,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Playback Controls (Left)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { player.previous() }) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                                IconButton(onClick = { player.togglePlayPause() }) {
                                    if (player.isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(
                                            imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { player.next() }) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
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
                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = Color.White)
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
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { /* More */ }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
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
                            isLandscape = false
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
                    modifier = Modifier.fillMaxSize(),
                    maxHeight = false
                )
            }
        }

        if (showQueueSheet) {
            PlaybackQueueSheet(
                player = player,
                onDismiss = { showQueueSheet = false },
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
            showRemoveFromCurrent = true
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlayerMainPage(
    player: PlayerManager,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowQueue: () -> Unit,
    isLandscape: Boolean,
    showOnlyInfo: Boolean = false,
    infoDisplayMode: Int = 2 // Default to Text + Shadow
) {
    val track = player.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                            contentDescription = "Previous",
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
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscape) 32.dp else 56.dp)
                            )
                        }
                    }

                    IconButton(onClick = { player.next() }) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
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
                IconButton(onClick = { /* Favorite */ }) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                }
                IconButton(onClick = onShowQueue) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", tint = Color.White)
                }
            }
        }
    }
}

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
                                player.seekTo(line.time.toInt())
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
