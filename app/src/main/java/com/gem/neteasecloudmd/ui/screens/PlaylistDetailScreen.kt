package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.TrackCollectionScaffold
import com.gem.neteasecloudmd.ui.viewmodel.PlaylistDetailRefreshResult
import com.gem.neteasecloudmd.ui.viewmodel.PlaylistDetailViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    type: String = "playlist",
    playlistId: Long,
    playlistName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val player = rememberPlayerManager(context)
    val playlistDetailViewModel: PlaylistDetailViewModel = viewModel()
    val uiState by playlistDetailViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val cookie = playlistDetailViewModel.getCookie()

    fun loadTracks(showToast: Boolean = false) {
        playlistDetailViewModel.loadPlaylist(
            type = type,
            id = playlistId,
            isRefresh = showToast
        ) { result ->
            if (!showToast) return@loadPlaylist
            when (result) {
                is PlaylistDetailRefreshResult.Success -> {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.playlist_detail_loaded_songs, result.count),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is PlaylistDetailRefreshResult.Error -> {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.playlist_detail_load_failed, result.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                PlaylistDetailRefreshResult.Timeout -> {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.common_request_timeout),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                PlaylistDetailRefreshResult.Skipped -> Unit
            }
        }
    }

    fun loadMenuPlaylists() {
        playlistDetailViewModel.loadMenuPlaylists()
    }

    LaunchedEffect(Unit) {
        player.setApiService(ApiProvider.get(context))
        loadTracks()
    }

    TrackCollectionScaffold(
        title = playlistName,
        tracks = uiState.tracks,
        playlistsForMenu = uiState.playlistsForMenu,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onNavigateBack = onNavigateBack,
        onRefresh = {
            loadTracks(showToast = true)
        },
        isRefreshing = uiState.isRefreshing,
        onBatchModeChanged = { hidden ->
            player.updatePlaybackBarHidden(hidden)
        },
        onPlayAll = {
            if (uiState.tracks.isNotEmpty()) {
                player.setCookie(cookie)
                player.setPlaylist(uiState.tracks, 0)
                Toast.makeText(
                    context,
                    resources.getString(R.string.playlist_detail_start_play_all, uiState.tracks.size),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onPlayTrackAt = { index, track ->
            player.setCookie(cookie)
            player.setPlaylist(uiState.tracks, index)
            Toast.makeText(
                context,
                resources.getString(R.string.main_play_track_toast, track.name),
                Toast.LENGTH_SHORT
            ).show()
        },
        onPlayTrackFromMenu = { track ->
            val index = uiState.tracks.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setCookie(cookie)
                player.setPlaylist(uiState.tracks, index)
                Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
            }
        },
        onAddTrackToQueue = { track ->
            player.appendToQueue(listOf(track))
            Toast.makeText(context, resources.getString(R.string.song_menu_queue_success), Toast.LENGTH_SHORT).show()
        },
        onAddTracksToQueue = { selectedIds ->
            val selectedTracks = uiState.tracks.filter { selectedIds.contains(it.id) }
            player.appendToQueue(selectedTracks)
            Toast.makeText(
                context,
                resources.getString(R.string.playlist_detail_batch_queue_success, selectedTracks.size),
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveSingleFromCurrent = { track ->
            scope.launch {
                val result = playlistDetailViewModel.removeTracksFromPlaylist(playlistId, listOf(track.id))
                result.fold(
                    onSuccess = {
                        playlistDetailViewModel.removeTracksLocally(setOf(track.id))
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
                val result = playlistDetailViewModel.removeTracksFromPlaylist(playlistId, selectedIds.toList())
                result.fold(
                    onSuccess = {
                        playlistDetailViewModel.removeTracksLocally(selectedIds)
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
                val result = playlistDetailViewModel.addTrackToPlaylist(targetPlaylistId, trackId)
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
                    val result = playlistDetailViewModel.addTrackToPlaylist(targetPlaylistId, trackId)
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
            val selectedTracks = uiState.tracks.filter { selectedIds.contains(it.id) }
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
        isTrackLiked = { track -> uiState.likedSongIds.contains(track.id) },
        onLikeToggle = { track -> playlistDetailViewModel.toggleSongLike(track.id) },
        onSingleRightAction = null,
        singleRightActionDescription = "",
        onLoadMore = { playlistDetailViewModel.loadMore() },
        isLoadingMore = uiState.isLoadingMore,
        totalTrackCount = uiState.allTrackIds.size
    )
}
