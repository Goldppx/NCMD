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
    val hasSearched: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val apiService = ApiProvider.get()
    private val sessionManager = SessionManager(appContext)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                hasSearched = false,
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

        suggestJob = viewModelScope.launch {
            delay(250)
            val suggestions = apiService.searchSuggest(query.trim())
                .getOrDefault(emptyList())
                .take(8)
            _uiState.update { it.copy(suggestions = suggestions) }
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
        _uiState.update { it.copy(query = suggestion) }
        performSearch()
    }

    fun performSearch() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(hasSearched = true, isSearching = true) }

            when (_uiState.value.selectedTab) {
                SearchTab.SONG -> {
                    val songs = apiService.searchSongs(query, 30).getOrDefault(emptyList())
                    val detailedSongs = if (songs.isNotEmpty()) {
                        val ids = songs.map { it.id }
                        val cookie = getCookie()
                        apiService.getSongsDetails(ids, cookie).getOrDefault(songs)
                    } else {
                        songs
                    }
                    _uiState.update { it.copy(songResults = detailedSongs, isSearching = false) }
                }

                SearchTab.PLAYLIST -> {
                    val playlists = apiService.searchPlaylists(query, 30).getOrDefault(emptyList())
                    _uiState.update { it.copy(playlistResults = playlists, isSearching = false) }
                }

                SearchTab.ALBUM -> {
                    val albums = apiService.searchAlbums(query, 30).getOrDefault(emptyList())
                    _uiState.update { it.copy(albumResults = albums, isSearching = false) }
                }
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

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): Result<Boolean> {
        val cookie = sessionManager.getCookie()
        if (cookie.isBlank()) return Result.failure(IllegalStateException("Not logged in"))
        return apiService.addTrackToPlaylist(playlistId, trackId, cookie)
    }

    fun getCookie(): String = sessionManager.getCookie()
}
