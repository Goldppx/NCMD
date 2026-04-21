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

data class SearchDetailUiState(
    val tracks: List<TrackItem> = emptyList(),
    val playlistsForMenu: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class SearchDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get(appContext)
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(SearchDetailUiState())
    val uiState: StateFlow<SearchDetailUiState> = _uiState.asStateFlow()

    fun loadTracks(type: String, id: Long) {
        viewModelScope.launch {
            val cookie = sessionManager.getCookie()
            val result: Result<List<TrackItem>> = when (type) {
                "playlist" -> apiService.getPlaylistDetail(id, cookie)
                "album" -> apiService.getAlbumTracks(id, cookie)
                else -> Result.success(emptyList())
            }

            result.fold(
                onSuccess = { trackList ->
                    _uiState.update {
                        it.copy(
                            tracks = trackList,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    fun refresh(type: String, id: Long) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        loadTracks(type, id)
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

    fun removeTrackById(trackId: Long) {
        _uiState.update { state ->
            state.copy(tracks = state.tracks.filterNot { it.id == trackId })
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.addTrackToPlaylist(playlistId, trackId, cookie)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.removeTracksFromPlaylist(playlistId, listOf(trackId), cookie)
    }

    fun getCookie(): String = sessionManager.getCookie()
}
