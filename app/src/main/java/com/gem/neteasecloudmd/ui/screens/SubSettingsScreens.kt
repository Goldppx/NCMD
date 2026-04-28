package com.gem.neteasecloudmd.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.PlayerManager
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.ui.common.Toast
import com.gem.neteasecloudmd.utils.Logger

// region Shared Components

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubSettingsScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable (innerPadding: androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    description: String = "",
        checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingClickableRow(
    label: String,
    value: String = "",
        onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SelectionAlertDialog(
    title: String,
    items: List<Pair<Int, String>>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                items.forEach { (id, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue == text,
                            onClick = { onSelect(id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.main_cancel))
            }
        }
    )
}

// endregion

// region Playback Settings

@Composable
fun PlaybackSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val playerManager = remember { PlayerManager.getInstance(context) }

    var volumeNormalization by remember { mutableStateOf(sessionManager.isVolumeNormalizationEnabled()) }

    SubSettingsScaffold(
        title = stringResource(R.string.settings_playback),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingSwitch(
                    label = stringResource(R.string.settings_volume_normalization),
                    description = stringResource(R.string.settings_volume_normalization_desc),
                    checked = volumeNormalization,
                    onCheckedChange = {
                        volumeNormalization = it
                        sessionManager.setVolumeNormalizationEnabled(it)
                        playerManager.updateVolumeNormalization(it)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// endregion

// region Storage Settings

@Composable
fun StorageSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var maxCacheSizeMb by remember { mutableStateOf(sessionManager.getMaxCacheSizeMb()) }
    var showSizeLimitPicker by remember { mutableStateOf(false) }

    SubSettingsScaffold(
        title = stringResource(R.string.settings_storage),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingClickableRow(
                    label = stringResource(R.string.settings_cache_size),
                    value = stringResource(R.string.settings_cache_size_value, maxCacheSizeMb),
                    onClick = { showSizeLimitPicker = true }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showSizeLimitPicker) {
        SizeLimitDialog(
            current = maxCacheSizeMb,
            onDismiss = { showSizeLimitPicker = false },
            onConfirm = {
                maxCacheSizeMb = it
                sessionManager.setMaxCacheSizeMb(it)
                showSizeLimitPicker = false
            }
        )
    }
}

@Composable
private fun SizeLimitDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember { mutableFloatStateOf(current.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_size)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_cache_size_value, value.toInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 100f..2000f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.settings_cache_trim_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.toInt()) }) {
                Text(stringResource(R.string.main_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.main_cancel))
            }
        }
    )
}

// endregion

// region Display Settings

