package com.gem.neteasecloudmd.utils

typealias LyricLine = com.gem.neteasecloudmd.core.lyrics.LyricLine

object LyricParser {
    fun parse(lrc: String?): List<LyricLine> =
        com.gem.neteasecloudmd.core.lyrics.LyricParser.parse(lrc)
}
