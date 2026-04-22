package com.gem.neteasecloudmd.ui.components

import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.TrackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCollectionScaffold(
    title: String,
    tracks: List<TrackItem>,
    playlistsForMenu: List<PlaylistItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    onBatchModeChanged: (Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrackAt: (index: Int, track: TrackItem) -> Unit,
    onPlayTrackFromMenu: (track: TrackItem) -> Unit,
    onAddTrackToQueue: (track: TrackItem) -> Unit,
    onAddTracksToQueue: (trackIds: Set<Long>) -> Unit,
    onRemoveSingleFromCurrent: (track: TrackItem) -> Unit,
    onRemoveBatchFromCurrent: (trackIds: Set<Long>) -> Unit,
    onRequestLoadPlaylists: () -> Unit,
    onAddSingleToPlaylist: (trackId: Long, playlistId: Long) -> Unit,
    onAddBatchToPlaylist: (trackIds: Set<Long>, playlistId: Long) -> Unit,
    onCopyShareLink: (track: TrackItem) -> Unit,
    showRemoveFromCurrentInMenu: Boolean,
    onPlaySelected: (trackIds: Set<Long>) -> Unit,
    showCopyShareLinkInMenu: Boolean,
    emptyText: String,
    showLikeIndicator: Boolean,
    isTrackLiked: (TrackItem) -> Boolean = { false },
    onSingleRightAction: ((TrackItem) -> Unit)? = null,
    singleRightActionDescription: String = "",
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false,
    totalTrackCount: Int = tracks.size
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }
    var showBatchPlaylistPicker by remember { mutableStateOf(false) }

    LaunchedEffect(tracks) {
        selectedTrackIds = selectedTrackIds.intersect(tracks.map { it.id }.toSet())
    }

    DisposableEffect(isBatchMode) {
        onBatchModeChanged(isBatchMode)
        onDispose {
            onBatchModeChanged(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isBatchMode) {
                        Text(stringResource(R.string.playlist_detail_batch_selected, selectedTrackIds.size))
                    } else {
                        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isBatchMode) {
                            isBatchMode = false
                            selectedTrackIds = emptySet()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (isBatchMode) {
                        TextButton(onClick = {
                            selectedTrackIds = if (selectedTrackIds.size == tracks.size) {
                                emptySet()
                            } else {
                                tracks.map { it.id }.toSet()
                            }
                        }) {
                            Text(
                                text = if (selectedTrackIds.size == tracks.size) {
                                    stringResource(R.string.playlist_detail_unselect_all)
                                } else {
                                    stringResource(R.string.playlist_detail_select_all)
                                }
                            )
                        }
                    } else if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (isBatchMode) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (selectedTrackIds.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.playlist_detail_batch_need_selection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onRequestLoadPlaylists()
                                    showBatchPlaylistPicker = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_detail_batch_add),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        TextButton(
                            onClick = {
                                if (selectedTrackIds.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.playlist_detail_batch_need_selection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                onPlaySelected(selectedTrackIds)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_detail_batch_play_selected),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        TextButton(
                            onClick = {
                                if (selectedTrackIds.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.playlist_detail_batch_need_selection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                onAddTracksToQueue(selectedTrackIds)
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_detail_batch_add_to_queue),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        TextButton(
                            onClick = {
                                if (selectedTrackIds.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.playlist_detail_batch_need_selection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                onRemoveBatchFromCurrent(selectedTrackIds)
                                selectedTrackIds = emptySet()
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_detail_batch_remove),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val content: @Composable () -> Unit = {
            when {
                isLoading && tracks.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null && tracks.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.common_error_with_prefix, errorMessage),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                tracks.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = emptyText, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                else -> {
                    val listState = rememberLazyListState()
                    // 使用 totalItemsCount（含 header/spacer 2项）计算阈值，
                    // 当最后可见 item 距离列表末尾 ≤ 5 时触发加载
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            val total = listState.layoutInfo.totalItemsCount
                            lastVisibleItem != null && total > 0 &&
                                lastVisibleItem.index >= total - 5
                        }
                    }

                    // 以 shouldLoadMore 和 isLoadingMore 共同作为 key：
                    // 当 isLoadingMore 从 true→false（本批加载完成）且用户仍在底部时重新触发
                    LaunchedEffect(shouldLoadMore, isLoadingMore) {
                        if (shouldLoadMore && onLoadMore != null && !isLoadingMore) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            TrackCollectionHeaderCard(
                                trackCount = totalTrackCount,
                                isBatchMode = isBatchMode,
                                onPlayAllClick = onPlayAll,
                                onBatchToggleClick = {
                                    if (isBatchMode) {
                                        isBatchMode = false
                                        selectedTrackIds = emptySet()
                                    } else {
                                        isBatchMode = true
                                        selectedTrackIds = emptySet()
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        itemsIndexed(tracks) { index, track ->
                            TrackCollectionItemCard(
                                track = track,
                                index = index + 1,
                                isBatchMode = isBatchMode,
                                isSelected = selectedTrackIds.contains(track.id),
                                showLikeIndicator = showLikeIndicator,
                                isTrackLiked = isTrackLiked(track),
                                onSingleRightAction = onSingleRightAction,
                                singleRightActionDescription = singleRightActionDescription,
                                onToggleSelect = {
                                    selectedTrackIds = if (selectedTrackIds.contains(track.id)) {
                                        selectedTrackIds - track.id
                                    } else {
                                        selectedTrackIds + track.id
                                    }
                                },
                                onLongClick = {
                                    if (!isBatchMode) {
                                        selectedTrackForMenu = track
                                    }
                                },
                                onClick = {
                                    if (isBatchMode) {
                                        selectedTrackIds = if (selectedTrackIds.contains(track.id)) {
                                            selectedTrackIds - track.id
                                        } else {
                                            selectedTrackIds + track.id
                                        }
                                    } else {
                                        onPlayTrackAt(index, track)
                                    }
                                }
                            )
                        }

                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (onRefresh != null) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    content()
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }

    SongLongPressMenu(
        track = selectedTrackForMenu,
        playlists = playlistsForMenu,
        onDismiss = { selectedTrackForMenu = null },
        onRequestLoadPlaylists = onRequestLoadPlaylists,
        onPlayTrack = onPlayTrackFromMenu,
        onAddToQueue = onAddTrackToQueue,
        onAddToPlaylist = onAddSingleToPlaylist,
        onCopyShareLink = onCopyShareLink,
        onRemoveFromCurrent = onRemoveSingleFromCurrent,
        showCopyShareLink = showCopyShareLinkInMenu,
        showRemoveFromCurrent = showRemoveFromCurrentInMenu
    )

    if (showBatchPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showBatchPlaylistPicker = false },
            title = { Text(stringResource(R.string.song_menu_select_playlist)) },
            text = {
                if (playlistsForMenu.isEmpty()) {
                    Text(stringResource(R.string.playlist_empty))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(playlistsForMenu) { playlist ->
                            TextButton(
                                onClick = {
                                    onAddBatchToPlaylist(selectedTrackIds, playlist.id)
                                    showBatchPlaylistPicker = false
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchPlaylistPicker = false }) {
                    Text(stringResource(R.string.main_cancel))
                }
            }
        )
    }
}

@Composable
private fun TrackCollectionHeaderCard(
    trackCount: Int,
    isBatchMode: Boolean,
    onPlayAllClick: () -> Unit,
    onBatchToggleClick: () -> Unit
) {
    Card(
        onClick = onPlayAllClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onPlayAllClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.playlist_detail_play_all),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.playlist_detail_play_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.playlist_detail_track_count, trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onBatchToggleClick) {
                Icon(
                    imageVector = if (isBatchMode) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = if (isBatchMode) {
                        stringResource(R.string.playlist_detail_exit_batch_mode)
                    } else {
                        stringResource(R.string.playlist_detail_batch_mode)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCollectionItemCard(
    track: TrackItem,
    index: Int,
    isBatchMode: Boolean,
    isSelected: Boolean,
    showLikeIndicator: Boolean,
    isTrackLiked: Boolean,
    onSingleRightAction: ((TrackItem) -> Unit)?,
    singleRightActionDescription: String,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isBatchMode && isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBatchMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (track.albumPicUrl != null) {
                    AsyncImage(
                        model = track.albumPicUrl,
                        contentDescription = track.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.main_music_symbol))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium,
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

            if (!isBatchMode) {
                when {
                    onSingleRightAction != null -> {
                        IconButton(onClick = { onSingleRightAction(track) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = singleRightActionDescription,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    showLikeIndicator -> {
                        Icon(
                            imageVector = if (isTrackLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isTrackLiked) {
                                stringResource(R.string.playlist_detail_liked)
                            } else {
                                stringResource(R.string.playlist_detail_unliked)
                            },
                            tint = if (isTrackLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
