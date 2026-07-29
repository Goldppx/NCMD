package com.gem.neteasecloudmd.core.model

/**
 * Platform-neutral music metadata used by UI, playback, persistence, and API layers.
 */
data class Track(
    val id: Long,
    val name: String,
    val artists: String,
    val albumName: String,
    val albumPicUrl: String?,
    val duration: Int = 0
)
