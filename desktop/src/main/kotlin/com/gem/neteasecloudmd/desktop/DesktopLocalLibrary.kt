package com.gem.neteasecloudmd.desktop

import com.gem.neteasecloudmd.core.library.LibraryStore
import com.gem.neteasecloudmd.core.model.Track
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

private val supportedAudioExtensions = setOf(
    "aif",
    "aiff",
    "au",
    "flac",
    "mp3",
    "oga",
    "ogg",
    "wav",
    "wave"
)

data class ImportResult(
    val addedTrackCount: Int,
    val ignoredEntryCount: Int
)

class DesktopLocalLibrary(private val store: LibraryStore) {
    private val repository = LocalLibraryRepository()
    private val artworkRepository = LocalArtworkRepository()
    @Volatile
    private var pathsByTrackId: Map<Long, Path> = emptyMap()

    fun loadSavedLibrary() {
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

    fun pathForTrack(trackId: Long): Path? = pathsByTrackId[trackId]

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
        val fallbackArtist = parts.getOrNull(0)?.takeIf { parts.size == 2 } ?: "Local music"
        val fallbackTitle = parts.getOrElse(if (parts.size == 2) 1 else 0) { titleWithoutExtension }
        val fallbackAlbum = path.parent?.name ?: "Local files"
        val normalizedPath = path.toAbsolutePath().normalize()
        val id = stableTrackId(normalizedPath)
        val metadata = readMetadata(normalizedPath, id)

        return LocalTrackEntry(
            path = normalizedPath,
            track = Track(
                id = id,
                name = metadata.title.ifBlank { fallbackTitle.ifBlank { fileName } },
                artists = metadata.artist.ifBlank { fallbackArtist },
                albumName = metadata.album.ifBlank { fallbackAlbum },
                albumPicUrl = metadata.artworkUri,
                duration = metadata.durationMs
            )
        )
    }

    private fun readMetadata(path: Path, trackId: Long): LocalTrackMetadata = runCatching {
        val audioFile = AudioFileIO.read(path.toFile())
        val tag = audioFile.tag
        val artwork = tag?.firstArtwork
        val artworkUri = artwork?.binaryData
            ?.takeIf { it.isNotEmpty() }
            ?.let { artworkRepository.save(trackId, it, artwork.mimeType) }
        LocalTrackMetadata(
            title = tag?.getFirst(FieldKey.TITLE).orEmpty(),
            artist = tag?.getFirst(FieldKey.ARTIST).orEmpty(),
            album = tag?.getFirst(FieldKey.ALBUM).orEmpty(),
            artworkUri = artworkUri,
            durationMs = (audioFile.audioHeader.trackLength * MILLIS_PER_SECOND).coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        )
    }.getOrElse { LocalTrackMetadata() }

    private fun isSupportedAudioFile(path: Path): Boolean =
        path.fileName.toString().substringAfterLast('.', missingDelimiterValue = "").lowercase() in supportedAudioExtensions

    private fun stableTrackId(path: Path): Long = path.toString().fold(1125899906842597L) { hash, character ->
        hash * 31L + character.code
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

private data class LocalTrackEntry(val path: Path, val track: Track)

private data class LocalTrackMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: String? = null,
    val durationMs: Int = 0
)

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

private class LocalArtworkRepository {
    private val artworkDirectory = Path.of(System.getProperty("user.home"), ".ncmd", "artwork")

    fun save(trackId: Long, data: ByteArray, mimeType: String?): String? = runCatching {
        Files.createDirectories(artworkDirectory)
        val extension = when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val artworkPath = artworkDirectory.resolve("$trackId.$extension")
        Files.write(artworkPath, data)
        artworkPath.toUri().toString()
    }.getOrNull()
}

fun selectAudioFiles(): List<Path> = DesktopSystemFilePicker.select(
    title = "Add music files",
    allowMultiple = true,
    selectDirectories = false
)

fun selectMusicFolder(): List<Path> = DesktopSystemFilePicker.select(
    title = "Add music folder",
    allowMultiple = false,
    selectDirectories = true
)

/** AWT FileDialog delegates to the platform file chooser instead of a Swing look-and-feel dialog. */
private object DesktopSystemFilePicker {
    fun select(title: String, allowMultiple: Boolean, selectDirectories: Boolean): List<Path> {
        val directorySelectionProperty = "apple.awt.fileDialogForDirectories"
        val previousDirectorySelection = System.getProperty(directorySelectionProperty)
        return try {
            if (selectDirectories) System.setProperty(directorySelectionProperty, "true")
            val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
            try {
                dialog.isMultipleMode = allowMultiple
                if (!selectDirectories) {
                    dialog.filenameFilter = java.io.FilenameFilter { _, name ->
                        name.substringAfterLast('.', missingDelimiterValue = "").lowercase() in supportedAudioExtensions
                    }
                }
                dialog.isVisible = true
                dialog.files.orEmpty().map(File::toPath)
            } finally {
                dialog.dispose()
            }
        } finally {
            if (previousDirectorySelection == null) {
                System.clearProperty(directorySelectionProperty)
            } else {
                System.setProperty(directorySelectionProperty, previousDirectorySelection)
            }
        }
    }
}
