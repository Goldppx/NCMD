package com.gem.neteasecloudmd.core.library

import com.gem.neteasecloudmd.core.model.Track
import com.gem.neteasecloudmd.core.playback.PlaybackState
import com.gem.neteasecloudmd.core.playback.PlaybackStatus
import com.gem.neteasecloudmd.core.playback.QueuePolicy
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

data class QueueRemovalResult(
    val removedTrack: Track,
    val replacementTrack: Track?,
    val removedCurrentTrack: Boolean,
    val shouldRestartPlayback: Boolean
)

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

    /**
     * Replaces the library supplied by a platform repository while preserving the selected track
     * when it still exists. This lets Android, Desktop, and future iOS repositories share the
     * same queue and filtering behavior.
     */
    fun replaceCatalog(catalog: List<Track>) {
        _state.update { state ->
            val currentTrackId = state.playback.currentTrack?.id
            val selectedIndex = catalog.indexOfFirst { it.id == currentTrackId }
            val hasSelectedTrack = selectedIndex >= 0
            val queue = QueueState(
                items = catalog,
                currentIndex = if (hasSelectedTrack) selectedIndex else 0,
                playMode = state.playback.queue.playMode
            )

            state.copy(
                catalog = catalog,
                playback = state.playback.copy(
                    queue = queue,
                    status = if (hasSelectedTrack) state.playback.status else PlaybackStatus.IDLE,
                    isPlaying = hasSelectedTrack && state.playback.isPlaying,
                    errorMessage = if (hasSelectedTrack) state.playback.errorMessage else null
                )
            )
        }
    }

    fun selectTrack(trackId: Long) {
        _state.update { state ->
            val index = state.playback.queue.items.indexOfFirst { it.id == trackId }
            if (index < 0) return@update state
            val selectedTrack = state.playback.queue.items[index]

            state.copy(
                playback = state.playback.copy(
                    queue = state.playback.queue.copy(currentIndex = index),
                    status = PlaybackStatus.READY,
                    isPlaying = true,
                    positionMs = 0L,
                    durationMs = selectedTrack.duration.toLong().coerceAtLeast(0L),
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

    fun selectNextTrack(): Track? = selectAdjacentTrack(QueuePolicy::nextSequential)

    fun selectPreviousTrack(): Track? = selectAdjacentTrack(QueuePolicy::previousSequential)

    fun removeQueueItem(index: Int): QueueRemovalResult? {
        var result: QueueRemovalResult? = null
        _state.update { state ->
            val queue = state.playback.queue
            if (index !in queue.items.indices) return@update state

            val removedTrack = queue.items[index]
            val removedCurrentTrack = index == queue.currentIndex
            val updatedQueue = QueuePolicy.afterRemoval(queue, index)
            val replacementTrack = updatedQueue.currentItem
            result = QueueRemovalResult(
                removedTrack = removedTrack,
                replacementTrack = replacementTrack,
                removedCurrentTrack = removedCurrentTrack,
                shouldRestartPlayback = removedCurrentTrack && state.playback.isPlaying && replacementTrack != null
            )

            state.copy(
                playback = if (removedCurrentTrack) {
                    state.playback.copy(
                        queue = updatedQueue,
                        status = if (replacementTrack == null) PlaybackStatus.IDLE else PlaybackStatus.READY,
                        isPlaying = false,
                        positionMs = 0L,
                        durationMs = replacementTrack?.duration?.toLong() ?: 0L,
                        errorMessage = null
                    )
                } else {
                    state.playback.copy(queue = updatedQueue)
                }
            )
        }
        return result
    }

    private fun selectAdjacentTrack(nextIndex: (QueueState<Track>) -> Int?): Track? {
        var selectedTrack: Track? = null
        _state.update { state ->
            val queue = state.playback.queue
            val targetIndex = nextIndex(queue) ?: return@update state
            selectedTrack = queue.items[targetIndex]
            state.copy(
                playback = state.playback.copy(
                    queue = queue.copy(currentIndex = targetIndex),
                    status = PlaybackStatus.READY,
                    isPlaying = true,
                    positionMs = 0L,
                    durationMs = selectedTrack?.duration?.toLong() ?: 0L,
                    errorMessage = null
                )
            )
        }
        return selectedTrack
    }

    /**
     * Receives facts from a platform media engine. Queue selection remains owned by this store,
     * while decoding, output and timing remain platform-specific.
     */
    fun updatePlayback(
        status: PlaybackStatus,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        errorMessage: String? = null
    ) {
        _state.update { state ->
            if (state.playback.currentTrack == null) return@update state

            state.copy(
                playback = state.playback.copy(
                    status = status,
                    isPlaying = isPlaying,
                    positionMs = positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.takeIf { it > 0L } ?: state.playback.durationMs,
                    errorMessage = errorMessage
                )
            )
        }
    }
}
