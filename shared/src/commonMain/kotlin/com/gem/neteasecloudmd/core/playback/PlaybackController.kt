package com.gem.neteasecloudmd.core.playback

import com.gem.neteasecloudmd.core.model.Track
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class PlaybackState(
    val queue: QueueState<Track> = QueueState(),
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentUrl: String? = null,
    val lyric: String? = null,
    val errorMessage: String? = null
) {
    val currentTrack: Track?
        get() = queue.currentItem
}

/**
 * Contract implemented by each platform's media layer.
 *
 * Android will delegate to Media3, iOS to AVFoundation, and Desktop to its native
 * playback engine. Platform UI and shared presentation logic only depend on this contract.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun replaceQueue(tracks: List<Track>, startIndex: Int = 0)
    fun appendToQueue(tracks: List<Track>)
    fun removeQueueItem(index: Int)
    fun seekToQueueItem(index: Int)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun updatePlayMode(mode: PlayMode)
    fun release()
}
