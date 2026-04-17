package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.SongLongPressMenu
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDetailScreen(
    type: String,
    id: Long,
    name: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val apiService = remember { NeteaseApiService(context) }
    val player = rememberPlayerManager(context)
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    val cookie = sessionManager.getCookie()

    var tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }
    var playlistsForMenu by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }

    fun loadMenuPlaylists() {
        if (cookie.isBlank()) return
        scope.launch {
            val result = apiService.getUserPlaylists(sessionManager.getUserId(), cookie)
            playlistsForMenu = result.getOrNull()?.playlist.orEmpty()
        }
    }

    fun loadTracks() {
        scope.launch {
            val result = when (type) {
                "playlist" -> apiService.getPlaylistDetail(id, cookie)
                "album" -> apiService.getAlbumTracks(id, cookie)
                else -> Result.success(emptyList())
            }

            result.fold(
                onSuccess = {
                    tracks = it
                    isLoading = false
                    isRefreshing = false
                    errorMessage = null
                },
                onFailure = { e ->
                    isLoading = false
                    isRefreshing = false
                    errorMessage = e.message
                }
            )
        }
    }

    LaunchedEffect(type, id) {
        loadTracks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadTracks()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.search_detail_load_failed, errorMessage ?: stringResource(R.string.common_unknown_error)), color = MaterialTheme.colorScheme.error)
                    }
                }

                tracks.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.search_detail_empty), style = MaterialTheme.typography.bodyLarge)
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
                                    player.setCookie(cookie)
                                    player.setPlaylist(tracks, 0)
                                    Toast.makeText(context, resources.getString(R.string.search_detail_start_play_all, tracks.size), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        itemsIndexed(tracks) { index, track ->
                            SearchDetailTrackCard(
                                track = track,
                                index = index + 1,
                                onLongClick = { selectedTrackForMenu = track },
                                onClick = {
                                    player.setCookie(cookie)
                                    player.setPlaylist(tracks, index)
                                    Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    SongLongPressMenu(
        track = selectedTrackForMenu,
        playlists = playlistsForMenu,
        onDismiss = { selectedTrackForMenu = null },
        onRequestLoadPlaylists = { loadMenuPlaylists() },
        onPlayTrack = { track ->
            val index = tracks.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setCookie(cookie)
                player.setPlaylist(tracks, index)
                Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
            }
        },
        onAddToQueue = { track ->
            player.appendToQueue(listOf(track))
            Toast.makeText(context, resources.getString(R.string.song_menu_queue_success), Toast.LENGTH_SHORT).show()
        },
        onAddToPlaylist = { trackId, playlistId ->
            scope.launch {
                val result = apiService.addTrackToPlaylist(playlistId, trackId, cookie)
                result.fold(
                    onSuccess = {
                        Toast.makeText(context, resources.getString(R.string.song_menu_add_success), Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            resources.getString(R.string.song_menu_add_failed, e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        },
        onCopyShareLink = { track ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val link = "https://music.163.com/#/song?id=${track.id}"
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.common_search), link))
            Toast.makeText(context, resources.getString(R.string.song_menu_share_link_copied), Toast.LENGTH_SHORT).show()
        },
        onRemoveFromCurrent = { track ->
            if (type != "playlist") {
                Toast.makeText(context, resources.getString(R.string.song_menu_remove_not_supported), Toast.LENGTH_SHORT).show()
                return@SongLongPressMenu
            }
            scope.launch {
                val result = apiService.removeTracksFromPlaylist(id, listOf(track.id), cookie)
                result.fold(
                    onSuccess = {
                        tracks = tracks.filterNot { it.id == track.id }
                        Toast.makeText(context, resources.getString(R.string.song_menu_remove_success), Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            resources.getString(R.string.song_menu_remove_failed, e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        },
        showCopyShareLink = true,
        showRemoveFromCurrent = type == "playlist"
    )
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
                    contentDescription = stringResource(R.string.playlist_detail_play_all),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.playlist_detail_play_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.playlist_detail_track_count, trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchDetailTrackCard(
    track: TrackItem,
    index: Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
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
                        Text(stringResource(R.string.main_music_symbol))
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
        }
    }
}
