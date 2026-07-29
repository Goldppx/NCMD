package com.gem.neteasecloudmd.api

import com.gem.neteasecloudmd.utils.Logger
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player


import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.core.playback.PlaybackRequestPolicy
import com.gem.neteasecloudmd.core.playback.PlaybackController
import com.gem.neteasecloudmd.core.playback.PlaybackState
import com.gem.neteasecloudmd.core.playback.PlaybackStatus
import com.gem.neteasecloudmd.core.playback.PrefetchedUrl
import com.gem.neteasecloudmd.core.playback.QueuePolicy
import com.gem.neteasecloudmd.core.playback.QueueState
import com.gem.neteasecloudmd.data.local.AppDatabase
import com.gem.neteasecloudmd.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val waitForQueueEnd: Boolean = false,
    val targetAtMs: Long = 0L
)

private fun Long.toIntSafe(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

@UnstableApi
@Suppress("StaticFieldLeak")
class PlayerManager private constructor(private val context: Context) : PlaybackController {
    var isPlaying by mutableStateOf(false)
        private set
    var currentPlaylist by mutableStateOf<List<TrackItem>>(emptyList())
        private set
    var currentTrackIndex by mutableIntStateOf(0)
        private set
    var currentUrl by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var currentLyric by mutableStateOf<String?>(null)
        private set

    var currentPosition by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set

    var themeSeedArgb by mutableIntStateOf(0)
        private set

    var playMode by mutableStateOf(PlayMode.SEQUENTIAL)
        private set

    var isPlaybackBarHidden by mutableStateOf(false)
        private set

    var volumeNormalizationEnabled by mutableStateOf(false)
        private set

    var likedSongIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun updateLikedSongIds(ids: Set<Long>) {
        likedSongIds = ids
    }

    private var queueRevision = 0L
    private var playbackRequestId = 0L
    private var prefetchedUrl: PrefetchedUrl? = null
    private var urlLoadJob: Job? = null
    private var lyricLoadJob: Job? = null
    private var prefetchJob: Job? = null

    var sleepTimerState by mutableStateOf(SleepTimerState())
        private set

    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerWaitForQueueEnd: Boolean = false
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationPlayer: Player? = null
    private var currentCookie: String = ""
    private var currentApiService: NeteaseApiService? = null
    private var isPersonalFmMode: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition.toIntSafe()
                    duration = player.duration.toIntSafe()
                    publishPlaybackState()
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }
    
    private val musicRepository: MusicRepository by lazy {
        val database = AppDatabase.getInstance(context)
        MusicRepository(database.recentPlayDao(), database.currentPlaylistDao())
    }
    
    val currentTrack: TrackItem?
        get() = currentPlaylist.getOrNull(currentTrackIndex)

    private fun publishPlaybackState() {
        _state.value = PlaybackState(
            queue = QueueState(currentPlaylist, currentTrackIndex, playMode),
            status = when {
                errorMessage != null -> PlaybackStatus.ERROR
                isLoading -> PlaybackStatus.LOADING
                currentPlaylist.isEmpty() -> PlaybackStatus.IDLE
                else -> PlaybackStatus.READY
            },
            isPlaying = isPlaying,
            positionMs = currentPosition.toLong(),
            durationMs = duration.toLong(),
            currentUrl = currentUrl,
            lyric = currentLyric,
            errorMessage = errorMessage
        )
    }
    
    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val newPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    mainHandler.post {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                this@PlayerManager.isLoading = true
                            }
                            Player.STATE_READY -> {
                                this@PlayerManager.isLoading = false
                                this@PlayerManager.isPlaying = this@PlayerManager.exoPlayer?.isPlaying == true
                                this@PlayerManager.duration = this@PlayerManager.exoPlayer?.duration?.toIntSafe() ?: 0
                                Logger.d("Player", "Player READY, duration: ${this@PlayerManager.duration}")
                            }
                            Player.STATE_ENDED -> {
                                Logger.d("Player", "Playback ENDED")
                                this@PlayerManager.isPlaying = false
                                val sleepStop = if (sleepTimerWaitForQueueEnd) {
                                    val isLastSequential =
                                        playMode == PlayMode.SEQUENTIAL && currentTrackIndex >= currentPlaylist.lastIndex
                                    val noMoreTrack = currentPlaylist.isEmpty() || isLastSequential
                                    if (noMoreTrack) {
                                        Logger.i("Player", "Sleep timer stopping at queue end")
                                        pauseBySleepTimer()
                                    }
                                    noMoreTrack
                                } else {
                                    false
                                }
                                if (!sleepStop) {
                                    this@PlayerManager.next()
                                }
                            }
                            Player.STATE_IDLE -> {
                                this@PlayerManager.isLoading = false
                            }
                        }
                        this@PlayerManager.publishPlaybackState()
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    mainHandler.post {
                        this@PlayerManager.isPlaying = playing
                        Logger.i("Player", "isPlaying: $playing")
                        this@PlayerManager.publishPlaybackState()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    mainHandler.post {
                        Logger.e("Player", "ExoPlayer error", error)
                        this@PlayerManager.errorMessage = context.getString(
                            R.string.player_error_playback,
                            error.message ?: ""
                        )
                        this@PlayerManager.isPlaying = false
                        this@PlayerManager.isLoading = false
                        this@PlayerManager.publishPlaybackState()
                    }
                }
            })
        }
        exoPlayer = newPlayer
        setupMediaSession(newPlayer)
        mainHandler.post(updateRunnable)
        return newPlayer
    }

    private fun setupMediaSession(player: ExoPlayer) {
        if (notificationPlayer == null) {
            notificationPlayer = object : ForwardingPlayer(player) {
                private fun canSkipNext(): Boolean {
                    if (currentPlaylist.isEmpty()) return false
                    if (isPersonalFmMode) return true
                    return when (playMode) {
                        PlayMode.REPEAT_ONE -> true
                        PlayMode.SHUFFLE -> currentPlaylist.size > 1
                        PlayMode.SEQUENTIAL -> currentTrackIndex < currentPlaylist.lastIndex
                    }
                }

                private fun canSkipPrevious(): Boolean {
                    if (currentPlaylist.isEmpty()) return false
                    return when (playMode) {
                        PlayMode.REPEAT_ONE -> true
                        PlayMode.SHUFFLE -> currentPlaylist.size > 1
                        PlayMode.SEQUENTIAL -> currentTrackIndex > 0
                    }
                }

                override fun getAvailableCommands(): Player.Commands {
                    val base = super.getAvailableCommands().buildUpon()
                    if (canSkipNext()) {
                        base.add(Player.COMMAND_SEEK_TO_NEXT)
                        base.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    }
                    if (canSkipPrevious()) {
                        base.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        base.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    }
                    return base.build()
                }

                override fun seekToNext() {
                    this@PlayerManager.next()
                }

                override fun seekToPrevious() {
                    this@PlayerManager.previous()
                }

                override fun seekToNextMediaItem() {
                    this@PlayerManager.next()
                }

                override fun seekToPreviousMediaItem() {
                    this@PlayerManager.previous()
                }
            }
        }

        mediaSession = MediaSession.Builder(context, notificationPlayer ?: player).build()
    }

    internal fun mediaSessionForService(): MediaSession = mediaSession ?: run {
        getOrCreatePlayer()
        checkNotNull(mediaSession)
    }

    internal fun releaseServiceResources() {
        releasePlayer()
    }
    
    fun setApiService(service: NeteaseApiService) {
        currentApiService = service
    }
    
    fun setCookie(cookie: String) {
        currentCookie = cookie
    }

    fun setThemeSeedColor(argb: Int) {
        themeSeedArgb = argb
    }

    fun setSleepTimer(minutes: Int, waitForQueueEnd: Boolean) {
        clearSleepTimer()
        if (minutes <= 0) return

        val totalMs = minutes * 60_000L
        val targetAt = System.currentTimeMillis() + totalMs
        sleepTimerWaitForQueueEnd = waitForQueueEnd
        sleepTimerState = SleepTimerState(
            isActive = true,
            remainingMs = totalMs,
            waitForQueueEnd = waitForQueueEnd,
            targetAtMs = targetAt
        )

        val runnable = object : Runnable {
            override fun run() {
                val remain = (targetAt - System.currentTimeMillis()).coerceAtLeast(0L)
                sleepTimerState = sleepTimerState.copy(remainingMs = remain)
                if (remain <= 0L) {
                    if (sleepTimerWaitForQueueEnd) {
                        if (currentPlaylist.isEmpty()) {
                            pauseBySleepTimer()
                        }
                    } else {
                        pauseBySleepTimer()
                    }
                    return
                }
                mainHandler.postDelayed(this, 1000L)
            }
        }
        sleepTimerRunnable = runnable
        mainHandler.post(runnable)
    }

    fun clearSleepTimer() {
        sleepTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        sleepTimerRunnable = null
        sleepTimerWaitForQueueEnd = false
        sleepTimerState = SleepTimerState()
    }

    private fun pauseBySleepTimer() {
        pause()
        clearSleepTimer()
    }
    
    fun setPlaylist(tracks: List<TrackItem>, startIndex: Int = 0) {
        Logger.i("Player", "Set playlist: ${tracks.size} tracks, startIndex: $startIndex")
        isPersonalFmMode = false
        updateQueueRevision()
        currentPlaylist = tracks
        currentTrackIndex = startIndex.coerceIn(0, maxOf(0, tracks.size - 1))
        isPlaying = true
        currentPosition = 0
        duration = 0
        
        managerScope.launch(Dispatchers.IO) {
            runCatching {
                musicRepository.saveCurrentPlaylist(tracks, currentTrackIndex)
            }.onFailure { Logger.e("Player", "Failed to save playlist: ${it.message}") }
        }
        
        loadAndPlayCurrentTrack()
    }

    override fun replaceQueue(tracks: List<TrackItem>, startIndex: Int) {
        setPlaylist(tracks, startIndex)
    }

    fun setPersonalFmPlaylist(tracks: List<TrackItem>, startIndex: Int = 0) {
        Logger.i("Player", "Set Personal FM playlist")
        isPersonalFmMode = true
        updateQueueRevision()
        currentPlaylist = tracks
        currentTrackIndex = startIndex.coerceIn(0, maxOf(0, tracks.size - 1))
        isPlaying = true
        currentPosition = 0
        duration = 0
        loadAndPlayCurrentTrack()
    }
    
    private fun updateQueueRevision() {
        queueRevision += 1
        prefetchedUrl = null
        prefetchJob?.cancel()
        prefetchJob = null
    }

    private fun startPlaybackRequest(): Long {
        playbackRequestId += 1
        urlLoadJob?.cancel()
        lyricLoadJob?.cancel()
        return playbackRequestId
    }

    private fun isCurrentPlaybackRequest(requestId: Long, trackId: Long): Boolean {
        return requestId == playbackRequestId && currentTrack?.id == trackId
    }

    private fun loadAndPlayCurrentTrack() {
        val track = currentTrack ?: return
        val apiService = currentApiService ?: return
        
        if (currentCookie.isEmpty()) {
            Logger.e("Player", "Cookie is empty!")
            errorMessage = context.getString(R.string.player_error_not_logged_in)
            return
        }

        context.startService(PlaybackService.intent(context))
        
        isLoading = true
        errorMessage = null
        currentUrl = null
        currentPosition = 0
        duration = 0
        currentLyric = null
        val requestId = startPlaybackRequest()
        publishPlaybackState()
        
        managerScope.launch(Dispatchers.IO) {
            try {
                musicRepository.addRecentPlay(track)
            } catch (e: Exception) {
                Logger.e("Player", "Failed to save recent play: ${e.message}")
            }
        }

        val cachedUrl = prefetchedUrl
        if (PlaybackRequestPolicy.canUsePrefetchedUrl(cachedUrl, queueRevision, track.id)) {
            prefetchedUrl = null
            val url = requireNotNull(cachedUrl).url
            currentUrl = url
            playFromUrl(url, track, requestId)
        } else {
            urlLoadJob = managerScope.launch {
                try {
                    val urlResult = withContext(Dispatchers.IO) {
                        apiService.getSongUrl(track.id, currentCookie)
                    }
                    urlResult.fold(
                        onSuccess = { url ->
                            if (!isCurrentPlaybackRequest(requestId, track.id)) return@fold
                            Logger.d("Player", "Got song URL: ${url.take(100)}...")
                            currentUrl = url
                            publishPlaybackState()
                            playFromUrl(url, track, requestId)
                        },
                        onFailure = { e ->
                            if (!isCurrentPlaybackRequest(requestId, track.id)) return@fold
                            Logger.e("Player", "Failed to get song URL: ${e.message}")
                            errorMessage = context.getString(
                                R.string.player_error_url_failed,
                                e.message ?: ""
                            )
                            isLoading = false
                            publishPlaybackState()
                        }
                    )
                } catch (e: Exception) {
                    if (!isCurrentPlaybackRequest(requestId, track.id)) return@launch
                    Logger.e("Player", "Exception: ${e.message}")
                    errorMessage = e.message
                    isLoading = false
                    publishPlaybackState()
                }
            }
        }

        fetchLyric(track.id, requestId)
    }

    private fun fetchLyric(id: Long, requestId: Long) {
        val apiService = currentApiService ?: return
        val cookie = currentCookie
        if (cookie.isEmpty()) return

        lyricLoadJob = managerScope.launch {
            val result = withContext(Dispatchers.IO) {
                apiService.getLyric(id, cookie)
            }
            result.fold(
                onSuccess = { response ->
                    if (!isCurrentPlaybackRequest(requestId, id)) return@fold
                    currentLyric = response.lrc?.lyric
                    publishPlaybackState()
                },
                onFailure = { e ->
                    if (!isCurrentPlaybackRequest(requestId, id)) return@fold
                    Logger.e("Player", "Failed to fetch lyric: ${e.message}")
                    currentLyric = null
                    publishPlaybackState()
                }
            )
        }
    }
    
    private fun playFromUrl(url: String, track: TrackItem, requestId: Long) {
        mainHandler.post {
            if (!isCurrentPlaybackRequest(requestId, track.id)) return@post
            try {
                val player = getOrCreatePlayer()
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(track.name)
                    .setArtist(track.artists)
                    .setAlbumTitle(track.albumName)

                val albumPicUrl = track.albumPicUrl
                if (!albumPicUrl.isNullOrBlank()) {
                    metadataBuilder.setArtworkUri(albumPicUrl.toUri())
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id.toString())
                    .setUri(url)
                    .setMediaMetadata(metadataBuilder.build())
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                applyVolumeNormalization()
                prefetchNextUrl(track, requestId)
                publishPlaybackState()
            } catch (e: Exception) {
                Logger.e("Player", "Exception playing: ${e.message}")
                errorMessage = e.message
                isLoading = false
                publishPlaybackState()
            }
        }
    }

    private fun prefetchNextUrl(currentTrack: TrackItem, requestId: Long) {
        val nextIdx = currentTrackIndex + 1
        if (nextIdx >= currentPlaylist.size) return
        val nextTrack = currentPlaylist[nextIdx]
        val apiService = currentApiService ?: return
        val cookie = currentCookie
        val currentQueueRevision = queueRevision
        if (cookie.isEmpty()) return

        prefetchJob = managerScope.launch {
            val urlResult = withContext(Dispatchers.IO) {
                apiService.getSongUrl(nextTrack.id, cookie)
            }
            urlResult.onSuccess { url ->
                if (
                    isCurrentPlaybackRequest(requestId, currentTrack.id) &&
                    queueRevision == currentQueueRevision
                ) {
                    prefetchedUrl = PrefetchedUrl(currentQueueRevision, nextTrack.id, url)
                }
            }
        }
    }
    
    override fun play() {
        if (currentPlaylist.isEmpty()) return
        exoPlayer?.let {
            if (!it.isPlaying) {
                it.play()
                isPlaying = true
                publishPlaybackState()
            }
        } ?: loadAndPlayCurrentTrack()
    }
    
    override fun pause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                publishPlaybackState()
            }
        }
    }
    
    fun togglePlayPause() {
        if (currentPlaylist.isEmpty()) return
        if (isLoading) return
        
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }
    
    fun next() {
        if (currentPlaylist.isEmpty()) return
        Logger.d("Player", "Action: Next")

        if (playMode == PlayMode.REPEAT_ONE) {
            currentPosition = 0
            duration = 0
            loadAndPlayCurrentTrack()
            return
        }

        if (playMode == PlayMode.SHUFFLE && currentPlaylist.size > 1) {
            val oldIndex = currentTrackIndex
            var newIndex = oldIndex
            repeat(5) {
                newIndex = (currentPlaylist.indices).random()
                if (newIndex != oldIndex) return@repeat
            }
            if (newIndex == oldIndex) {
                newIndex = (oldIndex + 1) % currentPlaylist.size
            }

            currentTrackIndex = newIndex
            currentPosition = 0
            duration = 0
            managerScope.launch(Dispatchers.IO) {
            runCatching {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
            }
            loadAndPlayCurrentTrack()
            return
        }

        if (currentTrackIndex < currentPlaylist.size - 1) {
            currentTrackIndex++
            currentPosition = 0
            duration = 0

            managerScope.launch(Dispatchers.IO) {
            runCatching {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
            }

            loadAndPlayCurrentTrack()
        } else {
            if (isPersonalFmMode) {
                fetchMorePersonalFmAndPlay()
            } else {
                isPlaying = false
                publishPlaybackState()
            }
        }
    }

    override fun skipToNext() {
        next()
    }

    fun seekToTrack(index: Int) {
        if (index !in currentPlaylist.indices) return
        currentTrackIndex = index
        currentPosition = 0
        duration = 0
        
        managerScope.launch(Dispatchers.IO) {
            runCatching {
            musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
            currentTrack?.let { track ->
                musicRepository.addRecentPlay(track)
            }
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
        }
        
        loadAndPlayCurrentTrack()
    }

    override fun seekToQueueItem(index: Int) {
        seekToTrack(index)
    }

    private fun fetchMorePersonalFmAndPlay() {
        val apiService = currentApiService ?: run {
            isPlaying = false
            return
        }
        if (currentCookie.isBlank()) {
            isPlaying = false
            return
        }

        isLoading = true
        managerScope.launch {
            val result = withContext(Dispatchers.IO) {
                apiService.getPersonalFm(currentCookie, 6)
            }
            result.fold(
                onSuccess = { newTracks ->
                    if (newTracks.isNotEmpty()) {
                        updateQueueRevision()
                        val dedupedNewTracks = newTracks.filter { newTrack ->
                            currentPlaylist.none { it.id == newTrack.id }
                        }
                        if (dedupedNewTracks.isNotEmpty()) {
                            currentPlaylist = currentPlaylist + dedupedNewTracks
                            currentTrackIndex = (currentTrackIndex + 1).coerceAtMost(currentPlaylist.lastIndex)
                            loadAndPlayCurrentTrack()
                        } else {
                            currentPlaylist = currentPlaylist + newTracks
                            currentTrackIndex = (currentTrackIndex + 1).coerceAtMost(currentPlaylist.lastIndex)
                            loadAndPlayCurrentTrack()
                        }
                    } else {
                        isLoading = false
                        isPlaying = false
                    }
                },
                onFailure = {
                    isLoading = false
                    isPlaying = false
                }
            )
        }
    }
    
    fun previous() {
        if (currentPlaylist.isEmpty()) return
        Logger.d("Player", "Action: Previous")

        if (playMode == PlayMode.REPEAT_ONE) {
            currentPosition = 0
            duration = 0
            loadAndPlayCurrentTrack()
            return
        }

        if (playMode == PlayMode.SHUFFLE && currentPlaylist.size > 1) {
            val oldIndex = currentTrackIndex
            var newIndex = oldIndex
            repeat(5) {
                newIndex = (currentPlaylist.indices).random()
                if (newIndex != oldIndex) return@repeat
            }
            if (newIndex == oldIndex) {
                newIndex = (oldIndex - 1).coerceAtLeast(0)
            }

            currentTrackIndex = newIndex
            currentPosition = 0
            duration = 0
            managerScope.launch(Dispatchers.IO) {
            runCatching {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
            }
            loadAndPlayCurrentTrack()
            return
        }

        if (currentTrackIndex > 0) {
            currentTrackIndex--
            currentPosition = 0
            duration = 0
            
            managerScope.launch(Dispatchers.IO) {
            runCatching {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
            }
            
            loadAndPlayCurrentTrack()
        }
    }

    override fun skipToPrevious() {
        previous()
    }
    
    fun seekTo(position: Int) {
        mainHandler.post {
            exoPlayer?.seekTo(position.toLong())
            currentPosition = position
            publishPlaybackState()
        }
    }

    override fun seekTo(positionMs: Long) {
        seekTo(positionMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
    }

    override fun updatePlayMode(mode: PlayMode) {
        Logger.i("Player", "Mode changed to: $mode")
        playMode = mode
        publishPlaybackState()
    }

    fun updatePlaybackBarHidden(hidden: Boolean) {
        isPlaybackBarHidden = hidden
    }

    fun updateVolumeNormalization(enabled: Boolean) {
        volumeNormalizationEnabled = enabled
        applyVolumeNormalization()
    }

    private fun applyVolumeNormalization() {
        exoPlayer?.volume = if (volumeNormalizationEnabled) 0.75f else 1.0f
    }

    fun clearPlaylist() {
        Logger.i("Player", "Clearing playlist")
        updateQueueRevision()
        startPlaybackRequest()
        currentPlaylist = emptyList()
        currentTrackIndex = 0
        currentUrl = null
        isPlaying = false
        isLoading = false
        currentPosition = 0
        duration = 0
        publishPlaybackState()
        mainHandler.post {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        }
        managerScope.launch(Dispatchers.IO) {
            runCatching {
            musicRepository.clearCurrentPlaylist()
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
        }
    }

    override fun appendToQueue(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        Logger.i("Player", "Append ${tracks.size} tracks to queue")
        updateQueueRevision()

        if (currentPlaylist.isEmpty()) {
            currentPlaylist = tracks
            currentTrackIndex = 0
            isPlaying = false
            currentPosition = 0
            duration = 0
        } else {
            currentPlaylist = currentPlaylist + tracks
            currentTrack?.let { track -> prefetchNextUrl(track, playbackRequestId) }
        }
        publishPlaybackState()

        managerScope.launch(Dispatchers.IO) {
            runCatching {
            musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
        }
    }

    fun removeTrackAt(index: Int) {
        if (index !in currentPlaylist.indices) return
        Logger.i("Player", "Remove track at index: $index")
        updateQueueRevision()

        val removingCurrent = index == currentTrackIndex
        val updatedQueue = QueuePolicy.afterRemoval(
            QueueState(currentPlaylist, currentTrackIndex, playMode),
            index
        )

        if (updatedQueue.items.isEmpty()) {
            clearPlaylist()
            return
        }

        currentPlaylist = updatedQueue.items
        currentTrackIndex = updatedQueue.currentIndex
        publishPlaybackState()

        if (removingCurrent) {
            currentPosition = 0
            duration = 0
            loadAndPlayCurrentTrack()
        } else {
            currentTrack?.let { track -> prefetchNextUrl(track, playbackRequestId) }
        }

        managerScope.launch(Dispatchers.IO) {
            runCatching {
            musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
            }.onFailure { Logger.e("Player", "Database error: ${it.message}") }
        }
    }

    override fun removeQueueItem(index: Int) {
        removeTrackAt(index)
    }
    
    private fun releasePlayer() {
        mainHandler.removeCallbacks(updateRunnable)
        clearSleepTimer()
        notificationPlayer = null
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.apply {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                Logger.w("Player", "Error releasing player: ${e.message}")
            }
        }
        exoPlayer = null
    }
    
    override fun release() {
        startPlaybackRequest()
        releasePlayer()
        managerScope.cancel()
        currentPlaylist = emptyList()
        currentTrackIndex = 0
        isPlaying = false
        currentUrl = null
        currentPosition = 0
        duration = 0
        publishPlaybackState()
    }
    
    suspend fun getRecentPlays(): List<TrackItem> {
        return try {
            musicRepository.getRecentPlays(500).first().map { entity ->
                TrackItem(
                    id = entity.id,
                    name = entity.name,
                    artists = entity.artists,
                    albumName = "",
                    albumPicUrl = entity.albumPicUrl,
                    duration = entity.duration
                )
            }
        } catch (e: Exception) {
            Logger.e("Player", "Failed to get recent plays: ${e.message}")
            emptyList()
        }
    }

    suspend fun removeRecentPlay(id: Long) {
        runCatching {
            musicRepository.removeRecentPlay(id)
        }
    }
    
    suspend fun restoreLastPlaylist(): Boolean {
        return try {
            val savedPlaylist = musicRepository.getCurrentPlaylist().first()
            if (savedPlaylist.isNotEmpty()) {
                val tracks = savedPlaylist.map { entity ->
                    TrackItem(
                        id = entity.trackId,
                        name = entity.name,
                        artists = entity.artists,
                        albumName = "",
                        albumPicUrl = entity.albumPicUrl,
                        duration = entity.duration
                    )
                }
                val savedPosition = musicRepository.getCurrentPosition()
                withContext(Dispatchers.Main.immediate) {
                    updateQueueRevision()
                    currentPlaylist = tracks
                    currentTrackIndex = savedPosition.coerceIn(0, tracks.lastIndex)
                    publishPlaybackState()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e("Player", "Failed to restore playlist: ${e.message}")
            false
        }
    }
    
    companion object {
        @Volatile
        private var instance: PlayerManager? = null
        
        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

@OptIn(UnstableApi::class) // Media3/ExoPlayer API 被标记为 unstable
@Composable
fun rememberPlayerManager(context: Context): PlayerManager {
    val manager = remember { PlayerManager.getInstance(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            // Don't release on dispose, keep singleton alive
        }
    }
    
    return manager
}
