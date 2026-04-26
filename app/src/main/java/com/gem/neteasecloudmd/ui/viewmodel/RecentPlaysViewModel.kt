package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentPlaysUiState(
    val recentPlays: List<TrackItem> = emptyList(),
    val playlistsForMenu: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val useLocalRecentPlays: Boolean = true,
    val likedSongIds: Set<Long> = emptySet()
)

@androidx.annotation.OptIn(UnstableApi::class)
class RecentPlaysViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get(appContext)
    private val sessionManager = SessionManager(appContext)
    private val playerManager = PlayerManager.getInstance(appContext)

    private val _uiState = MutableStateFlow(
        RecentPlaysUiState(
            useLocalRecentPlays = sessionManager.useLocalRecentPlays()
        )
    )
    val uiState: StateFlow<RecentPlaysUiState> = _uiState.asStateFlow()

    init {
        loadRecentPlays()
    }

    fun loadRecentPlays() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    useLocalRecentPlays = sessionManager.useLocalRecentPlays()
                )
            }

            val cookie = sessionManager.getCookie()
            val userId = sessionManager.getUserId()
            val useLocal = sessionManager.useLocalRecentPlays()

            val recentPlays = if (useLocal) {
                playerManager.getRecentPlays()
            } else {
                apiService.getUserPlayRecord(userId, cookie, 100).getOrDefault(emptyList())
            }

            val likedIds = if (userId > 0L && cookie.isNotBlank()) {
                apiService.getLikedSongIds(userId, cookie).getOrNull() ?: emptySet()
            } else {
                emptySet()
            }

            _uiState.update {
                it.copy(
                    recentPlays = recentPlays,
                    likedSongIds = likedIds,
                    isLoading = false,
                    useLocalRecentPlays = useLocal
                )
            }
        }
    }

    fun toggleSongLike(songId: Long) {
        val cookie = sessionManager.getCookie()
        val userId = sessionManager.getUserId()
        if (cookie.isBlank() || userId <= 0L) return
        val currentlyLiked = _uiState.value.likedSongIds.contains(songId)

        viewModelScope.launch {
            apiService.setSongLiked(songId, !currentlyLiked, cookie, userId)
            val ids = apiService.getLikedSongIds(userId, cookie).getOrNull() ?: emptySet()
            _uiState.update { it.copy(likedSongIds = ids) }
            playerManager.updateLikedSongIds(ids)
        }
    }

    fun loadMenuPlaylists() {
        val cookie = sessionManager.getCookie()
        val userId = sessionManager.getUserId()
        if (cookie.isBlank() || userId <= 0L) return

        viewModelScope.launch {
            val result = apiService.getUserPlaylists(userId, cookie)
            _uiState.update { it.copy(playlistsForMenu = result.getOrNull()?.playlist.orEmpty()) }
        }
    }

    fun removeLocalRecent(trackIds: Set<Long>) {
        if (trackIds.isEmpty()) return
        viewModelScope.launch {
            trackIds.forEach { id -> playerManager.removeRecentPlay(id) }
            _uiState.update { state ->
                state.copy(recentPlays = state.recentPlays.filterNot { trackIds.contains(it.id) })
            }
        }
    }

    suspend fun addTrackToPlaylist(targetPlaylistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.addTrackToPlaylist(targetPlaylistId, trackId, cookie)
    }

    fun getCookie(): String = sessionManager.getCookie()
}
