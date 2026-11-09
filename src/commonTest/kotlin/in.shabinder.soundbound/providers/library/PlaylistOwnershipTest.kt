package `in`.shabinder.soundbound.providers.library

import `in`.shabinder.soundbound.models.PlaylistOwnership
import `in`.shabinder.soundbound.models.ProviderExceptions
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.providers.source.LibrarySyncSpec
import `in`.shabinder.soundbound.providers.source.PlaylistEditOp
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistOwnershipTest {
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  @Test
  fun oldBundleRowDecodesAsUnknownAndNotEditable() {
    val item = json.decodeFromString(SearchItem.serializer(), """{"title":"t","link":"l","albumArtURL":"","type":"Playlist"}""")
    assertEquals(PlaylistOwnership.UNKNOWN, item.ownership)
    assertFalse(item.editable)
  }

  @Test
  fun editableDefaultsFromOwnershipAndRoundTrips() {
    assertTrue(SearchItem("t", "l", "", SearchItem.Type.Playlist, ownership = PlaylistOwnership.OWNED).editable)
    assertTrue(SearchItem("t", "l", "", SearchItem.Type.Playlist, ownership = PlaylistOwnership.COLLABORATIVE).editable)
    assertFalse(SearchItem("t", "l", "", SearchItem.Type.Playlist, ownership = PlaylistOwnership.FOLLOWED).editable)
    val row = SearchItem("t", "l", "", SearchItem.Type.Playlist, ownership = PlaylistOwnership.FOLLOWED, ownerName = "Spotify")
    val back = json.decodeFromString(SearchItem.serializer(), json.encodeToString(SearchItem.serializer(), row))
    assertEquals(row, back)
    assertEquals("Spotify", back.copy(title = "x").ownerName)
  }

  @Test
  fun editOpsDefaultFollowWritable() {
    assertEquals(emptySet(), LibrarySyncSpec(writable = setOf(LibraryCollections.FAVOURITE_SONGS)).playlistEditOps)
    assertEquals(PlaylistEditOp.entries.toSet() - PlaylistEditOp.FOLLOW, LibrarySyncSpec().playlistEditOps)
    assertEquals(PlaylistEditOp.entries.toSet(), LibrarySyncSpec(followablePlaylists = true).playlistEditOps)
    val addOnly = LibrarySyncSpec(playlistEditOps = setOf(PlaylistEditOp.CREATE, PlaylistEditOp.ADD))
    assertTrue(addOnly.canEditPlaylists(PlaylistEditOp.ADD)); assertFalse(addOnly.canEditPlaylists(PlaylistEditOp.REMOVE))
    // An older host/bundle descriptor without the new fields decodes to the defaults.
    val old = json.decodeFromString(LibrarySyncSpec.serializer(), """{"writable":["playlists"]}""")
    assertFalse(old.followablePlaylists)
    assertTrue(old.canEditPlaylists(PlaylistEditOp.REMOVE))
  }

  @Test
  fun followDefaultThrowsAndSubscriptionErrorIsNamed() = runTest {
    assertFailsWith<UnsupportedOperationException> { object : ServerLibrary {}.setPlaylistFollowed("l", true) }
    val e = ProviderExceptions.SubscriptionRequired("Apple Music")
    assertTrue(e::class.simpleName == "SubscriptionRequired")
    assertTrue(e.message!!.contains("Apple Music library needs an active subscription"))
  }
}
