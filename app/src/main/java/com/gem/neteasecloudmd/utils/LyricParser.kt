package com.gem.neteasecloudmd.utils

data class LyricLine(
    val time: Long,
    val text: String
)

object LyricParser {
    fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        
        val lines = lrc.split("\n")
        val lyricLines = mutableListOf<LyricLine>()
        
        val timeRegex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\]")
        
        for (line in lines) {
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong()
                
                val time = min * 60_000 + sec * 1_000 + ms
                val text = line.substring(match.range.last + 1).trim()
                
                if (text.isNotEmpty()) {
                    lyricLines.add(LyricLine(time, text))
                }
            }
        }
        
        return lyricLines.sortedBy { it.time }
    }
}
