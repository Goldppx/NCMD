package com.gem.neteasecloudmd.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private var exoPlayer: ExoPlayer? = null
    private var currentCookie: String = ""
    private var currentApiService: NeteaseApiService? = null
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
            exoPlayer = ExoPlayer.Builder(context).build().apply {
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
            mainHandler.post(updateRunnable)
        }
        return exoPlayer!!
    }
    
    fun setApiService(service: NeteaseApiService) {
        currentApiService = service
    }
    
    fun setCookie(cookie: String) {
        currentCookie = cookie
    }
    
    fun setPlaylist(tracks: List<TrackItem>, startIndex: Int = 0) {
        Log.d("PlayerManager", "setPlaylist: ${tracks.size} tracks, startIndex: $startIndex")
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
                val mediaItem = MediaItem.fromUri(url)
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
            isPlaying = false
        }
    }
    
    fun previous() {
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
    
    private fun releasePlayer() {
        mainHandler.removeCallbacks(updateRunnable)
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
        private var instance: PlayerManager? = null
        
        fun getInstance(context: Context): PlayerManager {
            if (instance == null) {
                instance = PlayerManager(context.applicationContext)
            }
            return instance!!
        }
    }
}

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