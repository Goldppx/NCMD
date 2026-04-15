package com.gem.neteasecloudmd.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gem.neteasecloudmd.ui.screens.LoginScreen
import com.gem.neteasecloudmd.ui.screens.MainScreen
import com.gem.neteasecloudmd.ui.screens.PlaylistDetailScreen
import com.gem.neteasecloudmd.ui.screens.PlaylistListScreen
import com.gem.neteasecloudmd.ui.screens.RecentPlaysScreen
import com.gem.neteasecloudmd.ui.screens.SearchScreen
import com.gem.neteasecloudmd.ui.screens.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    onThemeModeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToPlaylistList = {
                    navController.navigate(Screen.PlaylistList.route)
                },
                onNavigateToRecentPlays = {
                    navController.navigate(Screen.RecentPlays.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPlaylistDetail = { playlistId, playlistName ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId, playlistName))
                }
            )
        }

        composable(Screen.PlaylistList.route) {
            PlaylistListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylistDetail = { playlistId, playlistName ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId, playlistName))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("playlistName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val playlistName = Uri.decode(backStackEntry.arguments?.getString("playlistName") ?: "歌单")
            PlaylistDetailScreen(
                playlistId = playlistId,
                playlistName = playlistName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecentPlays.route) {
            RecentPlaysScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onThemeModeChanged = onThemeModeChanged,
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
