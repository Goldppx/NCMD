package com.gem.neteasecloudmd.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerPolicyTest {

    @Test
    fun resolveMinutes_returnsPresetValue() {
        assertEquals(15, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_15, 20))
        assertEquals(30, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_30, 20))
        assertEquals(45, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_45, 20))
        assertEquals(60, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_60, 20))
    }

    @Test
    fun resolveMinutes_handlesCustomAndBounds() {
        assertEquals(20, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_CUSTOM, 20))
        assertEquals(1, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_CUSTOM, 0))
        assertEquals(240, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_CUSTOM, 500))
    }

    @Test
    fun resolveMinutes_handlesDisabledAndUnknown() {
        assertEquals(0, SleepTimerPolicy.resolveMinutes(SessionManager.SLEEP_TIMER_PRESET_DISABLED, 20))
        assertEquals(0, SleepTimerPolicy.resolveMinutes(999, 20))
    }

    @Test
    fun remainingMinutesCeil_roundsUpAndHandlesZero() {
        assertEquals(0, SleepTimerPolicy.remainingMinutesCeil(0))
        assertEquals(1, SleepTimerPolicy.remainingMinutesCeil(1))
        assertEquals(1, SleepTimerPolicy.remainingMinutesCeil(60_000))
        assertEquals(2, SleepTimerPolicy.remainingMinutesCeil(60_001))
    }

    @Test
    fun shouldStopAtQueueEnd_behavesCorrectly() {
        assertTrue(SleepTimerPolicy.shouldStopAtQueueEnd(0, 0))
        assertFalse(SleepTimerPolicy.shouldStopAtQueueEnd(0, 3))
        assertTrue(SleepTimerPolicy.shouldStopAtQueueEnd(2, 3))
    }
}
