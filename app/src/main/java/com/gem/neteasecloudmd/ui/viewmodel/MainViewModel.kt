package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val nickname: String = "",
    val avatarUrl: String? = null,
    val userId: Long = 0L,
    val cookie: String = "",
    val playlists: List<PlaylistItem> = emptyList(),
    val recentPlays: List<TrackItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val sessionManager = SessionManager(appContext)
    val apiService = NeteaseApiService()

    private val _uiState = MutableStateFlow(
        MainUiState(
            isLoggedIn = sessionManager.isLoggedIn(),
            nickname = sessionManager.getNickname(),
            avatarUrl = sessionManager.getAvatarUrl(),
            userId = sessionManager.getUserId(),
            cookie = sessionManager.getCookie()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadHomeData(isRefresh = false)
    }

    fun refresh() {
        loadHomeData(isRefresh = true)
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
                isRefreshing = isRefresh,
                isLoading = !isRefresh,
                errorMessage = null
            )
        }

        if (!isLoggedIn || userId <= 0 || cookie.isBlank()) {
            _uiState.update {
                it.copy(
                    playlists = emptyList(),
                    recentPlays = emptyList(),
                    isLoading = false,
                    isRefreshing = false
                )
            }
            return
        }

        viewModelScope.launch {
            val recentPlays = PlayerManager.getInstance(appContext).getRecentPlays().take(3)

            val playlistsResult = withTimeoutOrNull(10000L) {
                apiService.getUserPlaylists(userId, cookie)
            }

            playlistsResult?.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            playlists = response.playlist ?: emptyList(),
                            recentPlays = recentPlays,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            recentPlays = recentPlays,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message
                        )
                    }
                }
            ) ?: run {
                _uiState.update {
                    it.copy(
                        recentPlays = recentPlays,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "请求超时"
                    )
                }
            }
        }
    }
}
