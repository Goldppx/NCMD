package com.gem.neteasecloudmd.api

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.data.local.AppDatabase
import com.gem.neteasecloudmd.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TrackItem(
    val id: Long,
    val name: String,
    val artists: String,
    val albumName: String,
    val albumPicUrl: String?,
    val duration: Int = 0
)

enum class PlayMode {
    SEQUENTIAL,
    SHUFFLE,
    REPEAT_ONE
}

@UnstableApi
class PlayerManager private constructor(private val context: Context) {
    var isPlaying by mutableStateOf(false)
        private set
    var currentPlaylist by mutableStateOf<List<TrackItem>>(emptyList())
        private set
    var currentTrackIndex by mutableStateOf(0)
        private set
    var currentUrl by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var currentPosition by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set

    var themeSeedArgb by mutableIntStateOf(0)
        private set

    var playMode by mutableStateOf(PlayMode.SEQUENTIAL)
        private set

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var notificationPlayer: Player? = null
    private var currentCookie: String = ""
    private var currentApiService: NeteaseApiService? = null
    private var isPersonalFmMode: Boolean = false
    private var configuredAudioBufferMs: Int = SessionManager(context).getAudioBufferMs()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition.toInt()
                    duration = player.duration.toInt().coerceAtLeast(0)
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
    
