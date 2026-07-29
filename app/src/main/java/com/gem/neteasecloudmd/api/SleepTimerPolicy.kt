package com.gem.neteasecloudmd.api

object SleepTimerPolicy {
    fun resolveMinutes(presetMinutes: Int, customMinutes: Int): Int {
        return com.gem.neteasecloudmd.core.playback.SleepTimerPolicy.resolveMinutes(
            presetMinutes,
            customMinutes
        )
    }

    fun remainingMinutesCeil(remainingMs: Long): Int =
        com.gem.neteasecloudmd.core.playback.SleepTimerPolicy.remainingMinutesCeil(remainingMs)

    fun shouldStopAtQueueEnd(currentTrackIndex: Int, queueSize: Int): Boolean =
        com.gem.neteasecloudmd.core.playback.SleepTimerPolicy.shouldStopAtQueueEnd(
            currentTrackIndex,
            queueSize
        )
}
