package com.gem.neteasecloudmd.desktop

import com.gem.neteasecloudmd.core.library.LibraryStore
import com.gem.neteasecloudmd.core.model.Track
import java.awt.Desktop
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private val supportedAudioExtensions = setOf("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav")

data class ImportResult(
    val addedTrackCount: Int,
    val ignoredEntryCount: Int
)

class DesktopLocalLibrary(private val store: LibraryStore) {
    private val repository = LocalLibraryRepository()
    private var pathsByTrackId: Map<Long, Path> = emptyMap()

    init {
        updateLibrary(repository.loadPaths())
    }

    fun importEntries(entries: List<Path>): ImportResult {
        val scannedEntries = entries.map { entry -> entry to findAudioFiles(entry) }
        val importedPaths = scannedEntries.flatMap { it.second }
        val mergedPaths = (pathsByTrackId.values + importedPaths)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .distinct()
            .sortedBy(Path::toString)
        val previousCount = pathsByTrackId.size

        repository.savePaths(mergedPaths)
        updateLibrary(mergedPaths)
        return ImportResult(
            addedTrackCount = (pathsByTrackId.size - previousCount).coerceAtLeast(0),
            ignoredEntryCount = scannedEntries.count { it.second.isEmpty() }
        )
    }

    fun openInSystemPlayer(trackId: Long): Result<Unit> = runCatching {
        val file = pathsByTrackId[trackId]?.toFile()
            ?: error("The selected track is not a local file.")
        check(file.isFile) { "The selected file no longer exists." }
        check(Desktop.isDesktopSupported()) { "System file opening is unavailable on this desktop." }
        Desktop.getDesktop().open(file)
    }

    private fun updateLibrary(paths: List<Path>) {
        val entries = paths
            .filter { it.isRegularFile() && isSupportedAudioFile(it) }
            .map(::toTrackEntry)
            .distinctBy { it.track.id }
            .sortedBy { it.track.name.lowercase() }
        pathsByTrackId = entries.associate { it.track.id to it.path }
        store.replaceCatalog(entries.map { it.track })
    }

    private fun findAudioFiles(entry: Path): List<Path> = runCatching {
        when {
            entry.isRegularFile() -> listOf(entry).filter(::isSupportedAudioFile)
            entry.isDirectory() -> Files.walk(entry).use { paths ->
                paths.filter { it.isRegularFile() && isSupportedAudioFile(it) }.toList()
            }
            else -> emptyList()
        }
    }.getOrDefault(emptyList())

    private fun toTrackEntry(path: Path): LocalTrackEntry {
        val fileName = path.fileName.toString()
        val titleWithoutExtension = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        val parts = titleWithoutExtension.split(" - ", limit = 2)
        val artist = parts.getOrNull(0)?.takeIf { parts.size == 2 } ?: "Local music"
        val title = parts.getOrElse(if (parts.size == 2) 1 else 0) { titleWithoutExtension }
        val album = path.parent?.name ?: "Local files"
        val normalizedPath = path.toAbsolutePath().normalize()

        return LocalTrackEntry(
            path = normalizedPath,
            track = Track(
                id = stableTrackId(normalizedPath),
                name = title.ifBlank { fileName },
                artists = artist,
                albumName = album,
                albumPicUrl = null
            )
        )
    }

    private fun isSupportedAudioFile(path: Path): Boolean =
        path.fileName.toString().substringAfterLast('.', missingDelimiterValue = "").lowercase() in supportedAudioExtensions

    private fun stableTrackId(path: Path): Long = path.toString().fold(1125899906842597L) { hash, character ->
        hash * 31L + character.code
    }
}

private data class LocalTrackEntry(val path: Path, val track: Track)

private class LocalLibraryRepository {
    private val storageFile: Path = Path.of(
        System.getProperty("user.home"),
        ".ncmd",
        "desktop-library.txt"
    )

    fun loadPaths(): List<Path> = runCatching {
        if (!Files.exists(storageFile)) return emptyList()
        Files.readAllLines(storageFile)
            .mapNotNull(::decodePath)
            .map(Path::of)
            .filter { Files.exists(it) }
    }.getOrDefault(emptyList())

    fun savePaths(paths: List<Path>) {
        Files.createDirectories(requireNotNull(storageFile.parent))
        Files.write(storageFile, paths.map(::encodePath))
    }

    private fun encodePath(path: Path): String = Base64.getEncoder().encodeToString(
        path.toString().toByteArray(StandardCharsets.UTF_8)
    )

    private fun decodePath(value: String): String? = runCatching {
        String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrNull()
}

fun selectAudioFiles(): List<Path> {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Add music files", java.awt.FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    return dialog.files.orEmpty().map(File::toPath)
}

fun selectMusicFolder(): List<Path> {
    val chooser = javax.swing.JFileChooser().apply {
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
    }
    return if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        listOf(chooser.selectedFile.toPath())
    } else {
        emptyList()
    }
}
