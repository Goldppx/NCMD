package com.gem.neteasecloudmd.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gem.neteasecloudmd.R
import com.gem.neteasecloudmd.utils.LogLevel
import com.gem.neteasecloudmd.utils.Logger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val logs by Logger.logs.collectAsStateWithLifecycle()
    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, filterLevel) {
        if (filterLevel == null) logs else logs.filter { it.level == filterLevel }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            BadgedBox(badge = {
                                if (filterLevel != null) {
                                    Badge { Text(filterLevel!!.name.take(1)) }
                                }
                            }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Levels") },
                                onClick = {
                                    filterLevel = null
                                    filterMenuExpanded = false
                                }
                            )
                            LogLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level.name) },
                                    onClick = {
                                        filterLevel = level
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        val file = Logger.getLogFile()
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Logs"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { Logger.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredLogs) { entry ->
                LogEntryItem(entry)
            }
        }
    }
}

@Composable
fun LogEntryItem(entry: com.gem.neteasecloudmd.utils.LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> Color.Gray
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> Color(0xFFFBC02D) // Material Yellow 700
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }

    val backgroundColor = when (entry.level) {
        LogLevel.WARN -> Color(0xFFFFF9C4) // Material Yellow 100
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }

    val onBackgroundColor = when (entry.level) {
        LogLevel.WARN -> Color(0xFF5D4037) // Dark Brown
        LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.level.name,
                color = if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) onBackgroundColor else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) Color.Transparent else levelColor, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) {
               Spacer(modifier = Modifier.width(4.dp))
            } else {
               Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) onBackgroundColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR) onBackgroundColor else MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            ),
            color = onBackgroundColor,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (entry.level != LogLevel.WARN && entry.level != LogLevel.ERROR) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
