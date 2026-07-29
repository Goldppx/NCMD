package com.gem.neteasecloudmd.core.library

import com.gem.neteasecloudmd.core.model.Track
import com.gem.neteasecloudmd.core.playback.PlaybackState
import com.gem.neteasecloudmd.core.playback.PlaybackStatus
import com.gem.neteasecloudmd.core.playback.QueueState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LibraryDestination {
    HOME,
    SEARCH,
    QUEUE
}

/**
 * Platform-independent state for a small music library experience.
 *
 * Platform modules provide persistence, networking, and the actual media engine. This store
 * only owns deterministic UI intent: navigation, filtering, queue selection, and play/pause.
 */
data class LibraryUiState(
    val destination: LibraryDestination = LibraryDestination.HOME,
    val query: String = "",
    val catalog: List<Track> = emptyList(),
    val playback: PlaybackState = PlaybackState()
) {
    val visibleTracks: List<Track>
        get() = when (destination) {
            LibraryDestination.SEARCH -> {
                val normalizedQuery = query.trim()
                if (normalizedQuery.isEmpty()) emptyList() else {
                    catalog.filter { track ->
                        track.name.contains(normalizedQuery, ignoreCase = true) ||
                            track.artists.contains(normalizedQuery, ignoreCase = true) ||
                            track.albumName.contains(normalizedQuery, ignoreCase = true)
                    }
                }
            }

            LibraryDestination.HOME -> catalog
            LibraryDestination.QUEUE -> playback.queue.items
        }
}

class LibraryStore(initialCatalog: List<Track>) {
    private val _state = MutableStateFlow(
        LibraryUiState(
            catalog = initialCatalog,
            playback = PlaybackState(queue = QueueState(items = initialCatalog))
        )
    )
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    fun navigate(destination: LibraryDestination) {
        _state.update { it.copy(destination = destination) }
    }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun selectTrack(trackId: Long) {
        _state.update { state ->
            val index = state.playback.queue.items.indexOfFirst { it.id == trackId }
            if (index < 0) return@update state

            state.copy(
                playback = state.playback.copy(
                    queue = state.playback.queue.copy(currentIndex = index),
                    status = PlaybackStatus.READY,
                    isPlaying = true,
                    errorMessage = null
                )
            )
        }
    }

    fun togglePlayback() {
        _state.update { state ->
            val hasTrack = state.playback.currentTrack != null
            state.copy(
                playback = state.playback.copy(
                    status = if (hasTrack) PlaybackStatus.READY else PlaybackStatus.IDLE,
                    isPlaying = hasTrack && !state.playback.isPlaying
                )
            )
        }
    }
}
