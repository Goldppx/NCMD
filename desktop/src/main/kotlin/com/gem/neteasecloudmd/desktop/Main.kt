package com.gem.neteasecloudmd.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.gem.neteasecloudmd.core.library.LibraryDestination
import com.gem.neteasecloudmd.core.library.LibraryStore
import com.gem.neteasecloudmd.core.library.LibraryUiState
import com.gem.neteasecloudmd.core.model.Track

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NCMD Desktop Preview"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                val store = remember { LibraryStore(previewTracks) }
                val state by store.state.collectAsState()
                DesktopApp(state = state, store = store)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DesktopApp(state: LibraryUiState, store: LibraryStore) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NCMD")
                        Text(
                            text = "Kotlin Multiplatform desktop preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = { DesktopPlayerBar(state, store::togglePlayback) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DesktopNavigation(
                selected = state.destination,
                onNavigate = store::navigate
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                if (state.destination == LibraryDestination.SEARCH) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = store::updateQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search your library") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = when (state.destination) {
                        LibraryDestination.HOME -> "Your library"
                        LibraryDestination.SEARCH -> "Search results"
                        LibraryDestination.QUEUE -> "Playback queue"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(12.dp))

                if (state.visibleTracks.isEmpty()) {
                    Text(
                        text = if (state.destination == LibraryDestination.SEARCH) {
                            "Type a song, artist, or album name to search."
                        } else {
                            "No tracks available yet."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.visibleTracks, key = Track::id) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = state.playback.currentTrack?.id == track.id,
                                onClick = { store.selectTrack(track.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopNavigation(
    selected: LibraryDestination,
    onNavigate: (LibraryDestination) -> Unit
) {
    NavigationRail {
        NavigationRailItem(
            selected = selected == LibraryDestination.HOME,
            onClick = { onNavigate(LibraryDestination.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationRailItem(
            selected = selected == LibraryDestination.SEARCH,
            onClick = { onNavigate(LibraryDestination.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationRailItem(
            selected = selected == LibraryDestination.QUEUE,
            onClick = { onNavigate(LibraryDestination.QUEUE) },
            icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Queue") },
            label = { Text("Queue") }
        )
    }
}

@Composable
private fun TrackRow(track: Track, isCurrent: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCurrent) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${track.artists} · ${track.albumName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DesktopPlayerBar(state: LibraryUiState, onTogglePlayback: () -> Unit) {
    val track = state.playback.currentTrack
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track?.name ?: "Choose a track to start")
                Text(
                    text = track?.artists ?: "Playback controls are shared-state preview only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onTogglePlayback, enabled = track != null) {
                Icon(
                    imageVector = if (state.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}

private val previewTracks = listOf(
    Track(1, "Graduation", "BrAnTB", "Graduation", null),
    Track(2, "Rain", "SASIOVERLXRD", "Night", null),
    Track(3, "Trouble Maker", "Yamy", "Trouble Maker", null)
)