    private fun getOrCreatePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val audioBufferMs = configuredAudioBufferMs
            val networkBufferMs = (audioBufferMs * 60).coerceIn(8000, 70000)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    networkBufferMs,
                    networkBufferMs,
                    audioBufferMs,
                    audioBufferMs
                )
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()
                .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                this@PlayerManager.isLoading = true
                            }
                            Player.STATE_READY -> {
                                this@PlayerManager.isLoading = false
                                this@PlayerManager.isPlaying = this@PlayerManager.exoPlayer?.isPlaying == true
                                this@PlayerManager.duration = this@PlayerManager.exoPlayer?.duration?.toInt() ?: 0
                            }
                            Player.STATE_ENDED -> {
                                this@PlayerManager.isPlaying = false
                                this@PlayerManager.next()
                            }
                            Player.STATE_IDLE -> {
                                this@PlayerManager.isLoading = false
                            }
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@PlayerManager.isPlaying = playing
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerManager", "ExoPlayer error: ${error.message}")
                        this@PlayerManager.errorMessage = "播放错误: ${error.message}"
                        this@PlayerManager.isPlaying = false
                        this@PlayerManager.isLoading = false
                    }
                })
            }
            setupMediaNotification(exoPlayer!!)
            mainHandler.post(updateRunnable)
        }
        return exoPlayer!!
    }

    fun setAudioBufferMs(bufferMs: Int) {
        val clamped = bufferMs.coerceIn(
            SessionManager.AUDIO_BUFFER_MIN_MS,
            SessionManager.AUDIO_BUFFER_MAX_MS
        )
        if (configuredAudioBufferMs == clamped) return
        configuredAudioBufferMs = clamped

        val wasPlaying = isPlaying
        val savedPosition = currentPosition
        val savedTrackIndex = currentTrackIndex
        val savedPlaylist = currentPlaylist

        releasePlayer()

        if (savedPlaylist.isNotEmpty()) {
            currentPlaylist = savedPlaylist
            currentTrackIndex = savedTrackIndex.coerceIn(0, savedPlaylist.lastIndex)
            loadAndPlayCurrentTrack()
            if (!wasPlaying) {
                mainHandler.postDelayed({
                    seekTo(savedPosition)
                    pause()
                }, 450)
            } else {
                mainHandler.postDelayed({ seekTo(savedPosition) }, 350)
            }
        }
    }

    private fun setupMediaNotification(player: ExoPlayer) {
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

        if (notificationManager != null) {
            notificationManager?.setPlayer(notificationPlayer)
            return
        }

        mediaSession = MediaSession.Builder(context, notificationPlayer ?: player).build()

        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setSmallIconResourceId(R.drawable.ic_home)
            .setMediaDescriptionAdapter(
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        return currentTrack?.name ?: "NCMD"
                    }

                    override fun createCurrentContentIntent(player: Player) = null

                    override fun getCurrentContentText(player: Player): CharSequence {
                        return currentTrack?.artists ?: ""
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ) = null
                }
            )
            .build()
            .apply {
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseStopAction(false)
                setUsePreviousAction(true)
                setUseNextAction(true)
                setUseNextActionInCompactView(true)
                setUsePreviousActionInCompactView(true)
                setPlayer(notificationPlayer)
            }

        NotificationManagerCompat.from(context).areNotificationsEnabled()
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
    
    fun setPlaylist(tracks: List<TrackItem>, startIndex: Int = 0) {
        Log.d("PlayerManager", "setPlaylist: ${tracks.size} tracks, startIndex: $startIndex")
        isPersonalFmMode = false
        currentPlaylist = tracks
        currentTrackIndex = startIndex.coerceIn(0, maxOf(0, tracks.size - 1))
        isPlaying = true
        currentPosition = 0
        duration = 0
        
        CoroutineScope(Dispatchers.IO).launch {
            musicRepository.saveCurrentPlaylist(tracks, currentTrackIndex)
        }
        
        loadAndPlayCurrentTrack()
    }

    fun setPersonalFmPlaylist(tracks: List<TrackItem>, startIndex: Int = 0) {
        isPersonalFmMode = true
        currentPlaylist = tracks
        currentTrackIndex = startIndex.coerceIn(0, maxOf(0, tracks.size - 1))
        isPlaying = true
        currentPosition = 0
        duration = 0
        loadAndPlayCurrentTrack()
    }
    
    private fun loadAndPlayCurrentTrack() {
        val track = currentTrack ?: return
        val apiService = currentApiService ?: return
        
        if (currentCookie.isEmpty()) {
            Log.e("PlayerManager", "Cookie is empty!")
            errorMessage = "未登录或Cookie失效"
            return
        }
        
        isLoading = true
        errorMessage = null
        currentPosition = 0
        duration = 0
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                musicRepository.addRecentPlay(track)
            } catch (e: Exception) {
                Log.e("PlayerManager", "Failed to save recent play: ${e.message}")
            }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlResult = apiService.getSongUrl(track.id, currentCookie)
                urlResult.fold(
                    onSuccess = { url ->
                        Log.d("PlayerManager", "Got song URL: ${url.take(100)}...")
                        currentUrl = url
                        playFromUrl(url)
                    },
                    onFailure = { e ->
                        Log.e("PlayerManager", "Failed to get song URL: ${e.message}")
                        errorMessage = "获取播放链接失败: ${e.message}"
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                Log.e("PlayerManager", "Exception: ${e.message}")
                errorMessage = e.message
                isLoading = false
            }
        }
    }
    
    private fun playFromUrl(url: String) {
        mainHandler.post {
            try {
                val player = getOrCreatePlayer()
                val track = currentTrack
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(track?.name)
                    .setArtist(track?.artists)
                    .setAlbumTitle(track?.albumName)

                if (!track?.albumPicUrl.isNullOrBlank()) {
                    metadataBuilder.setArtworkUri(Uri.parse(track?.albumPicUrl))
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track?.id?.toString() ?: "")
                    .setUri(url)
                    .setMediaMetadata(metadataBuilder.build())
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            } catch (e: Exception) {
                Log.e("PlayerManager", "Exception playing: ${e.message}")
                errorMessage = e.message
                isLoading = false
            }
        }
    }
    
    fun play() {
        if (currentPlaylist.isEmpty()) return
        exoPlayer?.let {
            if (!it.isPlaying) {
                it.play()
                isPlaying = true
            }
        } ?: loadAndPlayCurrentTrack()
    }
    
    fun pause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
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
            CoroutineScope(Dispatchers.IO).launch {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }
            loadAndPlayCurrentTrack()
            return
        }

        if (currentTrackIndex < currentPlaylist.size - 1) {
            currentTrackIndex++
            currentPosition = 0
            duration = 0
            
            CoroutineScope(Dispatchers.IO).launch {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }
            
            loadAndPlayCurrentTrack()
        } else {
            if (isPersonalFmMode) {
                fetchMorePersonalFmAndPlay()
            } else {
                isPlaying = false
            }
        }
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
        CoroutineScope(Dispatchers.IO).launch {
            val result = apiService.getPersonalFm(currentCookie, 6)
            result.fold(
                onSuccess = { newTracks ->
                    if (newTracks.isNotEmpty()) {
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
            CoroutineScope(Dispatchers.IO).launch {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
            }
            loadAndPlayCurrentTrack()
            return
        }

        if (currentTrackIndex > 0) {
            currentTrackIndex--
            currentPosition = 0
            duration = 0
            
            CoroutineScope(Dispatchers.IO).launch {
                musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
                currentTrack?.let { track ->
                    musicRepository.addRecentPlay(track)
                }
            }
            
            loadAndPlayCurrentTrack()
        }
    }
    
    fun seekTo(position: Int) {
        mainHandler.post {
            exoPlayer?.seekTo(position.toLong())
            currentPosition = position
        }
    }

    fun updatePlayMode(mode: PlayMode) {
        playMode = mode
    }

    fun clearPlaylist() {
        currentPlaylist = emptyList()
        currentTrackIndex = 0
        currentUrl = null
        isPlaying = false
        isLoading = false
        currentPosition = 0
        duration = 0
        mainHandler.post {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        }
        CoroutineScope(Dispatchers.IO).launch {
            musicRepository.clearCurrentPlaylist()
        }
    }

    fun removeTrackAt(index: Int) {
        if (index !in currentPlaylist.indices) return

        val mutable = currentPlaylist.toMutableList()
        val removingCurrent = index == currentTrackIndex
        mutable.removeAt(index)

        if (mutable.isEmpty()) {
            clearPlaylist()
            return
        }

        currentPlaylist = mutable

        if (index < currentTrackIndex) {
            currentTrackIndex -= 1
        } else if (removingCurrent) {
            if (currentTrackIndex >= currentPlaylist.size) {
                currentTrackIndex = currentPlaylist.lastIndex
            }
            currentPosition = 0
            duration = 0
            loadAndPlayCurrentTrack()
        }

        CoroutineScope(Dispatchers.IO).launch {
            musicRepository.saveCurrentPlaylist(currentPlaylist, currentTrackIndex)
        }
    }
    
    private fun releasePlayer() {
        mainHandler.removeCallbacks(updateRunnable)
        notificationManager?.setPlayer(null)
        notificationManager = null
        notificationPlayer = null
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.apply {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        exoPlayer = null
    }
    
    fun release() {
        releasePlayer()
        currentPlaylist = emptyList()
        currentTrackIndex = 0
        isPlaying = false
        currentUrl = null
        currentPosition = 0
        duration = 0
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
            Log.e("PlayerManager", "Failed to get recent plays: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun restoreLastPlaylist(): Boolean {
        return try {
            val savedPlaylist = musicRepository.getCurrentPlaylist().first()
            if (savedPlaylist.isNotEmpty()) {
                val tracks = savedPlaylist.map { entity ->
                    TrackItem(
                        id = entity.id,
                        name = entity.name,
                        artists = entity.artists,
                        albumName = "",
                        albumPicUrl = entity.albumPicUrl,
                        duration = entity.duration
                    )
                }
                currentPlaylist = tracks
                currentTrackIndex = savedPlaylist.firstOrNull()?.position ?: 0
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Failed to restore playlist: ${e.message}")
            false
        }
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "ncmd_playback"
        private var instance: PlayerManager? = null
        
        fun getInstance(context: Context): PlayerManager {
            if (instance == null) {
                instance = PlayerManager(context.applicationContext)
            }
            return instance!!
        }
    }
}

@OptIn(UnstableApi::class)
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
