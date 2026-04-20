package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PlaylistDetailUiState(
    val tracks: List<TrackItem> = emptyList(),
    val likedSongIds: Set<Long> = emptySet(),
    val playlistsForMenu: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PlaylistDetailRefreshResult {
    data class Success(val count: Int) : PlaylistDetailRefreshResult
    data class Error(val message: String?) : PlaylistDetailRefreshResult
    data object Timeout : PlaylistDetailRefreshResult
    data object Skipped : PlaylistDetailRefreshResult
}

class PlaylistDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get(appContext)
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    fun loadPlaylist(
        playlistId: Long,
        isRefresh: Boolean,
        onFinished: ((PlaylistDetailRefreshResult) -> Unit)? = null
    ) {
        val cookie = sessionManager.getCookie()
        val userId = sessionManager.getUserId()

        if (playlistId <= 0L || cookie.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
            onFinished?.invoke(PlaylistDetailRefreshResult.Skipped)
            return
        }

        _uiState.update {
            it.copy(
                isLoading = it.tracks.isEmpty() && !isRefresh,
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = withTimeoutOrNull(15_000L) {
                apiService.getPlaylistDetail(playlistId, cookie)
            }

            result?.fold(
                onSuccess = { trackList ->
                    val likedSongIds = apiService.getLikedSongIds(userId, cookie).getOrDefault(emptySet())
                    _uiState.update {
                        it.copy(
                            tracks = trackList,
                            likedSongIds = likedSongIds,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                    onFinished?.invoke(PlaylistDetailRefreshResult.Success(trackList.size))
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message
                        )
                    }
                    onFinished?.invoke(PlaylistDetailRefreshResult.Error(e.message))
                }
            ) ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
                onFinished?.invoke(PlaylistDetailRefreshResult.Timeout)
            }
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

    fun removeTracksLocally(trackIds: Set<Long>) {
        _uiState.update { state ->
            state.copy(
                tracks = state.tracks.filterNot { trackIds.contains(it.id) },
                likedSongIds = state.likedSongIds.filterNot { trackIds.contains(it) }.toSet()
            )
        }
    }

    suspend fun addTrackToPlaylist(targetPlaylistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.addTrackToPlaylist(targetPlaylistId, trackId, cookie)
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.removeTracksFromPlaylist(playlistId, trackIds, cookie)
    }

    fun getCookie(): String = sessionManager.getCookie()
}
