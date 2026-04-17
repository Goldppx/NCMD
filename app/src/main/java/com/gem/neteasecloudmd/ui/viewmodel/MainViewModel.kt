package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import com.gem.neteasecloudmd.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val avatarUrl: String? = null,
    val userId: Long = 0L,
    val cookie: String = "",
    val playlists: List<PlaylistItem> = emptyList(),
    val recentPlays: List<TrackItem> = emptyList(),
    val personalFmTracks: List<TrackItem> = emptyList(),
    val likedSongIds: Set<Long> = emptySet(),
    val useLocalRecentPlays: Boolean = true,
    val isRecentLoading: Boolean = false,
    val isPlaylistLoading: Boolean = false,
    val isFmLoading: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@androidx.annotation.OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val sessionManager = SessionManager(appContext)
    val apiService = NeteaseApiService(appContext)

    private val _uiState = MutableStateFlow(
        MainUiState(
            isLoggedIn = sessionManager.isLoggedIn(),
            nickname = sessionManager.getNickname(),
            avatarUrl = sessionManager.getAvatarUrl(),
            userId = sessionManager.getUserId(),
            cookie = sessionManager.getCookie(),
            useLocalRecentPlays = sessionManager.useLocalRecentPlays()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadHomeData(isRefresh = false)
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        loadHomeData(isRefresh = true)
    }

    fun setUseLocalRecentPlays(useLocal: Boolean) {
        sessionManager.setUseLocalRecentPlays(useLocal)
        _uiState.update { it.copy(useLocalRecentPlays = useLocal) }
        loadHomeData(isRefresh = true)
    }

    fun toggleSongLike(songId: Long) {
        val state = _uiState.value
        if (!state.isLoggedIn || state.cookie.isBlank() || state.userId <= 0) return

        val currentlyLiked = state.likedSongIds.contains(songId)
        viewModelScope.launch {
            val result = apiService.setSongLiked(songId, !currentlyLiked, state.cookie)
            result.onSuccess {
                val ids = apiService.getLikedSongIds(state.userId, state.cookie).getOrNull() ?: emptySet()
                _uiState.update { it.copy(likedSongIds = ids) }
            }
        }
    }

    fun startPersonalFm() {
        val state = _uiState.value
        if (state.personalFmTracks.isNotEmpty()) {
            val playerManager = PlayerManager.getInstance(appContext)
            playerManager.setApiService(apiService)
            playerManager.setCookie(state.cookie)
            playerManager.setPersonalFmPlaylist(state.personalFmTracks, 0)
        }
    }

    private fun loadHomeData(isRefresh: Boolean) {
        val isLoggedIn = sessionManager.isLoggedIn()
        val userId = sessionManager.getUserId()
        val cookie = sessionManager.getCookie()

        _uiState.update {
            it.copy(
                isLoggedIn = isLoggedIn,
                nickname = sessionManager.getNickname(),
                avatarUrl = sessionManager.getAvatarUrl(),
                userId = userId,
                cookie = cookie,
                useLocalRecentPlays = sessionManager.useLocalRecentPlays(),
                isRecentLoading = true,
                isPlaylistLoading = true,
                isFmLoading = true,
                isRefreshing = isRefresh,
                isLoading = true,
                errorMessage = null
            )
        }

        if (!isLoggedIn || userId <= 0 || cookie.isBlank()) {
            _uiState.update {
                it.copy(
                    playlists = emptyList(),
                    recentPlays = emptyList(),
                    personalFmTracks = emptyList(),
                    likedSongIds = emptySet(),
                    isRecentLoading = false,
                    isPlaylistLoading = false,
                    isFmLoading = false,
                    isLoading = false,
                    isRefreshing = false
                )
            }
            return
        }

        val pendingSections = AtomicInteger(3)
        val markSectionDone = {
            if (pendingSections.decrementAndGet() <= 0) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }

        viewModelScope.launch {
            val useLocalRecentPlays = sessionManager.useLocalRecentPlays()
            val localRecent = PlayerManager.getInstance(appContext).getRecentPlays()
            val recentPlays = if (useLocalRecentPlays) {
                localRecent.take(3)
            } else {
                apiService.getUserPlayRecord(userId, cookie, 30).getOrDefault(emptyList()).take(3)
            }

            _uiState.update {
                it.copy(
                    recentPlays = recentPlays,
                    isRecentLoading = false
                )
            }
            markSectionDone()
        }

        viewModelScope.launch {
            val personalFmTracks = apiService.getPersonalFm(cookie, 6).getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    personalFmTracks = personalFmTracks,
                    isFmLoading = false
                )
            }
            markSectionDone()
        }

        viewModelScope.launch {
            val likedSongIds = apiService.getLikedSongIds(userId, cookie).getOrDefault(emptySet())
            _uiState.update { it.copy(likedSongIds = likedSongIds) }
        }

        viewModelScope.launch {
            val playlistsResult = withTimeoutOrNull(10000L) {
                apiService.getUserPlaylists(userId, cookie)
            }

            playlistsResult?.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            playlists = response.playlist ?: emptyList(),
                            isPlaylistLoading = false,
                            errorMessage = null
                        )
                    }
                    markSectionDone()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isPlaylistLoading = false,
                            errorMessage = e.message
                        )
                    }
                    markSectionDone()
                }
            ) ?: run {
                _uiState.update {
                        it.copy(
                            isPlaylistLoading = false,
                            errorMessage = appContext.getString(R.string.viewmodel_request_timeout)
                        )
                    }
                markSectionDone()
            }
        }
    }
}
