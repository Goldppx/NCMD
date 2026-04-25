package com.gem.neteasecloudmd.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.gem.neteasecloudmd.ui.common.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.SessionManager
import com.gem.neteasecloudmd.utils.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeModeChanged: (Int) -> Unit,
    onLanguageModeChanged: (Int) -> Unit,
    onLoggedOut: () -> Unit,
    onNavigateToLog: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val cookie = sessionManager.getCookie()

    var searchQuery by remember { mutableStateOf("") }
    var disableCoverOverflow by remember { mutableStateOf(sessionManager.isCoverOverflowDisabled()) }
    var themeMode by remember { mutableIntStateOf(sessionManager.getThemeMode()) }
    var languageMode by remember { mutableIntStateOf(sessionManager.getLanguageMode()) }
    var useLocalRecentPlays by remember { mutableStateOf(sessionManager.useLocalRecentPlays()) }
    var enableCoverPalette by remember { mutableStateOf(sessionManager.isCoverPaletteEnabled()) }
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    val sections = listOf(
        SettingsSectionData(
            title = stringResource(R.string.settings_section_account),
            items = listOf(
                SettingsItemData(
                    title = stringResource(R.string.settings_copy_cookie),
                    description = "",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val latestCookie = sessionManager.getCookie()
                        if (latestCookie.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.settings_no_cookie), Toast.LENGTH_SHORT).show()
                        } else {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.settings_cookie_key), latestCookie))
                            Toast.makeText(context, context.getString(R.string.settings_cookie_copied), Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                SettingsItemData(
                    title = stringResource(R.string.settings_logout),
                    description = "",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = {
                        Logger.i("Settings", "User logged out")
                        sessionManager.logout()
                        Toast.makeText(context, context.getString(R.string.settings_logged_out), Toast.LENGTH_SHORT).show()
                        onLoggedOut()
                    }
                )
            )
        ),
        SettingsSectionData(
            title = stringResource(R.string.settings_section_ui),
            items = listOf(
                SettingsItemData(
                    title = stringResource(R.string.settings_section_ui),
                    description = when (themeMode) {
                        SessionManager.THEME_MODE_LIGHT -> stringResource(R.string.settings_theme_light)
                        SessionManager.THEME_MODE_DARK -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    },
                    icon = Icons.Default.Palette,
                    onClick = { themeDropdownExpanded = !themeDropdownExpanded }
                ),
                SettingsItemData(
                    title = stringResource(R.string.settings_language),
                    description = when (languageMode) {
                        SessionManager.LANGUAGE_ZH_CN -> stringResource(R.string.settings_language_zh_cn)
                        SessionManager.LANGUAGE_ZH_TW -> stringResource(R.string.settings_language_zh_tw)
                        SessionManager.LANGUAGE_EN -> stringResource(R.string.settings_language_en)
                        else -> stringResource(R.string.settings_language_system)
                    },
                    icon = Icons.Default.Visibility,
                    onClick = { languageDropdownExpanded = !languageDropdownExpanded }
                )
            )
        ),
        SettingsSectionData(
            title = stringResource(R.string.settings_section_feature),
            items = listOf(
                SettingsItemData(
                    title = stringResource(R.string.settings_use_local_recent),
                    description = "",
                    icon = Icons.Default.Description,
                    onClick = {
                        useLocalRecentPlays = !useLocalRecentPlays
                        sessionManager.setUseLocalRecentPlays(useLocalRecentPlays)
                        Logger.i("Settings", "Use local recent plays: $useLocalRecentPlays")
                    }
                ),
                SettingsItemData(
                    title = stringResource(R.string.settings_enable_cover_palette),
                    description = stringResource(R.string.settings_enable_cover_palette),
                    icon = Icons.Default.Palette,
                    onClick = {
                        enableCoverPalette = !enableCoverPalette
                        sessionManager.setCoverPaletteEnabled(enableCoverPalette)
                        Logger.i("Settings", "Enable cover palette: $enableCoverPalette")
                    }
                ),
                SettingsItemData(
                    title = stringResource(R.string.settings_disable_cover_overflow),
                    description = "",
                    icon = Icons.Default.Tune,
                    onClick = {
                        disableCoverOverflow = !disableCoverOverflow
                        sessionManager.setCoverOverflowDisabled(disableCoverOverflow)
                        Logger.i("Settings", "Disable cover overflow: $disableCoverOverflow")
                    }
                )
            )
        ),
        SettingsSectionData(
            title = stringResource(R.string.settings_section_debug),
            items = listOf(
                SettingsItemData(
                    title = stringResource(R.string.main_uid_format, sessionManager.getUserId()),
                    description = stringResource(R.string.settings_cookie_length, cookie.length),
                    icon = Icons.Default.BugReport,
                    onClick = { }
                ),
                SettingsItemData(
                    title = stringResource(R.string.settings_view_logs),
                    description = "",
                    icon = Icons.Default.Description,
                    onClick = onNavigateToLog
                )
            )
        )
    )

    fun matchesFilter(item: SettingsItemData): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.lowercase()
        return item.title.lowercase().contains(q) || item.description.lowercase().contains(q)
    }

    val filteredSections = sections.mapNotNull { section ->
        val filteredItems = section.items.filter(::matchesFilter)
        if (filteredItems.isNotEmpty()) {
            section.copy(items = filteredItems)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.common_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                filteredSections.forEach { section ->
                    item {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(section.items) { item ->
                        SettingsListItem(item = item)
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (themeDropdownExpanded) {
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
            onDismiss = { themeDropdownExpanded = false },
            onSelect = { mode ->
                Logger.i("Settings", "Theme mode changed: $mode")
                themeMode = mode
                sessionManager.setThemeMode(mode)
                onThemeModeChanged(mode)
                themeDropdownExpanded = false
            }
        )
    }

    if (languageDropdownExpanded) {
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
            onDismiss = { languageDropdownExpanded = false },
            onSelect = { mode ->
                val changed = mode != languageMode
                if (changed) {
                    Logger.i("Settings", "Language mode changed: $mode")
                }
                languageMode = mode
                sessionManager.setLanguageMode(mode)
                onLanguageModeChanged(mode)
                languageDropdownExpanded = false
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

private data class SettingsSectionData(
    val title: String,
    val items: List<SettingsItemData>
)

private data class SettingsItemData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SettingsListItem(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                items.forEach { (id, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(id)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedValue == text,
                            onClick = {
                                onSelect(id)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.main_cancel))
            }
        }
    )
}
