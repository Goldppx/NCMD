package com.gem.neteasecloudmd.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylistDetail: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val apiService = remember { NeteaseApiService(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var refreshKey by remember { mutableLongStateOf(0L) }
    var playlists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val cookie = sessionManager.getCookie()
    val userId = sessionManager.getUserId()
    
    fun loadPlaylists(showToast: Boolean = false) {
        if (userId > 0 && cookie.isNotEmpty()) {
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
                            Toast.makeText(context, context.getString(R.string.playlist_refresh_success, playlists.size), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, context.getString(R.string.playlist_refresh_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) ?: run {
                    isLoading = false
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, context.getString(R.string.common_request_timeout), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(refreshKey) {
        loadPlaylists()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlist_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        refreshKey++
                        loadPlaylists(showToast = true)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
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
                    loadPlaylists(showToast = true)
                },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null && playlists.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.common_error_with_prefix, errorMessage ?: ""),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    playlists.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_empty),
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
                            items(playlists) { playlist ->
                                PlaylistListCard(
                                    playlist = playlist,
                                    onClick = {
                                        onNavigateToPlaylistDetail(playlist.id, playlist.name)
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
private fun PlaylistListCard(
    playlist: PlaylistItem,
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
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
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
                        Text(stringResource(R.string.main_music_symbol))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.main_track_count_no_space, playlist.trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
