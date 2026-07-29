package com.gem.neteasecloudmd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gem.neteasecloudmd.api.ApiProvider
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SearchAlbumItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab {
    SONG,
    PLAYLIST,
    ALBUM
}

data class SearchUiState(
    val query: String = "",
    val selectedTab: SearchTab = SearchTab.SONG,
    val songResults: List<TrackItem> = emptyList(),
    val playlistResults: List<PlaylistItem> = emptyList(),
    val albumResults: List<SearchAlbumItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val playlistsForMenu: List<PlaylistItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchFailed: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get()
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null
    private var searchJob: Job? = null
    private var requestId = 0L

    fun updateQuery(query: String) {
        requestId += 1
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                hasSearched = false,
                isSearching = false,
                searchFailed = false,
                songResults = emptyList(),
                playlistResults = emptyList(),
                albumResults = emptyList()
            )
        }

        suggestJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        val suggestionRequestId = requestId
        suggestJob = viewModelScope.launch {
            delay(250)
            val suggestions = apiService.searchSuggest(query.trim())
                .getOrDefault(emptyList())
                .take(8)
            if (suggestionRequestId == requestId) {
                _uiState.update { it.copy(suggestions = suggestions) }
            }
        }
    }

    fun selectTab(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        val state = _uiState.value
        if (state.hasSearched && state.query.isNotBlank()) {
            performSearch()
        }
    }

    fun selectSuggestion(suggestion: String) {
        requestId += 1
        _uiState.update { it.copy(query = suggestion) }
        performSearch()
    }

    fun performSearch() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank()) return

        searchJob?.cancel()
        val searchRequestId = ++requestId
        val selectedTab = state.selectedTab
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    hasSearched = true,
                    isSearching = true,
                    searchFailed = false,
                    suggestions = emptyList()
                )
            }

            when (selectedTab) {
                SearchTab.SONG -> {
                    apiService.searchSongs(query, 30).fold(
                        onSuccess = { songs ->
                            val detailedSongs = if (songs.isNotEmpty()) {
                                apiService.getSongsDetails(songs.map { it.id }, getCookie())
                                    .getOrDefault(songs)
                            } else {
                                songs
                            }
                            updateSearchResult(searchRequestId) {
                                it.copy(songResults = detailedSongs, isSearching = false)
                            }
                        },
                        onFailure = {
                            updateSearchFailure(searchRequestId)
                        }
                    )
                }

                SearchTab.PLAYLIST -> {
                    apiService.searchPlaylists(query, 30).fold(
                        onSuccess = { playlists ->
                            updateSearchResult(searchRequestId) {
                                it.copy(playlistResults = playlists, isSearching = false)
                            }
                        },
                        onFailure = {
                            updateSearchFailure(searchRequestId)
                        }
                    )
                }

                SearchTab.ALBUM -> {
                    apiService.searchAlbums(query, 30).fold(
                        onSuccess = { albums ->
                            updateSearchResult(searchRequestId) {
                                it.copy(albumResults = albums, isSearching = false)
                            }
                        },
                        onFailure = {
                            updateSearchFailure(searchRequestId)
                        }
                    )
                }
            }
        }
    }

    private fun updateSearchResult(
        searchRequestId: Long,
        update: (SearchUiState) -> SearchUiState
    ) {
        if (searchRequestId == requestId) {
            _uiState.update(update)
        }
    }

    private fun updateSearchFailure(searchRequestId: Long) {
        updateSearchResult(searchRequestId) {
            it.copy(isSearching = false, searchFailed = true)
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

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.addTrackToPlaylist(playlistId, trackId, cookie)
    }

    fun getCookie(): String = sessionManager.getCookie()
}
