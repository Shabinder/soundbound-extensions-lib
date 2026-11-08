package `in`.shabinder.soundbound.providers.library

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SearchItem
import kotlinx.serialization.Serializable

/**
 * Optional capability for "bring your own server" sources (Navidrome/OpenSubsonic, Jellyfin, Plex):
 * a connection test, paged browse/search, and the write operations a personal library has
 * (favourite, playlist CRUD, scrobble / now playing).
 *
 * Items are addressed by the same links the provider hands out in [SearchItem.link] /
 * `SongModel.trackURL` (and accepts in `isLinkSupported` / `fetchPlatformQueryResult`), so the app
 * needs no server-specific ids.
 *
 * Backward compatible: extensions built before this existed have none of these members in their
 * Zipline bundle, so the host's call throws `ZiplineApiMismatchException`. Hosts must check
 * [isServerLibraryAvailable] first and treat any failure as "not capable", not as provider ill health.
 * Write methods throw (e.g. [UnsupportedOperationException]) when the server cannot do them.
 */
interface ServerLibrary {
  /** True if the members below are implemented; hosts check this before calling them. */
  val isServerLibraryAvailable: Boolean get() = false

  /** Probe the configured server with the saved config. Never throws: failures are a [ConnectionStatus]. */
  suspend fun testConnection(): ConnectionStatus = ConnectionStatus(ok = false, message = "Not supported")

  /** Free-text search of one [type] (Song, Album, Artist or Playlist), paged. */
  suspend fun searchPage(query: String, type: SearchItem.Type, offset: Int, limit: Int): List<SearchItem> =
    unsupported()

  /**
   * A page of an album / playlist / artist link: the result's trackList holds tracks
   * [offset, offset + limit). For an artist link the tracks are the artist's songs.
   */
  suspend fun fetchPage(link: String, offset: Int, limit: Int): PlatformQueryResult = unsupported()

  /** Albums of an artist link, paged. */
  suspend fun artistAlbums(artistLink: String, offset: Int, limit: Int): List<SearchItem> = unsupported()

  /** The user's playlists. */
  suspend fun playlists(): List<SearchItem> = unsupported()

  /** Favourited (starred) songs, paged. */
  suspend fun favouriteSongs(offset: Int, limit: Int): List<SearchItem> = unsupported()

  /** Favourite or un-favourite a song / album / artist link. */
  suspend fun setFavourite(link: String, favourite: Boolean): Unit = unsupported()

  /** Create a playlist holding [songLinks] (in order); returns the new playlist's link. */
  suspend fun createPlaylist(name: String, songLinks: List<String>): String = unsupported()

  suspend fun addToPlaylist(playlistLink: String, songLinks: List<String>): Unit = unsupported()

  /** Remove the entries at [indices] (0-based positions in the playlist as last read). */
  suspend fun removeFromPlaylist(playlistLink: String, indices: List<Int>): Unit = unsupported()

  /** Move the entry at [from] to position [to] (0-based). */
  suspend fun movePlaylistItem(playlistLink: String, from: Int, to: Int): Unit = unsupported()

  suspend fun deletePlaylist(playlistLink: String): Unit = unsupported()

  /**
   * Playback report for a song link: [PlaybackEvent.STARTED] = "now playing",
   * [PlaybackEvent.FINISHED] = scrobble (played). Best effort; hosts call it off the playback path.
   */
  suspend fun reportPlayback(songLink: String, event: PlaybackEvent, positionMs: Long): Unit = unsupported()
}

private fun unsupported(): Nothing = throw UnsupportedOperationException("Not supported by this source")

@Serializable
enum class PlaybackEvent { STARTED, FINISHED }

@Immutable
@Serializable
data class ConnectionStatus(
  val ok: Boolean,
  /** User-facing reason when [ok] is false (e.g. "Wrong username or password"). */
  val message: String = "",
  val serverName: String? = null,
  val serverVersion: String? = null,
  /** Feature flags the server reported, e.g. OpenSubsonic extension names. */
  val capabilities: List<String> = emptyList(),
)
