package com.gem.neteasecloudmd.ui.screens

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.util.UnstableApi
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.SearchAlbumItem
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.api.rememberPlayerManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class SearchTab {
    SONG,
    PLAYLIST,
    ALBUM
}

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
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { NeteaseApiService(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val cookie = sessionManager.getCookie()

    var searchInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedTab by remember { mutableStateOf(SearchTab.SONG) }
    var songResults by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var albumResults by remember { mutableStateOf<List<SearchAlbumItem>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(searchInput.text) {
        if (searchInput.text.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        if (searchInput.text.isNotBlank()) {
            suggestions = apiService.searchSuggest(searchInput.text.trim()).getOrDefault(emptyList()).take(8)
        }
    }

    suspend fun performSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return

        hasSearched = true
        isSearching = true

        when (selectedTab) {
            SearchTab.SONG -> {
                songResults = apiService.searchSongs(q, 30).getOrDefault(emptyList())
            }
            SearchTab.PLAYLIST -> {
                playlistResults = apiService.searchPlaylists(q, 30).getOrDefault(emptyList())
            }
            SearchTab.ALBUM -> {
                albumResults = apiService.searchAlbums(q, 30).getOrDefault(emptyList())
            }
        }

        isSearching = false
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
                    hasSearched = false
                    songResults = emptyList()
                    playlistResults = emptyList()
                    albumResults = emptyList()
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
                            scope.launch { performSearch(q) }
                        }
                    }
                ),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val q = searchInput.text.trim()
                            if (q.isBlank()) return@TextButton
                            scope.launch {
                                performSearch(q)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.common_search))
                    }
                },
                singleLine = true
            )

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
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

            LaunchedEffect(selectedTab) {
                if (hasSearched && searchInput.text.isNotBlank()) {
                    performSearch(searchInput.text.trim())
                }
            }

            when {
                isSearching -> {
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

                suggestions.isNotEmpty() && !hasSearched -> {
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
                        items(suggestions) { suggestion ->
                            Card(
                                    onClick = {
                                        searchInput = TextFieldValue(suggestion, selection = TextRange(0, suggestion.length))
                                        scope.launch { performSearch(suggestion) }
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

                !hasSearched -> {
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
                    when (selectedTab) {
                        SearchTab.SONG -> SongSearchResults(
                            songs = songResults,
                            onPlaySong = { track ->
                                if (cookie.isBlank()) {
                                    Toast.makeText(context, resources.getString(R.string.search_need_login), Toast.LENGTH_SHORT).show()
                                } else {
                                    player.setCookie(cookie)
                                    player.setPlaylist(songResults, songResults.indexOf(track))
                                    Toast.makeText(context, resources.getString(R.string.main_play_track_toast, track.name), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        SearchTab.PLAYLIST -> PlaylistSearchResults(
                            playlists = playlistResults,
                            onClick = { playlist ->
                                onNavigateToSearchDetail("playlist", playlist.id, playlist.name)
                            }
                        )
                        SearchTab.ALBUM -> AlbumSearchResults(
                            albums = albumResults,
                            onClick = { album ->
                                onNavigateToSearchDetail("album", album.id, album.name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongSearchResults(
    songs: List<TrackItem>,
    onPlaySong: (TrackItem) -> Unit
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
                onClick = { onPlaySong(track) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
