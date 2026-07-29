package com.gem.neteasecloudmd.core.playback

data class PrefetchedUrl(
    val queueRevision: Long,
    val trackId: Long,
    val url: String
)

object PlaybackRequestPolicy {
    fun canUsePrefetchedUrl(
        prefetchedUrl: PrefetchedUrl?,
        queueRevision: Long,
        trackId: Long
    ): Boolean = prefetchedUrl?.queueRevision == queueRevision && prefetchedUrl.trackId == trackId
}
