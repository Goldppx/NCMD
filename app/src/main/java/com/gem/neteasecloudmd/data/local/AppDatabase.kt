package com.gem.neteasecloudmd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gem.neteasecloudmd.data.local.dao.CurrentPlaylistDao
import com.gem.neteasecloudmd.data.local.dao.RecentPlayDao
import com.gem.neteasecloudmd.data.local.entity.CurrentPlaylistEntity
import com.gem.neteasecloudmd.data.local.entity.RecentPlayEntity

@Database(
    entities = [RecentPlayEntity::class, CurrentPlaylistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentPlayDao(): RecentPlayDao
    abstract fun currentPlaylistDao(): CurrentPlaylistDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netease_cloud_music_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
