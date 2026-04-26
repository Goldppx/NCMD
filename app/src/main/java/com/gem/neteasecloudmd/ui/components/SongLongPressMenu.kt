package com.gem.neteasecloudmd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.api.TrackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongLongPressMenu(
    track: TrackItem?,
    playlists: List<PlaylistItem>,
    onDismiss: () -> Unit,
    onRequestLoadPlaylists: () -> Unit,
    onPlayTrack: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onAddToPlaylist: (trackId: Long, playlistId: Long) -> Unit,
    onCopyShareLink: (TrackItem) -> Unit,
    onRemoveFromCurrent: (TrackItem) -> Unit,
    showCopyShareLink: Boolean = true,
    showRemoveFromCurrent: Boolean = true,
    isLiked: Boolean = false,
    onLikeToggle: ((TrackItem) -> Unit)? = null
) {
    if (track == null) return

    var showPlaylistPicker by remember(track.id) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = {
                Text(
                    text = track.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = track.artists,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        )
        TextButton(
            onClick = {
                onPlayTrack(track)
                onDismiss()
            },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.song_menu_play_now),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        TextButton(
            onClick = {
                onAddToQueue(track)
                onDismiss()
            },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.song_menu_add_to_queue),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        if (onLikeToggle != null) {
            TextButton(
                onClick = {
                    onLikeToggle(track)
                    onDismiss()
                },
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (isLiked) stringResource(R.string.playlist_detail_unliked) else stringResource(R.string.playlist_detail_liked),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }

        TextButton(
            onClick = {
                onRequestLoadPlaylists()
                showPlaylistPicker = true
            },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.song_menu_add_to_playlist),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        if (showCopyShareLink) {
            TextButton(
                onClick = {
                    onCopyShareLink(track)
                    onDismiss()
                },
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.song_menu_copy_share_link),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }

        if (showRemoveFromCurrent) {
            TextButton(
                onClick = {
                    onRemoveFromCurrent(track)
                    onDismiss()
                },
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.song_menu_remove_from_current),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text(stringResource(R.string.song_menu_select_playlist)) },
            text = {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.playlist_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(playlists) { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(track.id, playlist.id)
                                    showPlaylistPicker = false
                                    onDismiss()
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
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text(stringResource(R.string.main_cancel))
                }
            }
        )
    }
}
