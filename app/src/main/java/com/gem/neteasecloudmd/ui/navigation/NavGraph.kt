package com.gem.neteasecloudmd.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.ui.screens.LoginScreen
import com.gem.neteasecloudmd.ui.screens.MainScreen
import com.gem.neteasecloudmd.ui.screens.PlaylistDetailScreen
import com.gem.neteasecloudmd.ui.screens.PlaylistListScreen
import com.gem.neteasecloudmd.ui.screens.RecentPlaysScreen
import com.gem.neteasecloudmd.ui.screens.SearchScreen
import com.gem.neteasecloudmd.ui.screens.SettingsScreen
import com.gem.neteasecloudmd.ui.screens.PlaybackSettingsScreen
import com.gem.neteasecloudmd.ui.screens.StorageSettingsScreen
import com.gem.neteasecloudmd.ui.screens.DisplaySettingsScreen
import com.gem.neteasecloudmd.ui.screens.AccountSettingsScreen
import com.gem.neteasecloudmd.ui.screens.AboutSettingsScreen
import com.gem.neteasecloudmd.ui.screens.LicensesSettingsScreen
import com.gem.neteasecloudmd.ui.screens.LogScreen
import com.gem.neteasecloudmd.ui.screens.PlayerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    sharedTransitionScope: SharedTransitionScope,
    navController: NavHostController,
    startDestination: String,
    onThemeModeChanged: (Int) -> Unit,
    onLanguageModeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            )
        }
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable,
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
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToPlaylistDetail = { type, playlistId, playlistName ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(type, playlistId, playlistName))
                },
                onNavigateToPlayer = {
                    navController.navigate(Screen.Player.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.PlaylistList.route) {
            PlaylistListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylistDetail = { type, playlistId, playlistName ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(type, playlistId, playlistName))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("playlistName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "playlist"
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val playlistName = Uri.decode(
                backStackEntry.arguments?.getString("playlistName")
                    ?: resources.getString(R.string.nav_default_playlist_name)
            )

            PlaylistDetailScreen(
                type = type,
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { type, id, name ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(type, id, name))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayback = { navController.navigate(Screen.SettingsPlayback.route) },
                onNavigateToStorage = { navController.navigate(Screen.SettingsStorage.route) },
                onNavigateToDisplay = { navController.navigate(Screen.SettingsDisplay.route) },
                onNavigateToAccount = { navController.navigate(Screen.SettingsAccount.route) },
                onNavigateToAbout = { navController.navigate(Screen.SettingsAbout.route) }
            )
        }

        composable(Screen.SettingsPlayback.route) {
            PlaybackSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsStorage.route) {
            StorageSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SettingsDisplay.route) {
            DisplaySettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onThemeModeChanged = onThemeModeChanged,
                onLanguageModeChanged = onLanguageModeChanged
            )
        }

        composable(Screen.SettingsAccount.route) {
            AccountSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SettingsAbout.route) {
            AboutSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLog = { navController.navigate(Screen.Log.route) },
                onNavigateToLicenses = { navController.navigate(Screen.SettingsLicenses.route) }
            )
        }

        composable(Screen.SettingsLicenses.route) {
            LicensesSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Log.route) {
            LogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            PlayerScreen(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
