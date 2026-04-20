package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SearchAlbumItem
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import com.gem.neteasecloudmd.ui.components.SongLongPressMenu
import com.gem.neteasecloudmd.ui.viewmodel.SearchTab
import com.gem.neteasecloudmd.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearchDetail: (type: String, id: Long, name: String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val player = rememberPlayerManager(context)
    val searchViewModel: SearchViewModel = viewModel()
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val cookie = searchViewModel.getCookie()

    var searchInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }

    fun loadMenuPlaylists() {
        searchViewModel.loadMenuPlaylists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    searchViewModel.updateQuery(it.text)
                },
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && searchInput.text.isNotEmpty()) {
                            searchInput = searchInput.copy(
                                selection = TextRange(0, searchInput.text.length)
                            )
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val q = searchInput.text.trim()
                        if (q.isNotBlank()) {
                            searchViewModel.performSearch()
                        }
                    }
                ),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val q = searchInput.text.trim()
                            if (q.isBlank()) return@TextButton
                            searchViewModel.performSearch()
                        }
                    ) {
                        Text(stringResource(R.string.common_search))
                    }
                },
                singleLine = true
            )

            PrimaryScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = {
                            searchViewModel.selectTab(tab)
                        },
                        text = {
                            Text(
                                when (tab) {
                                    SearchTab.SONG -> stringResource(R.string.search_song_tab)
                                    SearchTab.PLAYLIST -> stringResource(R.string.search_playlist_tab)
                                    SearchTab.ALBUM -> stringResource(R.string.search_album_tab)
                                }
                            )
                        }
                    )
                }
            }

            when {
                uiState.isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                searchInput.text.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.search_tap_to_get_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.suggestions.isNotEmpty() && !uiState.hasSearched -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.search_suggestions),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(uiState.suggestions) { suggestion ->
                            Card(
                                    onClick = {
                                        searchInput = TextFieldValue(suggestion, selection = TextRange(0, suggestion.length))
                                        searchViewModel.selectSuggestion(suggestion)
                                    },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                !uiState.hasSearched -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.search_tap_to_get_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    when (uiState.selectedTab) {
                        SearchTab.SONG -> SongSearchResults(
                            songs = uiState.songResults,
                            onPlaySong = { track ->
                                if (cookie.isBlank()) {
                                    Toast.makeText(context, resources.getString(R.string.search_need_login), Toast.LENGTH_SHORT).show()
                                } else {
                                    player.setCookie(cookie)
                                    player.setPlaylist(uiState.songResults, uiState.songResults.indexOf(track))
                                    Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongPressSong = { track ->
                                selectedTrackForMenu = track
                            }
                        )

                        SearchTab.PLAYLIST -> PlaylistSearchResults(
                            playlists = uiState.playlistResults,
                            onClick = { playlist ->
                                onNavigateToSearchDetail("playlist", playlist.id, playlist.name)
                            }
                        )
                        SearchTab.ALBUM -> AlbumSearchResults(
                            albums = uiState.albumResults,
                            onClick = { album ->
                                onNavigateToSearchDetail("album", album.id, album.name)
                            }
                        )
                    }
                }
            }
        }
    }

    SongLongPressMenu(
        track = selectedTrackForMenu,
        playlists = uiState.playlistsForMenu,
        onDismiss = { selectedTrackForMenu = null },
        onRequestLoadPlaylists = { loadMenuPlaylists() },
        onPlayTrack = { track ->
            val index = uiState.songResults.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.setCookie(cookie)
                player.setPlaylist(uiState.songResults, index)
                Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
            }
        },
        onAddToQueue = { track ->
            player.appendToQueue(listOf(track))
            Toast.makeText(context, resources.getString(R.string.song_menu_queue_success), Toast.LENGTH_SHORT).show()
        },
        onAddToPlaylist = { trackId, playlistId ->
            scope.launch {
                val result = searchViewModel.addTrackToPlaylist(playlistId, trackId)
                result.fold(
                    onSuccess = {
                        Toast.makeText(context, resources.getString(R.string.song_menu_add_success), Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            resources.getString(R.string.song_menu_add_failed, e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        },
        onCopyShareLink = { track ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val link = "https://music.163.com/#/song?id=${track.id}"
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.common_search), link))
            Toast.makeText(context, resources.getString(R.string.song_menu_share_link_copied), Toast.LENGTH_SHORT).show()
        },
        onRemoveFromCurrent = {
            Toast.makeText(context, resources.getString(R.string.song_menu_remove_not_supported), Toast.LENGTH_SHORT).show()
        },
        showCopyShareLink = true,
        showRemoveFromCurrent = false
    )
}

@Composable
private fun SongSearchResults(
    songs: List<TrackItem>,
    onPlaySong: (TrackItem) -> Unit,
    onLongPressSong: (TrackItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (songs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_no_song),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(songs) { track ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onPlaySong(track) },
                            onLongClick = { onLongPressSong(track) }
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artists,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSearchResults(
    playlists: List<PlaylistItem>,
    onClick: (PlaylistItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (playlists.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_no_playlist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(playlists) { playlist ->
            Card(
                onClick = { onClick(playlist) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.playlist_detail_track_count, playlist.trackCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSearchResults(
    albums: List<SearchAlbumItem>,
    onClick: (SearchAlbumItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (albums.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_no_album),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(albums) { album ->
            Card(
                onClick = { onClick(album) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
