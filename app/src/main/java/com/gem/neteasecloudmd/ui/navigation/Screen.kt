package com.gem.neteasecloudmd.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Main : Screen("main")
    data object PlaylistDetail : Screen("collection/{type}/{playlistId}/{playlistName}") {
        fun createRoute(type: String, playlistId: Long, playlistName: String) =
            "collection/$type/$playlistId/${Uri.encode(playlistName)}"
    }
    data object PlaylistList : Screen("playlist_list")
    data object RecentPlays : Screen("recent_plays")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object SettingsPlayback : Screen("settings/playback")
    data object SettingsStorage : Screen("settings/storage")
    data object SettingsDisplay : Screen("settings/display")
    data object SettingsAccount : Screen("settings/account")
    data object SettingsAbout : Screen("settings/about")
    data object SettingsLicenses : Screen("settings/licenses")
    data object Log : Screen("log")
    data object Player : Screen("player")
}
