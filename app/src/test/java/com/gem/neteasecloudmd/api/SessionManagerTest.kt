package com.gem.neteasecloudmd.api

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val storage = mutableMapOf<String, Int>()

    @Before
    fun setUp() {
        val context = mockk<Context>()
        prefs = mockk()
        editor = mockk()

        every { context.getSharedPreferences("netease_session", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putInt(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.apply() } just runs
        every { prefs.getInt(any(), any()) } answers {
            storage[firstArg()] ?: secondArg()
        }

        sessionManager = SessionManager(context)
    }

    @Test
    fun themeMode_defaultsToSystem() = runTest {
        assertEquals(SessionManager.THEME_MODE_SYSTEM, sessionManager.getThemeMode())
    }

    @Test
    fun setThemeMode_persistsSelectedTheme() = runTest {
        sessionManager.setThemeMode(SessionManager.THEME_MODE_DARK)
        assertEquals(SessionManager.THEME_MODE_DARK, sessionManager.getThemeMode())

        sessionManager.setThemeMode(SessionManager.THEME_MODE_LIGHT)
        assertEquals(SessionManager.THEME_MODE_LIGHT, sessionManager.getThemeMode())
    }

    @Test
    fun languageMode_defaultsToSystem() = runTest {
        assertEquals(SessionManager.LANGUAGE_SYSTEM, sessionManager.getLanguageMode())
    }

    @Test
    fun setLanguageMode_persistsSelectedLanguage() = runTest {
        sessionManager.setLanguageMode(SessionManager.LANGUAGE_EN)
        assertEquals(SessionManager.LANGUAGE_EN, sessionManager.getLanguageMode())

        sessionManager.setLanguageMode(SessionManager.LANGUAGE_ZH_TW)
        assertEquals(SessionManager.LANGUAGE_ZH_TW, sessionManager.getLanguageMode())
    }

    @Test
    fun languageTagFromMode_mapsExpectedTags() = runTest {
        assertEquals("zh-CN", SessionManager.languageTagFromMode(SessionManager.LANGUAGE_ZH_CN))
        assertEquals("zh-TW", SessionManager.languageTagFromMode(SessionManager.LANGUAGE_ZH_TW))
        assertEquals("en", SessionManager.languageTagFromMode(SessionManager.LANGUAGE_EN))
        assertEquals("", SessionManager.languageTagFromMode(SessionManager.LANGUAGE_SYSTEM))
        assertEquals("", SessionManager.languageTagFromMode(999))
    }
}
