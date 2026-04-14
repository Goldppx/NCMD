package com.gem.neteasecloudmd.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gem.neteasecloudmd.data.local.entity.CurrentPlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentPlaylistDao {
    @Query("SELECT * FROM current_playlist ORDER BY position ASC")
    fun getCurrentPlaylist(): Flow<List<CurrentPlaylistEntity>>
    
    @Query("SELECT position FROM current_playlist LIMIT 1")
    suspend fun getCurrentPosition(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(tracks: List<CurrentPlaylistEntity>)
    
    @Query("UPDATE current_playlist SET position = :position WHERE position = (SELECT position FROM current_playlist ORDER BY position ASC LIMIT 1)")
    suspend fun updatePosition(position: Int)
    
    @Query("DELETE FROM current_playlist")
    suspend fun clearPlaylist()
    
    @Query("SELECT COUNT(*) FROM current_playlist")
    suspend fun getCount(): Int
}
