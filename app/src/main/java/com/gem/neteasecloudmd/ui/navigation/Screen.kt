package com.gem.neteasecloudmd.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Main : Screen("main")
    data object PlaylistDetail : Screen("playlist/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: Long, playlistName: String) =
            "playlist/$playlistId/${Uri.encode(playlistName)}"
    }
    data object PlaylistList : Screen("playlist_list")
    data object RecentPlays : Screen("recent_plays")
    data object Search : Screen("search")
    data object SearchDetail : Screen("search_detail/{type}/{id}/{name}") {
        fun createRoute(type: String, id: Long, name: String) =
            "search_detail/$type/$id/${Uri.encode(name)}"
    }
    data object Settings : Screen("settings")
}
