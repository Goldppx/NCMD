package com.gem.neteasecloudmd.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScreenTest {

    @Test
    fun login_route() {
        assertEquals("login", Screen.Login.route)
    }

    @Test
    fun main_route() {
        assertEquals("main", Screen.Main.route)
    }

    @Test
    fun playlistDetail_route() {
        assertEquals("collection/{type}/{playlistId}/{playlistName}", Screen.PlaylistDetail.route)
    }

    @Test
    fun playlistDetail_createRoute_basic() {
        val route = Screen.PlaylistDetail.createRoute("album", 12345L, "My Playlist")
        assertEquals("collection/album/12345/My%20Playlist", route)
    }

    @Test
    fun playlistDetail_createRoute_encodesSpecialChars() {
        val route = Screen.PlaylistDetail.createRoute("artist", 99L, "Best & Hits / 2024")
        assertEquals("collection/artist/99/Best%20%26%20Hits%20%2F%202024", route)
    }

    @Test
    fun playlistDetail_createRoute_encodesUnicode() {
        val route = Screen.PlaylistDetail.createRoute("playlist", 1L, "中文歌单")
        assertEquals("collection/playlist/1/%E4%B8%AD%E6%96%87%E6%AD%8C%E5%8D%95", route)
    }

    @Test
    fun playlistList_route() {
        assertEquals("playlist_list", Screen.PlaylistList.route)
    }

    @Test
    fun recentPlays_route() {
        assertEquals("recent_plays", Screen.RecentPlays.route)
    }

    @Test
    fun search_route() {
        assertEquals("search", Screen.Search.route)
    }

    @Test
    fun settings_route() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun settingsPlayback_route() {
        assertEquals("settings/playback", Screen.SettingsPlayback.route)
    }

    @Test
    fun settingsStorage_route() {
        assertEquals("settings/storage", Screen.SettingsStorage.route)
    }

    @Test
    fun settingsDisplay_route() {
        assertEquals("settings/display", Screen.SettingsDisplay.route)
    }

    @Test
    fun settingsAccount_route() {
        assertEquals("settings/account", Screen.SettingsAccount.route)
    }

    @Test
    fun settingsAbout_route() {
        assertEquals("settings/about", Screen.SettingsAbout.route)
    }

    @Test
    fun settingsLicenses_route() {
        assertEquals("settings/licenses", Screen.SettingsLicenses.route)
    }

    @Test
    fun log_route() {
        assertEquals("log", Screen.Log.route)
    }

    @Test
    fun player_route() {
        assertEquals("player", Screen.Player.route)
    }

    @Test
    fun allRoutes_areUnique() {
        val routes = listOf(
            Screen.Login.route,
            Screen.Main.route,
            Screen.PlaylistDetail.route,
            Screen.PlaylistList.route,
            Screen.RecentPlays.route,
            Screen.Search.route,
            Screen.Settings.route,
            Screen.SettingsPlayback.route,
            Screen.SettingsStorage.route,
            Screen.SettingsDisplay.route,
            Screen.SettingsAccount.route,
            Screen.SettingsAbout.route,
            Screen.SettingsLicenses.route,
            Screen.Log.route,
            Screen.Player.route,
        )
        assertEquals(routes.size, routes.distinct().size)
    }
}
