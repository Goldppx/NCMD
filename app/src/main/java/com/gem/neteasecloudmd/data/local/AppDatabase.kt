package com.gem.neteasecloudmd.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gem.neteasecloudmd.data.local.dao.CurrentPlaylistDao
import com.gem.neteasecloudmd.data.local.dao.RecentPlayDao
import com.gem.neteasecloudmd.data.local.entity.CurrentPlaylistEntity
import com.gem.neteasecloudmd.data.local.entity.RecentPlayEntity

@Database(
    entities = [RecentPlayEntity::class, CurrentPlaylistEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentPlayDao(): RecentPlayDao
    abstract fun currentPlaylistDao(): CurrentPlaylistDao
    
    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS current_playlist_new (
                        position INTEGER NOT NULL,
                        trackId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        artists TEXT NOT NULL,
                        albumPicUrl TEXT,
                        duration INTEGER NOT NULL,
                        isCurrent INTEGER NOT NULL,
                        PRIMARY KEY(position)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO current_playlist_new (
                        position, trackId, name, artists, albumPicUrl, duration, isCurrent
                    )
                    SELECT position, id, name, artists, albumPicUrl, duration, isCurrent
                    FROM current_playlist
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE current_playlist")
                db.execSQL("ALTER TABLE current_playlist_new RENAME TO current_playlist")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netease_cloud_music_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
