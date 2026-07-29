package com.gem.neteasecloudmd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.ui.common.LocalPlaybackBarInset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToDisplay: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val menuItems = listOf(
        SettingsMenuItem(
            title = stringResource(R.string.settings_playback),
            description = stringResource(R.string.settings_playback_desc),
            icon = Icons.Default.AudioFile,
            onClick = onNavigateToPlayback
        ),
        SettingsMenuItem(
            title = stringResource(R.string.settings_storage),
            description = stringResource(R.string.settings_storage_desc),
            icon = Icons.Default.Storage,
            onClick = onNavigateToStorage
        ),
        SettingsMenuItem(
            title = stringResource(R.string.settings_display),
            description = stringResource(R.string.settings_display_desc),
            icon = Icons.Default.Visibility,
            onClick = onNavigateToDisplay
        ),
        SettingsMenuItem(
            title = stringResource(R.string.settings_account),
            description = stringResource(R.string.settings_account_desc),
            icon = Icons.Default.Person,
            onClick = onNavigateToAccount
        ),
        SettingsMenuItem(
            title = stringResource(R.string.settings_about),
            description = stringResource(R.string.settings_about_desc),
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = LocalPlaybackBarInset.current)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(menuItems) { item ->
                SettingsMenuRow(item)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private data class SettingsMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SettingsMenuRow(item: SettingsMenuItem) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}
