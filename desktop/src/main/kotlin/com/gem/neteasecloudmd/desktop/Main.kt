package com.gem.neteasecloudmd.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gem.neteasecloudmd.core.library.LibraryDestination
import com.gem.neteasecloudmd.core.library.LibraryStore
import com.gem.neteasecloudmd.core.library.LibraryUiState
import com.gem.neteasecloudmd.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

fun main() {
    DesktopRuntime.configure()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NCMD Desktop",
            state = rememberWindowState(width = 1_160.dp, height = 760.dp)
        ) {
            val store = remember { LibraryStore(emptyList()) }
            val library = remember { DesktopLocalLibrary(store) }
            LaunchedEffect(library) {
                withContext(Dispatchers.IO) { library.loadSavedLibrary() }
            }
            val player = remember(library) {
                DesktopPlaybackEngine(library::pathForTrack) { update ->
                    store.updatePlayback(
                        status = update.status,
                        isPlaying = update.isPlaying,
                        positionMs = update.positionMs,
                        durationMs = update.durationMs,
                        errorMessage = update.errorMessage
                    )
                }
            }
            DisposableEffect(player) {
                onDispose(player::release)
            }
            val state by store.state.collectAsState()
            val systemDark = isSystemInDarkTheme()
            var appliedDarkTheme by remember { mutableStateOf(systemDark) }
            var themeTransition by remember { mutableStateOf<ThemeTransition?>(null) }
            var nextThemeTransitionId by remember { mutableStateOf(0L) }
            val themeScope = rememberCoroutineScope()
            DesktopTheme(artworkUri = state.playback.currentTrack?.albumPicUrl, darkTheme = appliedDarkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DesktopApp(
                        state = state,
                        store = store,
                        library = library,
                        player = player,
                        darkTheme = appliedDarkTheme,
                        onToggleTheme = { fromColor ->
                            if (themeTransition == null) {
                                val targetDarkTheme = !appliedDarkTheme
                                val transition = ThemeTransition(
                                    id = nextThemeTransitionId,
                                    targetDarkTheme = targetDarkTheme,
                                    phase = ThemeTransitionPhase.COVERING,
                                    fromColor = fromColor
                                )
                                nextThemeTransitionId += 1L
                                themeTransition = transition
                                themeScope.launch {
                                    kotlinx.coroutines.delay(THEME_COVER_DURATION_MS.toLong())
                                    appliedDarkTheme = targetDarkTheme
                                    themeTransition = transition.copy(phase = ThemeTransitionPhase.SWAPPING)
                                    kotlinx.coroutines.delay(THEME_COLOR_SWAP_DURATION_MS.toLong())
                                    themeTransition = transition.copy(phase = ThemeTransitionPhase.REVEALING)
                                    kotlinx.coroutines.delay(THEME_REVEAL_DURATION_MS.toLong())
                                    themeTransition = null
                                }
                            }
                        }
                    )
                    ThemeTransitionOverlay(themeTransition)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DesktopApp(
    state: LibraryUiState,
    store: LibraryStore,
    library: DesktopLocalLibrary,
    player: DesktopPlaybackEngine,
    darkTheme: Boolean,
    onToggleTheme: (Color) -> Unit
) {
    val scope = rememberCoroutineScope()
    val themeBackground = MaterialTheme.colorScheme.background
    var statusMessage by remember { mutableStateOf("Import local music to start your desktop library.") }

    fun importEntries(entries: List<java.nio.file.Path>) {
        if (entries.isEmpty()) return
        scope.launch {
            statusMessage = "Importing music…"
            val result = withContext(Dispatchers.IO) { library.importEntries(entries) }
            statusMessage = when {
                result.addedTrackCount > 0 -> "Added ${result.addedTrackCount} track(s) to your library."
                result.ignoredEntryCount > 0 -> "No supported audio files were found."
                else -> "Those tracks are already in your library."
            }
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Spacebar) {
                state.playback.currentTrack?.let { track ->
                    if (state.playback.isPlaying) player.pause() else player.play(track)
                } != null
            } else {
                false
            }
        },
        topBar = {
            TopAppBar(
                    title = {
                        Column {
                            Text("NCMD")
                            Text(
                                text = "Local music desktop alpha",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { importEntries(selectAudioFiles()) }) {
                            Text("Add files")
                        }
                        TextButton(onClick = { importEntries(selectMusicFolder()) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add folder")
                        }
                    }
            )
        },
        bottomBar = {
            DesktopPlayerBar(
                    state = state,
                    onTogglePlayback = {
                        state.playback.currentTrack?.let { track ->
                            if (state.playback.isPlaying) player.pause() else player.play(track)
                        }
                    },
                    onPrevious = { store.selectPreviousTrack()?.let(player::play) },
                    onNext = { store.selectNextTrack()?.let(player::play) },
                    onSeek = player::seekTo
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DesktopNavigation(
                selected = state.destination,
                onNavigate = store::navigate,
                darkTheme = darkTheme,
                onToggleTheme = { onToggleTheme(themeBackground) }
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (state.visibleTracks.isEmpty()) {
                    EmptyLibraryContent(state.destination, importFiles = { importEntries(selectAudioFiles()) })
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.visibleTracks, key = { _, track -> track.id }) { index, track ->
                            TrackRow(
                                    track = track,
                                    isCurrent = state.playback.currentTrack?.id == track.id,
                                    onClick = {
                                        store.selectTrack(track.id)
                                        player.play(track)
                                    },
                                    onRemove = if (state.destination == LibraryDestination.QUEUE) {
                                        {
                                            store.removeQueueItem(index)?.let { result ->
                                                if (result.removedCurrentTrack) {
                                                    if (result.shouldRestartPlayback) {
                                                        result.replacementTrack?.let(player::play)
                                                    } else {
                                                        player.release()
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        null
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent(destination: LibraryDestination, importFiles: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (destination) {
                LibraryDestination.SEARCH -> "No matching local tracks."
                LibraryDestination.QUEUE -> "The queue is empty."
                LibraryDestination.HOME -> "Your local library is empty."
            },
            style = MaterialTheme.typography.titleMedium
        )
        if (destination == LibraryDestination.HOME) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = importFiles) {
                Text("Add music files")
            }
        }
    }
}

@Composable
private fun DesktopNavigation(
    selected: LibraryDestination,
    onNavigate: (LibraryDestination) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
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
            icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue") },
            label = { Text("Queue") }
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme"
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
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
            LocalArtwork(track.albumPicUrl, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = trackSubtitle(track),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove from queue")
                }
            }
        }
    }
}

@Composable
private fun LocalArtwork(artworkUri: String?, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = null, artworkUri) {
        value = withContext(Dispatchers.IO) {
            artworkUri?.let { uri ->
                runCatching { ImageIO.read(File(URI(uri)))?.toComposeImageBitmap() }.getOrNull()
            }
        }
    }
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        if (image != null) {
            Image(
                bitmap = requireNotNull(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun trackSubtitle(track: Track): String = buildList {
    add(track.artists)
    add(track.albumName)
    if (track.duration > 0) add(formatDuration(track.duration.toLong()))
}.joinToString(" · ")

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun DesktopPlayerBar(
    state: LibraryUiState,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val track = state.playback.currentTrack
    val durationMs = state.playback.durationMs
    var isSeeking by remember(track?.id) { mutableStateOf(false) }
    var sliderPositionMs by remember(track?.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(state.playback.positionMs, durationMs, isSeeking) {
        if (!isSeeking) {
            sliderPositionMs = state.playback.positionMs.coerceIn(0L, durationMs).toFloat()
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderPositionMs,
                onValueChange = {
                    isSeeking = true
                    sliderPositionMs = it
                },
                onValueChangeFinished = {
                    if (durationMs > 0L) onSeek(sliderPositionMs.toLong())
                    isSeeking = false
                },
                enabled = track != null && durationMs > 0L,
                valueRange = 0f..durationMs.toFloat(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalArtwork(track?.albumPicUrl, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track?.name ?: "Choose a local track to start")
                    Text(
                        text = track?.let(::trackSubtitle) ?: "Play music locally in NCMD.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatDuration(state.playback.positionMs)} / ${formatDuration(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPrevious, enabled = state.playback.queue.currentIndex > 0) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                }
                IconButton(onClick = onTogglePlayback, enabled = track != null) {
                    Icon(
                        imageVector = if (state.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.playback.isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = state.playback.queue.currentIndex < state.playback.queue.items.lastIndex
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                }
            }
        }
    }
}

private enum class ThemeTransitionPhase {
    COVERING,
    SWAPPING,
    REVEALING
}

private data class ThemeTransition(
    val id: Long,
    val targetDarkTheme: Boolean,
    val phase: ThemeTransitionPhase,
    val fromColor: Color
)

/**
 * A full-screen veil makes the theme switch atomic from the user's perspective: the old surface
 * is covered first, the content changes behind it, then the veil reveals the new color scheme.
 */
@Composable
private fun ThemeTransitionOverlay(transition: ThemeTransition?) {
    if (transition == null) return

    val opacity = remember(transition.id) { Animatable(0f) }
    val targetBackground = MaterialTheme.colorScheme.background
    val overlayTarget = if (transition.phase == ThemeTransitionPhase.COVERING) {
        transition.fromColor
    } else {
        targetBackground
    }
    val overlayColor by animateColorAsState(
        targetValue = overlayTarget,
        animationSpec = tween(durationMillis = THEME_COLOR_SWAP_DURATION_MS),
        label = "themeTransitionColor"
    )

    LaunchedEffect(transition.id) {
        opacity.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = THEME_COVER_DURATION_MS)
        )
    }
    LaunchedEffect(transition.phase) {
        when (transition.phase) {
            ThemeTransitionPhase.COVERING -> Unit
            ThemeTransitionPhase.SWAPPING -> Unit

            ThemeTransitionPhase.REVEALING -> {
                opacity.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = THEME_REVEAL_DURATION_MS)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = opacity.value)
            .background(overlayColor),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = transition.phase != ThemeTransitionPhase.COVERING,
            animationSpec = tween(durationMillis = THEME_ICON_CROSSFADE_DURATION_MS),
            label = "themeTransitionIcon"
        ) { showingTargetIcon ->
            val showDarkIcon = if (showingTargetIcon) transition.targetDarkTheme else !transition.targetDarkTheme
            Icon(
                imageVector = if (showDarkIcon) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                modifier = Modifier.size(THEME_TRANSITION_ICON_SIZE_DP.dp),
                tint = if (showDarkIcon) Color.White else Color(0xFF202124)
            )
        }
    }
}

private const val THEME_COVER_DURATION_MS = 320
private const val THEME_COLOR_SWAP_DURATION_MS = 260
private const val THEME_REVEAL_DURATION_MS = 360
private const val THEME_ICON_CROSSFADE_DURATION_MS = 220
private const val THEME_TRANSITION_ICON_SIZE_DP = 64
