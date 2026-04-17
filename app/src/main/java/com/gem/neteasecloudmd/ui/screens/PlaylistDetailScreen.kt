package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.TrackCollectionScaffold
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val apiService = remember { NeteaseApiService(context) }
    val player = rememberPlayerManager(context)
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var likedSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playlistsForMenu by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }

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
                            Toast.makeText(
                                context,
                                resources.getString(R.string.playlist_detail_loaded_songs, trackList.size),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.playlist_detail_load_failed, e.message ?: ""),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) ?: run {
                    isLoading = false
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, resources.getString(R.string.common_request_timeout), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }

    fun loadMenuPlaylists() {
        if (cookie.isBlank()) return
        scope.launch {
            val result = apiService.getUserPlaylists(sessionManager.getUserId(), cookie)
            playlistsForMenu = result.getOrNull()?.playlist.orEmpty()
        }
    }

    LaunchedEffect(Unit) {
        loadTracks()
    }

    TrackCollectionScaffold(
        title = playlistName,
        tracks = tracks,
        playlistsForMenu = playlistsForMenu,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onNavigateBack = onNavigateBack,
        onRefresh = {
            isRefreshing = true
            loadTracks(showToast = true)
        },
        isRefreshing = isRefreshing,
        onBatchModeChanged = { hidden ->
            player.updatePlaybackBarHidden(hidden)
        },
        onPlayAll = {
            if (tracks.isNotEmpty()) {
                player.setCookie(cookie)
                player.setPlaylist(tracks, 0)
                Toast.makeText(
                    context,
                    resources.getString(R.string.playlist_detail_start_play_all, tracks.size),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onPlayTrackAt = { index, track ->
            player.setCookie(cookie)
            player.setPlaylist(tracks, index)
            Toast.makeText(
                context,
                resources.getString(R.string.main_play_track_toast, track.name),
                Toast.LENGTH_SHORT
            ).show()
        },
        onPlayTrackFromMenu = { track ->
            val index = tracks.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setCookie(cookie)
                player.setPlaylist(tracks, index)
                Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
            }
        },
        onAddTrackToQueue = { track ->
            player.appendToQueue(listOf(track))
            Toast.makeText(context, resources.getString(R.string.song_menu_queue_success), Toast.LENGTH_SHORT).show()
        },
        onAddTracksToQueue = { selectedIds ->
            val selectedTracks = tracks.filter { selectedIds.contains(it.id) }
            player.appendToQueue(selectedTracks)
            Toast.makeText(
                context,
                resources.getString(R.string.playlist_detail_batch_queue_success, selectedTracks.size),
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveSingleFromCurrent = { track ->
            scope.launch {
                val result = apiService.removeTracksFromPlaylist(playlistId, listOf(track.id), cookie)
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
        onRemoveBatchFromCurrent = { selectedIds ->
            scope.launch {
                val result = apiService.removeTracksFromPlaylist(playlistId, selectedIds.toList(), cookie)
                result.fold(
                    onSuccess = {
                        tracks = tracks.filterNot { selectedIds.contains(it.id) }
                        likedSongIds = likedSongIds.filterNot { selectedIds.contains(it) }.toSet()
                        Toast.makeText(
                            context,
                            resources.getString(R.string.playlist_detail_batch_remove_success, selectedIds.size),
                            Toast.LENGTH_SHORT
                        ).show()
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
        onRequestLoadPlaylists = { loadMenuPlaylists() },
        onAddSingleToPlaylist = { trackId, targetPlaylistId ->
            scope.launch {
                val result = apiService.addTrackToPlaylist(targetPlaylistId, trackId, cookie)
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
        onAddBatchToPlaylist = { selectedIds, targetPlaylistId ->
            scope.launch {
                var successCount = 0
                selectedIds.forEach { trackId ->
                    val result = apiService.addTrackToPlaylist(targetPlaylistId, trackId, cookie)
                    if (result.getOrDefault(false)) successCount++
                }
                Toast.makeText(
                    context,
                    resources.getString(R.string.playlist_detail_batch_add_success, successCount),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onCopyShareLink = { track ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val link = "https://music.163.com/#/song?id=${track.id}"
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.common_search), link))
            Toast.makeText(context, resources.getString(R.string.song_menu_share_link_copied), Toast.LENGTH_SHORT).show()
        },
        showRemoveFromCurrentInMenu = true,
        onPlaySelected = { selectedIds ->
            val selectedTracks = tracks.filter { selectedIds.contains(it.id) }
            player.setCookie(cookie)
            player.setPlaylist(selectedTracks, 0)
            Toast.makeText(
                context,
                resources.getString(R.string.playlist_detail_batch_play_success, selectedTracks.size),
                Toast.LENGTH_SHORT
            ).show()
        },
        showCopyShareLinkInMenu = true,
        emptyText = resources.getString(R.string.playlist_detail_empty),
        showLikeIndicator = true,
        isTrackLiked = { track -> likedSongIds.contains(track.id) },
        onSingleRightAction = null,
        singleRightActionDescription = ""
    )
}