@Composable
fun DisplaySettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeModeChanged: (Int) -> Unit,
    onLanguageModeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var themeMode by remember { mutableStateOf(sessionManager.getThemeMode()) }
    var languageMode by remember { mutableStateOf(sessionManager.getLanguageMode()) }
    var enableCoverPalette by remember { mutableStateOf(sessionManager.isCoverPaletteEnabled()) }
    var disableCoverOverflow by remember { mutableStateOf(sessionManager.isCoverOverflowDisabled()) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    SubSettingsScaffold(
        title = stringResource(R.string.settings_display),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                SettingClickableRow(
                    label = stringResource(R.string.settings_section_ui),
                    value = when (themeMode) {
                        SessionManager.THEME_MODE_LIGHT -> stringResource(R.string.settings_theme_light)
                        SessionManager.THEME_MODE_DARK -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingClickableRow(
                    label = stringResource(R.string.settings_language),
                    value = when (languageMode) {
                        SessionManager.LANGUAGE_ZH_CN -> stringResource(R.string.settings_language_zh_cn)
                        SessionManager.LANGUAGE_ZH_TW -> stringResource(R.string.settings_language_zh_tw)
                        SessionManager.LANGUAGE_EN -> stringResource(R.string.settings_language_en)
                        else -> stringResource(R.string.settings_language_system)
                    },
                    onClick = { showLanguageDialog = true }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            item {
                SettingSwitch(
                    label = stringResource(R.string.settings_enable_cover_palette),
                    checked = enableCoverPalette,
                    onCheckedChange = {
                        enableCoverPalette = it
                        sessionManager.setCoverPaletteEnabled(it)
                    }
                )
            }
            item {
                SettingSwitch(
                    label = stringResource(R.string.settings_disable_cover_overflow),
                    checked = disableCoverOverflow,
                    onCheckedChange = {
                        disableCoverOverflow = it
                        sessionManager.setCoverOverflowDisabled(it)
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showThemeDialog) {
        SelectionAlertDialog(
            title = stringResource(R.string.settings_section_ui),
            items = listOf(
                SessionManager.THEME_MODE_SYSTEM to stringResource(R.string.settings_theme_system),
                SessionManager.THEME_MODE_LIGHT to stringResource(R.string.settings_theme_light),
                SessionManager.THEME_MODE_DARK to stringResource(R.string.settings_theme_dark)
            ),
            selectedValue = when (themeMode) {
                SessionManager.THEME_MODE_LIGHT -> stringResource(R.string.settings_theme_light)
                SessionManager.THEME_MODE_DARK -> stringResource(R.string.settings_theme_dark)
                else -> stringResource(R.string.settings_theme_system)
            },
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                themeMode = mode
                sessionManager.setThemeMode(mode)
                onThemeModeChanged(mode)
                showThemeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        SelectionAlertDialog(
            title = stringResource(R.string.settings_language),
            items = listOf(
                SessionManager.LANGUAGE_SYSTEM to stringResource(R.string.settings_language_system),
                SessionManager.LANGUAGE_ZH_CN to stringResource(R.string.settings_language_zh_cn),
                SessionManager.LANGUAGE_ZH_TW to stringResource(R.string.settings_language_zh_tw),
                SessionManager.LANGUAGE_EN to stringResource(R.string.settings_language_en)
            ),
            selectedValue = when (languageMode) {
                SessionManager.LANGUAGE_ZH_CN -> stringResource(R.string.settings_language_zh_cn)
                SessionManager.LANGUAGE_ZH_TW -> stringResource(R.string.settings_language_zh_tw)
                SessionManager.LANGUAGE_EN -> stringResource(R.string.settings_language_en)
                else -> stringResource(R.string.settings_language_system)
            },
            onDismiss = { showLanguageDialog = false },
            onSelect = { mode ->
                val changed = mode != languageMode
                languageMode = mode
                sessionManager.setLanguageMode(mode)
                onLanguageModeChanged(mode)
                showLanguageDialog = false
                if (changed) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_language_switched_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                    (context as? Activity)?.recreate()
                }
            }
        )
    }
}

// endregion

// region Account Settings

@Composable
fun AccountSettingsScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
            val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val isLoggedIn = sessionManager.isLoggedIn()
    val nickname = sessionManager.getNickname()
    val userId = sessionManager.getUserId()
    val cookie = sessionManager.getCookie()
    val avatarUrl = sessionManager.getAvatarUrl()

    SubSettingsScaffold(
        title = stringResource(R.string.settings_account),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.settings_not_logged_in),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.settings_login_prompt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = stringResource(R.string.main_avatar),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(MaterialTheme.shapes.extraLarge),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = nickname,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.main_uid_format, userId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (isLoggedIn) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    Button(
                        onClick = {
                            Logger.i("Settings", "User logged out")
                            sessionManager.logout()
                            Toast.makeText(context, context.getString(R.string.settings_logged_out), Toast.LENGTH_SHORT).show()
                            onLoggedOut()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_logout))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// endregion

// region About Settings

@Composable
fun AboutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    val context = LocalContext.current

    SubSettingsScaffold(
        title = stringResource(R.string.settings_about),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(144.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                Text(
                    text = stringResource(R.string.settings_about_desc_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Goldppx/NCMD"))
                                context.startActivity(intent)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_github),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_github),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_check_update),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_check_update_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToLog)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_view_logs),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToLicenses)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_open_source_licenses),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// endregion

// region Licenses

@Composable
fun LicensesSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val licenses = listOf(
        LicenseInfo("Jetpack Compose", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
        LicenseInfo("Material 3", "Apache 2.0", "https://m3.material.io/"),
        LicenseInfo("Media3 (ExoPlayer)", "Apache 2.0", "https://github.com/androidx/media"),
        LicenseInfo("OkHttp", "Apache 2.0", "https://square.github.io/okhttp/"),
        LicenseInfo("Coil", "Apache 2.0", "https://coil-kt.github.io/coil/"),
        LicenseInfo("Kotlin Serialization", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
        LicenseInfo("Material Kolor", "MIT", "https://github.com/jordond/material-kolor"),
        LicenseInfo("Room", "Apache 2.0", "https://developer.android.com/training/data-storage/room")
    )

    SubSettingsScaffold(
        title = stringResource(R.string.settings_open_source_licenses),
        onNavigateBack = onNavigateBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(licenses) { license ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = license.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${license.license} — ${license.url}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private data class LicenseInfo(
    val name: String,
    val license: String,
    val url: String
)

// endregion
