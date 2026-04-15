package com.gem.neteasecloudmd.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gem.neteasecloudmd.data.local.entity.RecentPlayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPlayDao {
    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentPlays(limit: Int = 500): Flow<List<RecentPlayEntity>>
    
    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT 10")
    fun getRecentPlaysPreview(): Flow<List<RecentPlayEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(recentPlay: RecentPlayEntity)
    
    @Query("DELETE FROM recent_plays WHERE id = :id")
    suspend fun deleteRecentPlay(id: Long)
    
    @Query("DELETE FROM recent_plays WHERE playedAt < :timestamp")
    suspend fun deleteOldPlays(timestamp: Long)

    @Query("DELETE FROM recent_plays WHERE id NOT IN (SELECT id FROM recent_plays ORDER BY playedAt DESC LIMIT :limit)")
    suspend fun trimToLatest(limit: Int)
    
    @Query("SELECT COUNT(*) FROM recent_plays")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM recent_plays")
    suspend fun clearAll()
}
