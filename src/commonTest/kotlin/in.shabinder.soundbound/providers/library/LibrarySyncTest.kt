package `in`.shabinder.soundbound.providers.library

import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.models.SourceModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibrarySyncTest {
  private fun song(i: Int) = SearchItem("s$i", "song:$i", "", SearchItem.Type.Song)

  /** A server that only implements the original ServerLibrary listing, like Navidrome/Jellyfin/Plex today. */
  private class Fake(var favourites: List<SearchItem>, val lists: List<SearchItem> = emptyList()) : ServerLibrary {
    var reads = 0
    override suspend fun favouriteSongs(offset: Int, limit: Int): List<SearchItem> {
      reads++
      return favourites.drop(offset).take(limit)
    }
    override suspend fun playlists() = lists
    override suspend fun fetchPage(link: String, offset: Int, limit: Int) =
      PlatformQueryResult("", "", link, emptyList(), SourceModel("fake", "", true))
  }

  @Test
  fun versionHashIsStableAndSeparatorAware() {
    // Vectors computed independently (Python FNV-1a 64 with a 0x1f separator per part).
    assertEquals("cbf29ce484222325", libraryVersionOf(emptyList()))
    assertEquals("e8bcb18230513c4a", libraryVersionOf(listOf("a", "b")))
    assertEquals("e7200e1905420ecf", libraryVersionOf(listOf("ab")))
  }

  @Test
  fun changesSinceStopsAtFirstKnownItem() = runTest {
    // newest first: 0 is newest. Known head = 5,6 -> only 0..4 are new, and paging stops early.
    val fake = Fake((0 until 1000).map(::song))
    val changes = fake.changesSince(LibraryCollections.FAVOURITE_SONGS, listOf("song:5", "song:6"), pageSize = 3)
    assertTrue(changes.boundaryFound)
    assertEquals((0..4).map { "song:$it" }, changes.added.map { it.link })
    assertEquals(2, fake.reads) // pages [0,1,2] and [3,4,5]; never the other 994
  }

  @Test
  fun changesSinceToleratesRemovedNewestItem() = runTest {
    // The user un-liked the newest known item (song:5): the next known link still anchors.
    val fake = Fake((0 until 10).map(::song).filter { it.link != "song:5" })
    val changes = fake.changesSince(LibraryCollections.FAVOURITE_SONGS, listOf("song:5", "song:6"), pageSize = 4)
    assertTrue(changes.boundaryFound)
    assertEquals((0..4).map { "song:$it" }, changes.added.map { it.link })
  }

  @Test
  fun changesSinceWithoutBoundaryIsAFullRead() = runTest {
    val fake = Fake((0 until 7).map(::song))
    val changes = fake.changesSince(LibraryCollections.FAVOURITE_SONGS, listOf("gone"), pageSize = 3)
    assertFalse(changes.boundaryFound)
    assertEquals(7, changes.added.size)
  }

  @Test
  fun defaultFingerprintsDetectChangeAndAreExactOnlyWhenComplete() = runTest {
    val small = Fake((0 until 3).map(::song), lists = listOf(SearchItem("p", "pl:1", "", SearchItem.Type.Playlist)))
    val before = small.libraryFingerprints()
    val fav = before.getValue(LibraryCollections.FAVOURITE_SONGS)
    assertEquals(3, fav.count)
    assertTrue(fav.exact)
    assertEquals("song:0", fav.newestLink)
    assertEquals(1, before.getValue(LibraryCollections.PLAYLISTS).count)

    small.favourites = listOf(song(9)) + small.favourites
    assertFalse(small.libraryFingerprints().getValue(LibraryCollections.FAVOURITE_SONGS).sameAs(fav))
    assertTrue(fav.sameAs(fav.copy()))

    val big = Fake((0 until 250).map(::song)).libraryFingerprints().getValue(LibraryCollections.FAVOURITE_SONGS)
    assertNull(big.count) // first page is not the whole collection
    assertFalse(big.exact)
  }
}
