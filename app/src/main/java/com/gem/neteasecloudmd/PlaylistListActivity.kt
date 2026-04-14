package com.gem.neteasecloudmd

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.api.NeteaseApiService
import com.gem.neteasecloudmd.api.PlaylistItem
import com.gem.neteasecloudmd.ui.theme.NeteaseCloudMDTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PlaylistListActivity : ComponentActivity() {
    private var refreshKey = 0L
    private var userId: Long = 0
    private var cookie: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getLongExtra("userId", 0)
        cookie = intent.getStringExtra("cookie") ?: ""
        enableEdgeToEdge()
        setContent {
            NeteaseCloudMDTheme {
                PlaylistListScreen(
                    refreshKey = refreshKey,
                    onRefresh = { refreshKey++ },
                    onBackClick = { finish() },
                    userId = userId,
                    cookie = cookie
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    refreshKey: Long = 0,
    onRefresh: () -> Unit = {},
    onBackClick: () -> Unit = {},
    userId: Long,
    cookie: String
) {
    val context = LocalContext.current
    val apiService = remember { NeteaseApiService() }
    val scope = rememberCoroutineScope()
    
    var playlists by remember(refreshKey) { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var isLoading by remember(refreshKey) { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember(refreshKey) { mutableStateOf<String?>(null) }
    
    fun loadPlaylists(showToast: Boolean = false) {
        if (userId > 0 && cookie.isNotEmpty()) {
            scope.launch {
                val result = withTimeoutOrNull(10000L) {
                    apiService.getUserPlaylists(userId, cookie)
                }
                result?.fold(
                    onSuccess = { response ->
                        playlists = response.playlist ?: emptyList()
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "刷新成功，共${playlists.size}个歌单", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                        isRefreshing = false
                        if (showToast) {
                            Toast.makeText(context, "刷新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) ?: run {
                    isLoading = false
                    isRefreshing = false
                    if (showToast) {
                        Toast.makeText(context, "请求超时", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(refreshKey) {
        loadPlaylists()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的歌单") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isRefreshing = true
                        loadPlaylists(showToast = true)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { loadPlaylists(showToast = true) },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null && playlists.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "错误: $errorMessage",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    playlists.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无歌单",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                PlaylistListCard(
                                    playlist = playlist,
                                    onClick = {
                                        val intent = Intent(context, PlaylistDetailActivity::class.java)
                                        intent.putExtra("playlistId", playlist.id)
                                        intent.putExtra("playlistName", playlist.name)
                                        intent.putExtra("cookie", cookie)
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // PlaybackBar removed in single-Activity architecture
        }
    }
}

@Composable
fun PlaylistListCard(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (playlist.coverImgUrl != null) {
                    AsyncImage(
                        model = playlist.coverImgUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text("♪")
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.trackCount}首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}