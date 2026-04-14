package com.gem.neteasecloudmd

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.ui.navigation.NavGraph
import com.gem.neteasecloudmd.ui.navigation.Screen
import com.gem.neteasecloudmd.ui.theme.NCMDTheme

@Composable
fun NCMDApp() {
    val sessionManager = SessionManager.getInstance()
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState()

    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route

    NCMDTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}
