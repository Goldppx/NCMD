package com.gem.neteasecloudmd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_playlist")
data class CurrentPlaylistEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val artists: String,
    val albumPicUrl: String?,
    val duration: Int,
    val position: Int = 0
)
