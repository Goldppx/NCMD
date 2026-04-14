package com.gem.neteasecloudmd.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Main : Screen("main")
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    data object PlaylistList : Screen("playlist_list")
    data object RecentPlays : Screen("recent_plays")
    data object Search : Screen("search")
}
