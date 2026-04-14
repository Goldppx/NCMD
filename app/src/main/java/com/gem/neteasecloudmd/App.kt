package com.gem.neteasecloudmd

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gem.neteasecloudmd.ui.navigation.NavGraph
import com.gem.neteasecloudmd.ui.navigation.Screen
import com.gem.neteasecloudmd.ui.theme.NeteaseCloudMDTheme

@Composable
fun NCMDApp() {
    val navController = rememberNavController()
    val startDestination = Screen.Login.route

    NeteaseCloudMDTheme {
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
