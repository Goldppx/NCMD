package com.gem.neteasecloudmd.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.rememberPlayerManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val apiService = remember { NeteaseApiService() }
    val player = rememberPlayerManager(context)
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var refreshKey by remember { mutableLongStateOf(0L) }
    var tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var likedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val cookie = sessionManager.getCookie()
    
    LaunchedEffect(Unit) {
        player.setApiService(apiService)
    }
    
    fun loadTracks(showToast: Boolean = false) {
        if (playlistId > 0 && cookie.isNotEmpty()) {
            scope.launch {
                val result = withTimeoutOrNull(15000L) {
                    apiService.getPlaylistDetail(playlistId, cookie)
                }
                result?.fold(
                    onSuccess = { trackList ->
                        tracks = trackList
                        likedSongIds = apiService.getLikedSongIds(sessionManager.getUserId(), cookie).getOrDefault(emptySet())
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "已加载 ${trackList.size} 首歌曲", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
    
    LaunchedEffect(refreshKey) {
        loadTracks()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlistName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        refreshKey++
                        loadTracks(showToast = true)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    refreshKey++
                    loadTracks(showToast = true)
                },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && tracks.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null && tracks.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "错误: $errorMessage",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    tracks.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "歌单为空",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                PlayAllCard(
                                    trackCount = tracks.size,
                                    onClick = {
                                        if (tracks.isNotEmpty()) {
                                            player.setCookie(cookie)
                                            player.setPlaylist(tracks, 0)
                                            Toast.makeText(context, "开始播放全部 ${tracks.size} 首", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            itemsIndexed(tracks) { index, track ->
                                SongCard(
                                    track = track,
                                    index = index + 1,
                                    isLiked = likedSongIds.contains(track.id),
                                    onClick = {
                                        player.setCookie(cookie)
                                        player.setPlaylist(tracks, index)
                                        Toast.makeText(context, "播放: ${track.name}", Toast.LENGTH_SHORT).show()
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

@Composable
private fun PlayAllCard(
    trackCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play All",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "播放全部",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$trackCount 首歌曲",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SongCard(
    track: TrackItem,
    index: Int,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
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
                        Text("♪")
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
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
            
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isLiked) "已喜欢" else "未喜欢",
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
