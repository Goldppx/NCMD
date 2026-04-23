package com.gem.neteasecloudmd.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.PlaybackQueueSheet
import com.gem.neteasecloudmd.ui.components.SongLongPressMenu
import com.gem.neteasecloudmd.ui.viewmodel.MainViewModel
import com.gem.neteasecloudmd.utils.LyricParser
import com.gem.neteasecloudmd.utils.LyricLine
import kotlinx.coroutines.launch

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
    
    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var showQueueSheet by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
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
                        onShowQueue = { showQueueSheet = true }
                    )
                    1 -> PlayerLyricsPage(player)
                }
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
    onShowQueue: () -> Unit
) {
    val track = player.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Large Cover Art with Crossfade
        with(sharedTransitionScope) {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .sharedElement(
                        rememberSharedContentState(key = "cover"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 16.dp
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
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Info and Controls
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls
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
                        modifier = Modifier.size(48.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { player.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (player.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                IconButton(onClick = { player.next() }) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        
        // Bottom Actions
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

@Composable
private fun PlayerLyricsPage(player: PlayerManager) {
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
            listState.animateScrollToItem(currentLineIndex, scrollOffset = -200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricLines.isEmpty()) {
            Text(
                text = if (lyrics.isNullOrBlank()) stringResource(R.string.player_no_lyrics) else lyrics,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    val alpha by animateFloatAsState(if (isCurrent) 1f else 0.4f)
                    val scale by animateFloatAsState(if (isCurrent) 1.1f else 1f)
                    
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            lineHeight = 40.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * scale).sp
                        ),
                        color = Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Start,
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
