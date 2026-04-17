package com.gem.neteasecloudmd.data.repository

import com.gem.neteasecloudmd.api.TrackItem
import com.gem.neteasecloudmd.data.local.dao.CurrentPlaylistDao
import com.gem.neteasecloudmd.data.local.dao.RecentPlayDao
import com.gem.neteasecloudmd.data.local.entity.CurrentPlaylistEntity
import com.gem.neteasecloudmd.data.local.entity.RecentPlayEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MusicRepositoryTest {

    private lateinit var recentPlayDao: RecentPlayDao
    private lateinit var currentPlaylistDao: CurrentPlaylistDao
    private lateinit var repository: MusicRepository

    @Before
    fun setUp() {
        recentPlayDao = mockk()
        currentPlaylistDao = mockk()
        repository = MusicRepository(recentPlayDao, currentPlaylistDao)
    }

    @Test
    fun getRecentPlays_delegatesToDaoWithDefaultLimit() = runTest {
        val expected = listOf(
            RecentPlayEntity(1L, "Song", "Artist", "pic", 123000, 999L)
        )
        every { recentPlayDao.getRecentPlays(500) } returns flowOf(expected)

        val actual = repository.getRecentPlays().first()

        assertEquals(expected, actual)
        verify(exactly = 1) { recentPlayDao.getRecentPlays(500) }
    }

    @Test
    fun getRecentPlaysPreview_delegatesToDao() = runTest {
        val expected = listOf(
            RecentPlayEntity(2L, "Preview", "Artist", null, 1000, 1000L)
        )
        every { recentPlayDao.getRecentPlaysPreview() } returns flowOf(expected)

        val actual = repository.getRecentPlaysPreview().first()

        assertEquals(expected, actual)
        verify(exactly = 1) { recentPlayDao.getRecentPlaysPreview() }
    }

    @Test
    fun addRecentPlay_insertsMappedEntityAndTrimsWhenOverLimit() = runTest {
        val track = trackItem()
        val inserted = slot<RecentPlayEntity>()
        coEvery { recentPlayDao.insertRecentPlay(capture(inserted)) } just runs
        coEvery { recentPlayDao.getCount() } returns 501
        coEvery { recentPlayDao.trimToLatest(500) } just runs

        val beforeCall = System.currentTimeMillis()
        repository.addRecentPlay(track)
        val afterCall = System.currentTimeMillis()

        assertEquals(track.id, inserted.captured.id)
        assertEquals(track.name, inserted.captured.name)
        assertEquals(track.artists, inserted.captured.artists)
        assertEquals(track.albumPicUrl, inserted.captured.albumPicUrl)
        assertEquals(track.duration, inserted.captured.duration)
        assertTrue(inserted.captured.playedAt in beforeCall..afterCall)
        coVerify(exactly = 1) { recentPlayDao.insertRecentPlay(any()) }
        coVerify(exactly = 1) { recentPlayDao.getCount() }
        coVerify(exactly = 1) { recentPlayDao.trimToLatest(500) }
    }

    @Test
    fun addRecentPlay_doesNotTrimWhenAtOrBelowLimit() = runTest {
        coEvery { recentPlayDao.insertRecentPlay(any()) } just runs
        coEvery { recentPlayDao.getCount() } returns 500

        repository.addRecentPlay(trackItem(id = 2L))

        coVerify(exactly = 1) { recentPlayDao.insertRecentPlay(any()) }
        coVerify(exactly = 1) { recentPlayDao.getCount() }
        coVerify(exactly = 0) { recentPlayDao.trimToLatest(any()) }
    }

    @Test
    fun clearRecentPlays_delegatesToDao() = runTest {
        coEvery { recentPlayDao.clearAll() } just runs

        repository.clearRecentPlays()

        coVerify(exactly = 1) { recentPlayDao.clearAll() }
    }

    @Test
    fun getCurrentPlaylist_delegatesToDao() = runTest {
        val expected = listOf(
            CurrentPlaylistEntity(1L, "Song", "Artist", "pic", 123000, 0)
        )
        every { currentPlaylistDao.getCurrentPlaylist() } returns flowOf(expected)

        val actual = repository.getCurrentPlaylist().first()

        assertEquals(expected, actual)
        verify(exactly = 1) { currentPlaylistDao.getCurrentPlaylist() }
    }

    @Test
    fun saveCurrentPlaylist_clearsInsertsMappedEntitiesAndUpdatesPosition() = runTest {
        val tracks = listOf(
            trackItem(id = 10L, name = "A", artists = "AA", albumPicUrl = "u1", duration = 11),
            trackItem(id = 11L, name = "B", artists = "BB", albumPicUrl = null, duration = 22)
        )
        val inserted = slot<List<CurrentPlaylistEntity>>()
        coEvery { currentPlaylistDao.clearPlaylist() } just runs
        coEvery { currentPlaylistDao.insertPlaylist(capture(inserted)) } just runs
        coEvery { currentPlaylistDao.updatePosition(1) } just runs

        repository.saveCurrentPlaylist(tracks, currentIndex = 1)

        coVerify(exactly = 1) { currentPlaylistDao.clearPlaylist() }
        coVerify(exactly = 1) { currentPlaylistDao.insertPlaylist(any()) }
        coVerify(exactly = 1) { currentPlaylistDao.updatePosition(1) }
        assertEquals(2, inserted.captured.size)
        assertEquals(10L, inserted.captured[0].id)
        assertEquals("A", inserted.captured[0].name)
        assertEquals("AA", inserted.captured[0].artists)
        assertEquals("u1", inserted.captured[0].albumPicUrl)
        assertEquals(11, inserted.captured[0].duration)
        assertEquals(0, inserted.captured[0].position)
        assertEquals(11L, inserted.captured[1].id)
        assertEquals("B", inserted.captured[1].name)
        assertEquals("BB", inserted.captured[1].artists)
        assertNull(inserted.captured[1].albumPicUrl)
        assertEquals(22, inserted.captured[1].duration)
        assertEquals(1, inserted.captured[1].position)
    }

    @Test
    fun updateCurrentPosition_delegatesToDao() = runTest {
        coEvery { currentPlaylistDao.updatePosition(3) } just runs

        repository.updateCurrentPosition(3)

        coVerify(exactly = 1) { currentPlaylistDao.updatePosition(3) }
    }

    @Test
    fun clearCurrentPlaylist_delegatesToDao() = runTest {
        coEvery { currentPlaylistDao.clearPlaylist() } just runs

        repository.clearCurrentPlaylist()

        coVerify(exactly = 1) { currentPlaylistDao.clearPlaylist() }
    }

    private fun trackItem(
        id: Long = 1L,
        name: String = "Track",
        artists: String = "Artist",
        albumPicUrl: String? = "pic",
        duration: Int = 180000
    ): TrackItem {
        return TrackItem(
            id = id,
            name = name,
            artists = artists,
            albumName = "Album",
            albumPicUrl = albumPicUrl,
            duration = duration
        )
    }
}
