package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class PlaylistListUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PlaylistListRefreshResult {
    data class Success(val count: Int) : PlaylistListRefreshResult
    data class Error(val message: String?) : PlaylistListRefreshResult
    data object Timeout : PlaylistListRefreshResult
    data object Skipped : PlaylistListRefreshResult
}

class PlaylistListViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get(appContext)
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(PlaylistListUiState())
    val uiState: StateFlow<PlaylistListUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists(isRefresh = false, onFinished = null)
    }

    fun refresh(onFinished: ((PlaylistListRefreshResult) -> Unit)? = null) {
        loadPlaylists(isRefresh = true, onFinished = onFinished)
    }

    private fun loadPlaylists(
        isRefresh: Boolean,
        onFinished: ((PlaylistListRefreshResult) -> Unit)?
    ) {
        val cookie = sessionManager.getCookie()
        val userId = sessionManager.getUserId()

        if (userId <= 0L || cookie.isBlank()) {
            _uiState.update {
                it.copy(
                    playlists = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
            onFinished?.invoke(PlaylistListRefreshResult.Skipped)
            return
        }

        _uiState.update {
            it.copy(
                isLoading = it.playlists.isEmpty() && !isRefresh,
                isRefreshing = isRefresh,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = withTimeoutOrNull(10_000L) {
                apiService.getUserPlaylists(userId, cookie)
            }

            result?.fold(
                onSuccess = { response ->
                    val playlists = response.playlist.orEmpty()
                    _uiState.update {
                        it.copy(
                            playlists = playlists,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                    onFinished?.invoke(PlaylistListRefreshResult.Success(playlists.size))
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message
                        )
                    }
                    onFinished?.invoke(PlaylistListRefreshResult.Error(e.message))
                }
            ) ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
                onFinished?.invoke(PlaylistListRefreshResult.Timeout)
            }
        }
    }
}
