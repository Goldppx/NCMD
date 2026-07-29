package com.gem.neteasecloudmd.core.playback

object SleepTimerPolicy {
    const val DISABLED = 0
    const val CUSTOM = -1

    fun resolveMinutes(presetMinutes: Int, customMinutes: Int): Int = when (presetMinutes) {
        15, 30, 45, 60 -> presetMinutes
        CUSTOM -> customMinutes.coerceIn(1, 240)
        else -> DISABLED
    }

    fun remainingMinutesCeil(remainingMs: Long): Int {
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 59_999L) / 60_000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    fun shouldStopAtQueueEnd(currentTrackIndex: Int, queueSize: Int): Boolean =
        queueSize <= 0 || currentTrackIndex >= queueSize - 1
}
