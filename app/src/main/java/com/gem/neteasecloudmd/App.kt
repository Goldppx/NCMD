package com.gem.neteasecloudmd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.navigation.NavGraph
import com.gem.neteasecloudmd.ui.navigation.Screen
import com.gem.neteasecloudmd.ui.screens.PlaybackBar
import com.gem.neteasecloudmd.ui.theme.NeteaseCloudMDTheme

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun NCMDApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val player = rememberPlayerManager(context)
    var themeMode by remember { mutableStateOf(sessionManager.getThemeMode()) }
    var languageMode by remember { mutableStateOf(sessionManager.getLanguageMode()) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val startDestination = if (sessionManager.isLoggedIn()) {
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    val useDarkTheme = when (themeMode) {
        SessionManager.THEME_MODE_LIGHT -> false
        SessionManager.THEME_MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val seedArgb = if (sessionManager.isCoverPaletteEnabled() && player.themeSeedArgb != 0) {
        player.themeSeedArgb
    } else {
        null
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    NeteaseCloudMDTheme(darkTheme = useDarkTheme, seedArgb = seedArgb) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val showPlaybackBar = currentRoute != Screen.Login.route &&
                player.currentPlaylist.isNotEmpty() &&
                !player.isPlaybackBarHidden &&
                currentRoute != Screen.Player.route

            SharedTransitionLayout {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        navController = navController,
                        startDestination = startDestination,
                        onThemeModeChanged = { mode ->
                            themeMode = mode
                        },
                        onLanguageModeChanged = { mode ->
                            languageMode = mode
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = showPlaybackBar,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    ) {
                        PlaybackBar(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onNavigateToPlayer = {
                                navController.navigate(Screen.Player.route)
                            },
                            showPlayBar = true,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}
