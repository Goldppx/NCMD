package com.gem.neteasecloudmd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlin.math.roundToInt
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.api.rememberPlayerManager

private enum class SettingsSection(val title: String) {
    Account("账号"),
    UI("界面"),
    Feature("功能"),
    Debug("调试")
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeModeChanged: (Int) -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val player = rememberPlayerManager(context)
    val cookie = sessionManager.getCookie()

    var expandedSections by remember { mutableStateOf(emptySet<SettingsSection>()) }
    var disableCoverOverflow by remember { mutableStateOf(sessionManager.isCoverOverflowDisabled()) }
    var themeMode by remember { mutableIntStateOf(sessionManager.getThemeMode()) }
    var useLocalRecentPlays by remember { mutableStateOf(sessionManager.useLocalRecentPlays()) }
    var enableCoverPalette by remember { mutableStateOf(sessionManager.isCoverPaletteEnabled()) }
    var audioBufferMs by remember { mutableIntStateOf(sessionManager.getAudioBufferMs()) }

    fun toggleSection(section: SettingsSection) {
        expandedSections = if (expandedSections.contains(section)) {
            expandedSections - section
        } else {
            expandedSections + section
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SettingsSection.entries) { section ->
                SettingsSectionCard(
                    section = section,
                    expanded = expandedSections.contains(section),
                    onToggle = { toggleSection(section) },
                    disableCoverOverflow = disableCoverOverflow,
                    onDisableCoverOverflowChanged = { disabled ->
                        disableCoverOverflow = disabled
                        sessionManager.setCoverOverflowDisabled(disabled)
                    },
                    useLocalRecentPlays = useLocalRecentPlays,
                    onUseLocalRecentPlaysChanged = { useLocal ->
                        useLocalRecentPlays = useLocal
                        sessionManager.setUseLocalRecentPlays(useLocal)
                    },
                    enableCoverPalette = enableCoverPalette,
                    onEnableCoverPaletteChanged = { enabled ->
                        enableCoverPalette = enabled
                        sessionManager.setCoverPaletteEnabled(enabled)
                    },
                    audioBufferMs = audioBufferMs,
                    onAudioBufferMsChanged = { bufferMs ->
                        audioBufferMs = bufferMs
                        sessionManager.setAudioBufferMs(bufferMs)
                        player.setAudioBufferMs(bufferMs)
                    },
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        sessionManager.setThemeMode(mode)
                        onThemeModeChanged(mode)
                    },
                    onCopyCookieClick = {
                        val latestCookie = sessionManager.getCookie()
                        if (latestCookie.isBlank()) {
                            Toast.makeText(context, "当前没有可复制的 Cookie", Toast.LENGTH_SHORT).show()
                        } else {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("cookie", latestCookie))
                            Toast.makeText(context, "Cookie 已复制", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLogoutClick = {
                        sessionManager.logout()
                        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                        onLoggedOut()
                    },
                    cookie = cookie,
                    userId = sessionManager.getUserId()
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    expanded: Boolean,
    onToggle: () -> Unit,
    disableCoverOverflow: Boolean,
    onDisableCoverOverflowChanged: (Boolean) -> Unit,
    useLocalRecentPlays: Boolean,
    onUseLocalRecentPlaysChanged: (Boolean) -> Unit,
    enableCoverPalette: Boolean,
    onEnableCoverPaletteChanged: (Boolean) -> Unit,
    audioBufferMs: Int,
    onAudioBufferMsChanged: (Int) -> Unit,
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    onCopyCookieClick: () -> Unit,
    onLogoutClick: () -> Unit,
    cookie: String,
    userId: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开"
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (section) {
                    SettingsSection.Account -> {
                        TextButton(onClick = onCopyCookieClick, modifier = Modifier.fillMaxWidth()) {
                            Text("复制 Cookie")
                        }
                        TextButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                            Text("退出登录")
                        }
                    }

                    SettingsSection.UI -> {
                        ThemeModeOption(
                            title = "浅色模式",
                            selected = themeMode == SessionManager.THEME_MODE_LIGHT,
                            onClick = { onThemeModeChanged(SessionManager.THEME_MODE_LIGHT) }
                        )
                        ThemeModeOption(
                            title = "深色模式",
                            selected = themeMode == SessionManager.THEME_MODE_DARK,
                            onClick = { onThemeModeChanged(SessionManager.THEME_MODE_DARK) }
                        )
                        ThemeModeOption(
                            title = "跟随系统",
                            selected = themeMode == SessionManager.THEME_MODE_SYSTEM,
                            onClick = { onThemeModeChanged(SessionManager.THEME_MODE_SYSTEM) }
                        )
                    }

                    SettingsSection.Feature -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "使用本地最近播放记录",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = useLocalRecentPlays,
                                onCheckedChange = onUseLocalRecentPlaysChanged
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "启用歌曲封面取色",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = enableCoverPalette,
                                onCheckedChange = onEnableCoverPaletteChanged
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "禁用封面超出控制栏",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = disableCoverOverflow,
                                onCheckedChange = onDisableCoverOverflowChanged
                            )
                        }

                        Text(
                            text = "音频缓冲: ${audioBufferMs} ms",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = audioBufferMs.toFloat(),
                            onValueChange = { value ->
                                onAudioBufferMsChanged(value.roundToInt())
                            },
                            valueRange = SessionManager.AUDIO_BUFFER_MIN_MS.toFloat()..SessionManager.AUDIO_BUFFER_MAX_MS.toFloat(),
                            steps = ((SessionManager.AUDIO_BUFFER_MAX_MS - SessionManager.AUDIO_BUFFER_MIN_MS) / 20) - 1
                        )
                    }

                    SettingsSection.Debug -> {
                        Text(
                            text = "UID: $userId",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Cookie 长度: ${cookie.length}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
