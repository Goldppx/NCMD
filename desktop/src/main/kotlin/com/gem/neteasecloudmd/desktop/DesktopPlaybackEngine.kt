package com.gem.neteasecloudmd.desktop

import com.gem.neteasecloudmd.core.model.Track
import com.gem.neteasecloudmd.core.playback.PlaybackStatus
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

data class DesktopPlaybackUpdate(
    val status: PlaybackStatus,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val errorMessage: String? = null
)

/**
 * Small, packaged desktop audio engine backed by Java Sound. Java Sound handles WAV itself and
 * gains MP3 decoding from the bundled MP3SPI service provider.
 */
class DesktopPlaybackEngine(
    private val resolvePath: (Long) -> Path?,
    private val onUpdate: (DesktopPlaybackUpdate) -> Unit
) {
    private val lock = Any()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ncmd-desktop-playback").apply { isDaemon = true }
    }

    private var sessionId = 0L
    private var activeTrackId: Long? = null
    private var activeTrackPath: Path? = null
    private var isPaused = false
    private var activeLine: SourceDataLine? = null
    private var currentPositionMs = 0L
    private var currentDurationMs = 0L

    fun play(track: Track) {
        val path = resolvePath(track.id)
        if (path == null) {
            onUpdate(
                DesktopPlaybackUpdate(
                    status = PlaybackStatus.ERROR,
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    errorMessage = "The selected track is not available locally."
                )
            )
            return
        }

        var resumeUpdate: DesktopPlaybackUpdate? = null
        synchronized(lock) {
            if (activeTrackId == track.id && isPaused) {
                isPaused = false
                activeLine?.start()
                resumeUpdate = DesktopPlaybackUpdate(
                    status = PlaybackStatus.READY,
                    isPlaying = true,
                    positionMs = currentPositionMs,
                    durationMs = currentDurationMs
                )
                notifyPlaybackThread()
            } else {
                sessionId += 1L
                activeTrackId = track.id
                activeTrackPath = path
                isPaused = false
                currentPositionMs = 0L
                currentDurationMs = track.duration.toLong().coerceAtLeast(0L)
                activeLine?.close()
                val newSessionId = sessionId
                val expectedDurationMs = currentDurationMs
                executor.execute { playSession(newSessionId, track.id, path, expectedDurationMs = expectedDurationMs) }
            }
        }
        resumeUpdate?.let(onUpdate)
    }

    fun pause() {
        val update: DesktopPlaybackUpdate
        synchronized(lock) {
            if (activeTrackId == null || isPaused) return
            isPaused = true
            activeLine?.stop()
            update = DesktopPlaybackUpdate(
                status = PlaybackStatus.READY,
                isPlaying = false,
                positionMs = currentPositionMs,
                durationMs = currentDurationMs
            )
        }
        onUpdate(update)
    }

    fun seekTo(positionMs: Long) {
        synchronized(lock) {
            val trackId = activeTrackId ?: return
            val path = activeTrackPath ?: return
            val targetPositionMs = positionMs.coerceIn(0L, currentDurationMs)
            sessionId += 1L
            currentPositionMs = targetPositionMs
            activeLine?.close()
            val newSessionId = sessionId
            val expectedDurationMs = currentDurationMs
            executor.execute {
                playSession(
                    currentSessionId = newSessionId,
                    trackId = trackId,
                    path = path,
                    initialPositionMs = targetPositionMs,
                    expectedDurationMs = expectedDurationMs
                )
            }
        }
    }

    fun release() {
        synchronized(lock) {
            sessionId += 1L
            activeTrackId = null
            activeTrackPath = null
            isPaused = false
            activeLine?.close()
            activeLine = null
            notifyPlaybackThread()
        }
        executor.shutdownNow()
    }

    private fun playSession(
        currentSessionId: Long,
        trackId: Long,
        path: Path,
        initialPositionMs: Long = 0L,
        expectedDurationMs: Long = 0L
    ) {
        var durationMs = 0L
        var positionMs = initialPositionMs
        try {
            openDecodedStream(path).use { stream ->
                durationMs = stream.durationMs().takeIf { it > 0L } ?: expectedDurationMs
                val startPositionMs = initialPositionMs.coerceIn(0L, durationMs.takeIf { it > 0L } ?: initialPositionMs)
                stream.skipToPosition(startPositionMs)
                synchronized(lock) {
                    currentDurationMs = durationMs
                    currentPositionMs = startPositionMs
                }
                val line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, stream.format)) as SourceDataLine
                line.use {
                    synchronized(lock) {
                        if (!isCurrentSession(currentSessionId, trackId)) return
                        activeLine = line
                    }
                    line.open(stream.format)
                    val playing = synchronized(lock) { !isPaused }
                    if (playing) line.start()
                    emitIfCurrent(currentSessionId, trackId, PlaybackStatus.READY, playing, startPositionMs, durationMs)

                    val buffer = ByteArray(BUFFER_SIZE)
                    var lastReportedAt = 0L
                    while (true) {
                        synchronized(lock) {
                            while (isPaused && isCurrentSession(currentSessionId, trackId)) {
                                val currentPosition = startPositionMs + line.positionMs(stream.format)
                                emitIfCurrent(
                                    currentSessionId,
                                    trackId,
                                    PlaybackStatus.READY,
                                    false,
                                    currentPosition,
                                    durationMs
                                )
                                awaitResume()
                            }
                            if (!isCurrentSession(currentSessionId, trackId)) return
                        }

                        val bytesRead = stream.read(buffer)
                        if (bytesRead < 0) break
                        line.write(buffer, 0, bytesRead)
                        positionMs = startPositionMs + line.positionMs(stream.format)
                        synchronized(lock) { currentPositionMs = positionMs }
                        val now = System.nanoTime()
                        if (now - lastReportedAt >= PROGRESS_INTERVAL_NANOS) {
                            lastReportedAt = now
                            emitIfCurrent(currentSessionId, trackId, PlaybackStatus.READY, true, positionMs, durationMs)
                        }
                    }
                    line.drain()
                    positionMs = if (durationMs > 0L) durationMs else startPositionMs + line.positionMs(stream.format)
                }
            }
            emitIfCurrent(currentSessionId, trackId, PlaybackStatus.READY, false, positionMs, durationMs)
        } catch (error: Exception) {
            emitIfCurrent(
                currentSessionId,
                trackId,
                PlaybackStatus.ERROR,
                false,
                positionMs,
                durationMs,
                error.message ?: "Unable to play this audio file."
            )
        } finally {
            synchronized(lock) {
                if (isCurrentSession(currentSessionId, trackId)) {
                    activeLine = null
                    activeTrackId = null
                    activeTrackPath = null
                    isPaused = false
                }
            }
        }
    }

    private fun openDecodedStream(path: Path): AudioInputStream {
        val source = AudioSystem.getAudioInputStream(path.toFile())
        val sourceFormat = source.format
        if (sourceFormat.encoding == AudioFormat.Encoding.PCM_SIGNED) return source

        val decodedFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.sampleRate,
            PCM_SAMPLE_SIZE_BITS,
            sourceFormat.channels,
            sourceFormat.channels * PCM_SAMPLE_SIZE_BITS / Byte.SIZE_BITS,
            sourceFormat.sampleRate,
            false
        )
        return AudioSystem.getAudioInputStream(decodedFormat, source)
    }

    private fun AudioInputStream.durationMs(): Long {
        if (frameLength == AudioSystem.NOT_SPECIFIED.toLong() || format.frameRate <= 0f) return 0L
        return (frameLength * MILLIS_PER_SECOND / format.frameRate).toLong()
    }

    private fun AudioInputStream.skipToPosition(positionMs: Long) {
        if (positionMs <= 0L || format.frameRate <= 0f || format.frameSize <= 0) return
        var remainingBytes = (positionMs * format.frameRate / MILLIS_PER_SECOND).toLong() * format.frameSize
        while (remainingBytes > 0L) {
            val skipped = skip(remainingBytes)
            if (skipped <= 0L) break
            remainingBytes -= skipped
        }
    }

    private fun SourceDataLine.positionMs(format: AudioFormat): Long {
        if (format.frameRate <= 0f) return 0L
        return (longFramePosition * MILLIS_PER_SECOND / format.frameRate).toLong()
    }

    private fun emitIfCurrent(
        currentSessionId: Long,
        trackId: Long,
        status: PlaybackStatus,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        errorMessage: String? = null
    ) {
        synchronized(lock) {
            if (!isCurrentSession(currentSessionId, trackId)) return
        }
        onUpdate(DesktopPlaybackUpdate(status, isPlaying, positionMs, durationMs, errorMessage))
    }

    private fun isCurrentSession(currentSessionId: Long, trackId: Long): Boolean =
        sessionId == currentSessionId && activeTrackId == trackId

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun awaitResume() {
        (lock as Object).wait()
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun notifyPlaybackThread() {
        (lock as Object).notifyAll()
    }

    private companion object {
        const val BUFFER_SIZE = 16 * 1024
        const val PCM_SAMPLE_SIZE_BITS = 16
        const val MILLIS_PER_SECOND = 1_000L
        const val PROGRESS_INTERVAL_NANOS = 200_000_000L
    }
}
