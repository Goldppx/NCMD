package com.gem.neteasecloudmd.api

object SleepTimerPolicy {
    fun resolveMinutes(presetMinutes: Int, customMinutes: Int): Int {
        return when (presetMinutes) {
            SessionManager.SLEEP_TIMER_PRESET_DISABLED -> 0
            SessionManager.SLEEP_TIMER_PRESET_15,
            SessionManager.SLEEP_TIMER_PRESET_30,
            SessionManager.SLEEP_TIMER_PRESET_45,
            SessionManager.SLEEP_TIMER_PRESET_60 -> presetMinutes
            SessionManager.SLEEP_TIMER_PRESET_CUSTOM -> customMinutes.coerceIn(1, 240)
            else -> 0
        }
    }

    fun remainingMinutesCeil(remainingMs: Long): Int {
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 59_999L) / 60_000L).toInt()
    }

    fun shouldStopAtQueueEnd(currentTrackIndex: Int, queueSize: Int): Boolean {
        if (queueSize <= 0) return true
        return currentTrackIndex >= queueSize - 1
    }
}
