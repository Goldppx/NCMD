package com.gem.neteasecloudmd.core.lyrics

data class LyricLine(
    val time: Long,
    val text: String
)

object LyricParser {
    private val timestampRegex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)]")

    fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()

        return buildList {
            lrc.lineSequence().forEach { line ->
                val match = timestampRegex.find(line) ?: return@forEach
                val text = line.substring(match.range.last + 1).trim()
                if (text.isEmpty()) return@forEach

                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                val milliseconds = match.groupValues[3].toLongOrNull() ?: return@forEach
                add(LyricLine(minutes * 60_000L + seconds * 1_000L + milliseconds, text))
            }
        }.sortedBy(LyricLine::time)
    }
}
