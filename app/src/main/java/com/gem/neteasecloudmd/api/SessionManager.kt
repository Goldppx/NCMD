package com.gem.neteasecloudmd.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val cookie: String
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("netease_session", Context.MODE_PRIVATE)

    companion object {
        const val THEME_MODE_SYSTEM = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2
        const val LANGUAGE_SYSTEM = 0
        const val LANGUAGE_ZH_CN = 1
        const val LANGUAGE_ZH_TW = 2
        const val LANGUAGE_EN = 3
        const val SLEEP_TIMER_PRESET_DISABLED = 0
        const val SLEEP_TIMER_PRESET_15 = 15
        const val SLEEP_TIMER_PRESET_30 = 30
        const val SLEEP_TIMER_PRESET_45 = 45
        const val SLEEP_TIMER_PRESET_60 = 60
        const val SLEEP_TIMER_PRESET_CUSTOM = -1

        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DISABLE_COVER_OVERFLOW = "disable_cover_overflow"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE_MODE = "language_mode"
        private const val KEY_USE_LOCAL_RECENT_PLAYS = "use_local_recent_plays"
        private const val KEY_ENABLE_COVER_PALETTE = "enable_cover_palette"
        private const val KEY_SLEEP_TIMER_PRESET_MINUTES = "sleep_timer_preset_minutes"
        private const val KEY_SLEEP_TIMER_CUSTOM_MINUTES = "sleep_timer_custom_minutes"
        private const val KEY_SLEEP_TIMER_WAIT_FOR_QUEUE_END = "sleep_timer_wait_for_queue_end"

        fun languageTagFromMode(mode: Int): String {
            return when (mode) {
                LANGUAGE_ZH_CN -> "zh-CN"
                LANGUAGE_ZH_TW -> "zh-TW"
                LANGUAGE_EN -> "en"
                else -> ""
            }
        }
    }

    fun saveLoginResult(result: LoginResult, cookie: String) {
        val profile = result.profile
        prefs.edit()
            .putLong(KEY_USER_ID, profile?.userId ?: 0L)
            .putString(KEY_NICKNAME, profile?.nickname ?: "")
            .putString(KEY_AVATAR_URL, profile?.avatarUrl)
            .putString(KEY_COOKIE, cookie)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun getNickname(): String = prefs.getString(KEY_NICKNAME, "") ?: ""

    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun getCookie(): String = prefs.getString(KEY_COOKIE, "") ?: ""

    fun isCoverOverflowDisabled(): Boolean = prefs.getBoolean(KEY_DISABLE_COVER_OVERFLOW, false)

    fun setCoverOverflowDisabled(disabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_COVER_OVERFLOW, disabled).apply()
    }

    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun getLanguageMode(): Int = prefs.getInt(KEY_LANGUAGE_MODE, LANGUAGE_SYSTEM)

    fun setLanguageMode(mode: Int) {
        prefs.edit().putInt(KEY_LANGUAGE_MODE, mode).apply()
    }

    fun useLocalRecentPlays(): Boolean = prefs.getBoolean(KEY_USE_LOCAL_RECENT_PLAYS, true)

    fun setUseLocalRecentPlays(useLocal: Boolean) {
        prefs.edit().putBoolean(KEY_USE_LOCAL_RECENT_PLAYS, useLocal).apply()
    }

    fun isCoverPaletteEnabled(): Boolean = prefs.getBoolean(KEY_ENABLE_COVER_PALETTE, false)

    fun setCoverPaletteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_COVER_PALETTE, enabled).apply()
    }

    fun getSleepTimerPresetMinutes(): Int {
        return prefs.getInt(KEY_SLEEP_TIMER_PRESET_MINUTES, SLEEP_TIMER_PRESET_DISABLED)
    }

    fun setSleepTimerPresetMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER_PRESET_MINUTES, minutes).apply()
    }

    fun getSleepTimerCustomMinutes(): Int {
        return prefs.getInt(KEY_SLEEP_TIMER_CUSTOM_MINUTES, 20).coerceIn(1, 240)
    }

    fun setSleepTimerCustomMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SLEEP_TIMER_CUSTOM_MINUTES, minutes.coerceIn(1, 240)).apply()
    }

    fun getSleepTimerWaitForQueueEnd(): Boolean {
        return prefs.getBoolean(KEY_SLEEP_TIMER_WAIT_FOR_QUEUE_END, false)
    }

    fun setSleepTimerWaitForQueueEnd(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLEEP_TIMER_WAIT_FOR_QUEUE_END, enabled).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
