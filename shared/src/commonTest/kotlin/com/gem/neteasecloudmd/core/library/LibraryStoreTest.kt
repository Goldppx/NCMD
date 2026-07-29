package com.gem.neteasecloudmd.core.library

import com.gem.neteasecloudmd.core.model.Track
import com.gem.neteasecloudmd.core.playback.PlaybackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryStoreTest {
    private val tracks = listOf(
        Track(1, "Graduation", "BrAnTB", "Graduation", null),
        Track(2, "Rain", "SASIOVERLXRD", "Night", null)
    )

    @Test
    fun searchFiltersTheSharedCatalog() {
        val store = LibraryStore(tracks)

        store.navigate(LibraryDestination.SEARCH)
        store.updateQuery("rain")

        assertEquals(listOf(tracks[1]), store.state.value.visibleTracks)
    }

    @Test
    fun selectingTrackUpdatesTheSharedPlaybackState() {
        val store = LibraryStore(tracks)

        store.selectTrack(2)

        assertEquals(1, store.state.value.playback.queue.currentIndex)
        assertTrue(store.state.value.playback.isPlaying)
    }

    @Test
    fun playbackCannotStartWithoutATrack() {
        val store = LibraryStore(emptyList())

        store.togglePlayback()

        assertFalse(store.state.value.playback.isPlaying)
    }

    @Test
    fun replacingCatalogPreservesTheSelectedTrack() {
        val store = LibraryStore(tracks)
        store.selectTrack(2)

        store.replaceCatalog(listOf(tracks[1], tracks[0]))

        assertEquals(0, store.state.value.playback.queue.currentIndex)
        assertTrue(store.state.value.playback.isPlaying)
    }

    @Test
    fun replacingCatalogStopsPlaybackWhenTheTrackWasRemoved() {
        val store = LibraryStore(tracks)
        store.selectTrack(2)

        store.replaceCatalog(listOf(tracks[0]))

        assertFalse(store.state.value.playback.isPlaying)
        assertEquals(0, store.state.value.playback.queue.currentIndex)
    }

    @Test
    fun playbackEngineUpdatesAreReflectedForTheSelectedTrack() {
        val store = LibraryStore(tracks)
        store.selectTrack(tracks.first().id)

        store.updatePlayback(
            status = PlaybackStatus.READY,
            isPlaying = true,
            positionMs = 1_500L,
            durationMs = 12_000L
        )

        val playback = store.state.value.playback
        assertEquals(PlaybackStatus.READY, playback.status)
        assertTrue(playback.isPlaying)
        assertEquals(1_500L, playback.positionMs)
        assertEquals(12_000L, playback.durationMs)
    }
}
