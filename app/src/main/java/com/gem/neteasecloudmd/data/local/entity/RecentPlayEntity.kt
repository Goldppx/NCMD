package com.gem.neteasecloudmd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val artists: String,
    val albumPicUrl: String?,
    val duration: Int,
    val playedAt: Long = System.currentTimeMillis()
)
