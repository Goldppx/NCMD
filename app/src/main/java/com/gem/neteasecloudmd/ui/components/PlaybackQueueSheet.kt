package com.gem.neteasecloudmd.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.PlayMode
import com.gem.neteasecloudmd.api.TrackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueSheet(
    player: PlayerManager,
    onDismiss: () -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    likedSongIds: Set<Long> = emptySet(),
    onLikeToggle: ((Long) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        PlaybackQueueContent(
            player = player,
            onDismiss = onDismiss,
            onTrackLongClick = onTrackLongClick,
            likedSongIds = likedSongIds,
            onLikeToggle = onLikeToggle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackQueueContent(
    player: PlayerManager,
    onDismiss: () -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    likedSongIds: Set<Long> = emptySet(),
    onLikeToggle: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxHeight: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.main_queue_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.main_queue_count, player.currentPlaylist.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { player.updatePlayMode(PlayMode.SEQUENTIAL) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (player.playMode == PlayMode.SEQUENTIAL) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (player.playMode == PlayMode.SEQUENTIAL) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(Icons.Default.Repeat, contentDescription = stringResource(R.string.main_play_mode_sequential))
            }
            FilledTonalIconButton(
                onClick = { player.updatePlayMode(PlayMode.SHUFFLE) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (player.playMode == PlayMode.SHUFFLE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (player.playMode == PlayMode.SHUFFLE) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.main_play_mode_shuffle))
            }
            FilledTonalIconButton(
                onClick = { player.updatePlayMode(PlayMode.REPEAT_ONE) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (player.playMode == PlayMode.REPEAT_ONE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (player.playMode == PlayMode.REPEAT_ONE) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Icon(Icons.Default.RepeatOne, contentDescription = stringResource(R.string.main_play_mode_repeat_one))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    player.clearPlaylist()
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.main_clear))
            }
        }

        if (player.currentPlaylist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.main_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (maxHeight) Modifier.heightIn(max = 420.dp) else Modifier.weight(1f)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        )
        {
            itemsIndexed(player.currentPlaylist) { index, track ->
                val isCurrent = index == player.currentTrackIndex
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    player.seekToTrack(index)
                                    player.play()
                                },
                                onLongClick = {
                                    onTrackLongClick(track)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.width(28.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artists,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (onLikeToggle != null) {
                            val isLiked = likedSongIds.contains(track.id)
                            IconButton(onClick = { onLikeToggle(track.id) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isLiked) {
                                        MaterialTheme.colorScheme.primary
                                    } else if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { player.removeTrackAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
