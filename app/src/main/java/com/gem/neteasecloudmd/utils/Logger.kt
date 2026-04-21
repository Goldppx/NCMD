package com.gem.neteasecloudmd.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun format(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return "${sdf.format(Date(timestamp))} [${level.name}] $tag: $message"
    }
}

object Logger {
    private const val TAG = "NCMD"
    private const val LOG_FILE_NAME = "app_logs.txt"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, LOG_FILE_NAME)
        loadLogsFromFile()
        
        // Setup UncaughtExceptionHandler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        i("System", "Logger initialized")
    }

    private fun loadLogsFromFile() {
        val file = logFile ?: return
        if (!file.exists()) return
        try {
            val lines = file.readLines().takeLast(500) // Keep memory lean
            val entries = lines.mapNotNull { parseLogLine(it) }
            _logs.value = entries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load logs", e)
        }
    }

    private fun parseLogLine(line: String): LogEntry? {
        return try {
            val parts = line.split(" ", limit = 4)
            val dateStr = "${parts[0]} ${parts[1]}"
            val levelPart = parts[2].removeSurrounding("[", "]")
            val tagAndMsg = parts[3].split(": ", limit = 2)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val timestamp = sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
            
            LogEntry(
                timestamp = timestamp,
                level = LogLevel.valueOf(levelPart),
                tag = tagAndMsg[0],
                message = tagAndMsg.getOrNull(1) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        log(LogLevel.ERROR, tag, msg)
    }

    @Synchronized
    private fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        
        // Console output
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }

        // Memory update
        val newList = _logs.value.toMutableList().apply {
            add(entry)
            if (size > 1000) removeAt(0)
        }
        _logs.value = newList

        // Persistence
        persistToFile(entry)
    }

    private fun persistToFile(entry: LogEntry) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_FILE_SIZE) {
                file.delete()
            }
            FileOutputStream(file, true).use { fos ->
                fos.write((entry.format() + "\n").toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Persistence failed", e)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        logFile?.delete()
    }

    fun getLogFile(): File? = logFile
}
