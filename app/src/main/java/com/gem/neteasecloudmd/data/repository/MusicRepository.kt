package com.gem.neteasecloudmd.data.repository

import com.gem.neteasecloudmd.data.local.dao.CurrentPlaylistDao
import com.gem.neteasecloudmd.data.local.dao.RecentPlayDao
import com.gem.neteasecloudmd.data.local.entity.CurrentPlaylistEntity
import com.gem.neteasecloudmd.data.local.entity.RecentPlayEntity
import com.gem.neteasecloudmd.api.TrackItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(
    private val recentPlayDao: RecentPlayDao,
    private val currentPlaylistDao: CurrentPlaylistDao
) {
    companion object {
        private const val MAX_RECENT_PLAYS = 500
    }
    
    fun getRecentPlays(limit: Int = 500): Flow<List<RecentPlayEntity>> {
        return recentPlayDao.getRecentPlays(limit)
    }
    
    fun getRecentPlaysPreview(): Flow<List<RecentPlayEntity>> {
        return recentPlayDao.getRecentPlaysPreview()
    }
    
    suspend fun addRecentPlay(track: TrackItem) {
        val entity = RecentPlayEntity(
            id = track.id,
            name = track.name,
            artists = track.artists,
            albumPicUrl = track.albumPicUrl,
            duration = track.duration,
            playedAt = System.currentTimeMillis()
        )
        
        recentPlayDao.insertRecentPlay(entity)
        
        val count = recentPlayDao.getCount()
        if (count > MAX_RECENT_PLAYS) {
            val excess = count - MAX_RECENT_PLAYS
            deleteOldestPlays(excess)
        }
    }
    
    private suspend fun deleteOldestPlays(count: Int) {
        val allPlays = recentPlayDao.getRecentPlays(MAX_RECENT_PLAYS)
        allPlays.collect { plays ->
            if (plays.size > MAX_RECENT_PLAYS) {
                val toDelete = plays.drop(MAX_RECENT_PLAYS)
                toDelete.forEach { play ->
                    recentPlayDao.deleteRecentPlay(play.id)
                }
            }
            return@collect
        }
    }
    
    suspend fun clearRecentPlays() {
        recentPlayDao.clearAll()
    }
    
    fun getCurrentPlaylist(): Flow<List<CurrentPlaylistEntity>> {
        return currentPlaylistDao.getCurrentPlaylist()
    }
    
    suspend fun saveCurrentPlaylist(tracks: List<TrackItem>, currentIndex: Int) {
        currentPlaylistDao.clearPlaylist()
        val entities = tracks.mapIndexed { index, track ->
            CurrentPlaylistEntity(
                id = track.id,
                name = track.name,
                artists = track.artists,
                albumPicUrl = track.albumPicUrl,
                duration = track.duration,
                position = index
            )
        }
        currentPlaylistDao.insertPlaylist(entities)
        currentPlaylistDao.updatePosition(currentIndex)
    }
    
    suspend fun updateCurrentPosition(position: Int) {
        currentPlaylistDao.updatePosition(position)
    }
    
    suspend fun clearCurrentPlaylist() {
        currentPlaylistDao.clearPlaylist()
    }
}
