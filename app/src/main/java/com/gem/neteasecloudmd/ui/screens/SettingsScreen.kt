package com.gem.neteasecloudmd.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.api.SessionManager

private enum class SettingsSection {
    Account,
    UI,
    Feature,
    Debug
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeModeChanged: (Int) -> Unit,
    onLanguageModeChanged: (Int) -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val sessionManager = remember { SessionManager(context) }
    val cookie = sessionManager.getCookie()

    var expandedSections by remember { mutableStateOf(emptySet<SettingsSection>()) }
    var disableCoverOverflow by remember { mutableStateOf(sessionManager.isCoverOverflowDisabled()) }
    var themeMode by remember { mutableIntStateOf(sessionManager.getThemeMode()) }
    var languageMode by remember { mutableIntStateOf(sessionManager.getLanguageMode()) }
    var useLocalRecentPlays by remember { mutableStateOf(sessionManager.useLocalRecentPlays()) }
    var enableCoverPalette by remember { mutableStateOf(sessionManager.isCoverPaletteEnabled()) }

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
                title = { Text(stringResource(R.string.settings_title)) },
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
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        sessionManager.setThemeMode(mode)
                        onThemeModeChanged(mode)
                    },
                    languageMode = languageMode,
                    onLanguageModeChanged = { mode ->
                        val changed = mode != languageMode
                        languageMode = mode
                        sessionManager.setLanguageMode(mode)
                        onLanguageModeChanged(mode)
                        if (changed) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.settings_language_switched_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                            (context as? Activity)?.recreate()
                        }
                    },
                    onCopyCookieClick = {
                        val latestCookie = sessionManager.getCookie()
                        if (latestCookie.isBlank()) {
                            Toast.makeText(context, resources.getString(R.string.settings_no_cookie), Toast.LENGTH_SHORT).show()
                        } else {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.settings_cookie_key), latestCookie))
                            Toast.makeText(context, resources.getString(R.string.settings_cookie_copied), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLogoutClick = {
                        sessionManager.logout()
                        Toast.makeText(context, resources.getString(R.string.settings_logged_out), Toast.LENGTH_SHORT).show()
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
    themeMode: Int,
    onThemeModeChanged: (Int) -> Unit,
    languageMode: Int,
    onLanguageModeChanged: (Int) -> Unit,
    onCopyCookieClick: () -> Unit,
    onLogoutClick: () -> Unit,
    cookie: String,
    userId: Long
) {
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }

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
                text = when (section) {
                    SettingsSection.Account -> stringResource(R.string.settings_section_account)
                    SettingsSection.UI -> stringResource(R.string.settings_section_ui)
                    SettingsSection.Feature -> stringResource(R.string.settings_section_feature)
                    SettingsSection.Debug -> stringResource(R.string.settings_section_debug)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) stringResource(R.string.common_collapse) else stringResource(R.string.common_expand)
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
                            Text(stringResource(R.string.settings_copy_cookie))
                        }
                        TextButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_logout))
                        }
                    }

                    SettingsSection.UI -> {
                        SelectionDropdown(
                            label = stringResource(R.string.settings_section_ui),
                            value = when (themeMode) {
                                SessionManager.THEME_MODE_LIGHT -> stringResource(R.string.settings_theme_light)
                                SessionManager.THEME_MODE_DARK -> stringResource(R.string.settings_theme_dark)
                                else -> stringResource(R.string.settings_theme_system)
                            },
                            expanded = themeDropdownExpanded,
                            onExpandedChange = { themeDropdownExpanded = it },
                            items = listOf(
                                SessionManager.THEME_MODE_SYSTEM to stringResource(R.string.settings_theme_system),
                                SessionManager.THEME_MODE_LIGHT to stringResource(R.string.settings_theme_light),
                                SessionManager.THEME_MODE_DARK to stringResource(R.string.settings_theme_dark)
                            ),
                            onSelect = {
                                onThemeModeChanged(it)
                                themeDropdownExpanded = false
                            }
                        )

                        SelectionDropdown(
                            label = stringResource(R.string.settings_language),
                            value = when (languageMode) {
                                SessionManager.LANGUAGE_ZH_CN -> stringResource(R.string.settings_language_zh_cn)
                                SessionManager.LANGUAGE_ZH_TW -> stringResource(R.string.settings_language_zh_tw)
                                SessionManager.LANGUAGE_EN -> stringResource(R.string.settings_language_en)
                                else -> stringResource(R.string.settings_language_system)
                            },
                            expanded = languageDropdownExpanded,
                            onExpandedChange = { languageDropdownExpanded = it },
                            items = listOf(
                                SessionManager.LANGUAGE_SYSTEM to stringResource(R.string.settings_language_system),
                                SessionManager.LANGUAGE_ZH_CN to stringResource(R.string.settings_language_zh_cn),
                                SessionManager.LANGUAGE_ZH_TW to stringResource(R.string.settings_language_zh_tw),
                                SessionManager.LANGUAGE_EN to stringResource(R.string.settings_language_en)
                            ),
                            onSelect = {
                                onLanguageModeChanged(it)
                                languageDropdownExpanded = false
                            }
                        )
                    }

                    SettingsSection.Feature -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_use_local_recent),
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
                                text = stringResource(R.string.settings_enable_cover_palette),
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
                                text = stringResource(R.string.settings_disable_cover_overflow),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = disableCoverOverflow,
                                onCheckedChange = onDisableCoverOverflowChanged
                            )
                        }
                    }

                    SettingsSection.Debug -> {
                        Text(
                            text = stringResource(R.string.main_uid_format, userId),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_cookie_length, cookie.length),
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
private fun SelectionDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpandedChange(true) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                IconButton(onClick = { onExpandedChange(true) }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    items.forEach { (id, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = { onSelect(id) }
                        )
                    }
                }
            }
        }
    }
}
