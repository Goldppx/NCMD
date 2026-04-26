package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.TrackCollectionScaffold
import com.gem.neteasecloudmd.ui.viewmodel.RecentPlaysViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentPlaysScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val player = rememberPlayerManager(context)
    val recentPlaysViewModel: RecentPlaysViewModel = viewModel()
    val uiState by recentPlaysViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val cookie = recentPlaysViewModel.getCookie()

    fun loadMenuPlaylists() {
        recentPlaysViewModel.loadMenuPlaylists()
    }

    fun removeLocalRecent(trackIds: Set<Long>) {
        if (trackIds.isEmpty()) {
            Toast.makeText(context, resources.getString(R.string.playlist_detail_batch_need_selection), Toast.LENGTH_SHORT).show()
            return
        }
        recentPlaysViewModel.removeLocalRecent(trackIds)
        Toast.makeText(context, resources.getString(R.string.playlist_detail_batch_remove_success, trackIds.size), Toast.LENGTH_SHORT).show()
    }

    TrackCollectionScaffold(
        title = resources.getString(R.string.recent_title),
        tracks = uiState.recentPlays,
        playlistsForMenu = uiState.playlistsForMenu,
        isLoading = uiState.isLoading,
        errorMessage = null,
        onNavigateBack = onNavigateBack,
        onRefresh = null,
        isRefreshing = false,
        onBatchModeChanged = { hidden ->
            player.updatePlaybackBarHidden(hidden)
        },
        onPlayAll = {
            if (uiState.recentPlays.isNotEmpty()) {
                player.setCookie(cookie)
                player.setPlaylist(uiState.recentPlays, 0)
                Toast.makeText(
                    context,
                    resources.getString(R.string.recent_start_play_all, uiState.recentPlays.size),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onPlayTrackAt = { index, track ->
            player.setCookie(cookie)
            player.setPlaylist(uiState.recentPlays, index)
            Toast.makeText(
                context,
                resources.getString(R.string.main_play_track_toast, track.name),
                Toast.LENGTH_SHORT
            ).show()
        },
        onPlayTrackFromMenu = { track ->
            val index = uiState.recentPlays.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setCookie(cookie)
                player.setPlaylist(uiState.recentPlays, index)
                Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
            }
        },
        onAddTrackToQueue = { track ->
            player.appendToQueue(listOf(track))
            Toast.makeText(context, resources.getString(R.string.song_menu_queue_success), Toast.LENGTH_SHORT).show()
        },
        onAddTracksToQueue = { selectedIds ->
            val selectedTracks = uiState.recentPlays.filter { selectedIds.contains(it.id) }
            player.appendToQueue(selectedTracks)
            Toast.makeText(
                context,
                resources.getString(R.string.playlist_detail_batch_queue_success, selectedTracks.size),
                Toast.LENGTH_SHORT
            ).show()
        },
        onRemoveSingleFromCurrent = { track ->
            removeLocalRecent(setOf(track.id))
        },
        onRemoveBatchFromCurrent = { selectedIds ->
            removeLocalRecent(selectedIds)
        },
        onRequestLoadPlaylists = { loadMenuPlaylists() },
        onAddSingleToPlaylist = { trackId, targetPlaylistId ->
            scope.launch {
                val result = recentPlaysViewModel.addTrackToPlaylist(targetPlaylistId, trackId)
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
                    val result = recentPlaysViewModel.addTrackToPlaylist(targetPlaylistId, trackId)
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
            val selectedTracks = uiState.recentPlays.filter { selectedIds.contains(it.id) }
            player.setCookie(cookie)
            player.setPlaylist(selectedTracks, 0)
            Toast.makeText(
                context,
                resources.getString(R.string.playlist_detail_batch_play_success, selectedTracks.size),
                Toast.LENGTH_SHORT
            ).show()
        },
        showCopyShareLinkInMenu = false,
        emptyText = resources.getString(R.string.recent_empty),
        showLikeIndicator = true,
        isTrackLiked = { track -> uiState.likedSongIds.contains(track.id) },
        onLikeToggle = { track -> recentPlaysViewModel.toggleSongLike(track.id) },
        onSingleRightAction = { track ->
            removeLocalRecent(setOf(track.id))
        },
        singleRightActionDescription = resources.getString(R.string.recent_remove_song)
    )
}
