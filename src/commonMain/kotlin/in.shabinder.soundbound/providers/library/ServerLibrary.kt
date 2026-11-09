package `in`.shabinder.soundbound.providers.library

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.PlatformQueryResult
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.models.SongModel
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

  /**
   * The user's playlists (owned, collaborative and followed). Rows carry [SearchItem.ownership] /
   * [SearchItem.editable] when the source knows them.
   */
  suspend fun playlists(): List<SearchItem> = unsupported()

  /** Favourited (starred) songs, paged. */
  suspend fun favouriteSongs(offset: Int, limit: Int): List<SearchItem> = unsupported()

  /** Favourite or un-favourite a song / album / artist link. */
  suspend fun setFavourite(link: String, favourite: Boolean): Unit = unsupported()

  /**
   * Create a playlist holding [songLinks] (in order); returns the new playlist's link.
   * Backends with a privacy setting create it [PlaylistPrivacy.PRIVATE].
   */
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

  // ---------------------------------------------------------------- 0.33: library listing, instances, device login

  /**
   * Every song in the library, paged in a stable order, as full [SongModel]s (ISRC / MBID / favourite flag
   * included) so the host can mirror the library offline and dedupe it against other sources.
   */
  suspend fun songs(offset: Int, limit: Int): List<SongModel> = unsupported()

  /**
   * Turn this loaded copy into instance [instanceId] of a multi-instance source (a second server of the same
   * kind). Called once by the host right after load, before `init`. The provider then reports
   * `source.instanceId = instanceId` and `source.sourceName = SourceModel.instanceSourceName(base, instanceId)`,
   * so config, auth and links (`<scheme>+<instanceId>://...`) never collide with another instance.
   * Empty = the default instance (today's keys and links).
   */
  suspend fun bindInstance(instanceId: String): Unit = unsupported()

  /** True if [startDeviceLogin] works (Jellyfin QuickConnect, Plex PIN). */
  val isDeviceLoginAvailable: Boolean get() = false

  /** Begin a device-code sign-in with the saved server URL. Show [DeviceLogin.code] / [DeviceLogin.verificationUrl]. */
  suspend fun startDeviceLogin(): DeviceLogin = unsupported()

  /**
   * Poll a login started by [startDeviceLogin] every [DeviceLogin.pollIntervalSec]. On [DeviceLoginState.Status.APPROVED]
   * the provider has already saved the resulting token in its own config.
   */
  suspend fun pollDeviceLogin(handle: String): DeviceLoginState = unsupported()

  // ------------------------------------------------------------------ library sync
  // Lets one sync engine treat every backend (YouTube Music, Navidrome, Jellyfin, Plex) alike:
  // cheap fingerprints decide WHICH collections to re-read, changesSince reads only the new head
  // of newest-first collections. Defaults below are built on the paged listing above, so existing
  // providers get working (if not the cheapest) behaviour without changes; override for speed.

  /** Library songs / albums / artists ("Save to library"). Backends without a separate library throw. */
  suspend fun setInLibrary(link: String, inLibrary: Boolean): Unit = unsupported()

  suspend fun renamePlaylist(playlistLink: String, name: String): Unit = unsupported()

  suspend fun setPlaylistPrivacy(playlistLink: String, privacy: PlaylistPrivacy): Unit = unsupported()

  /**
   * Follow (save to library) or unfollow someone else's playlist link. Unfollowing removes it from the user's
   * library without deleting it (for an [in.shabinder.soundbound.models.PlaylistOwnership.OWNED] playlist some
   * backends, e.g. Spotify, treat unfollow as delete: hosts use [deletePlaylist] for owned ones).
   * Declared by `LibrarySyncSpec.followablePlaylists`. Default throws [UnsupportedOperationException].
   */
  suspend fun setPlaylistFollowed(link: String, followed: Boolean): Unit = unsupported()

  /**
   * One page of a collection ([LibraryCollections] id or a playlist link). [cursor] is null for the
   * first page, then the previous page's [LibraryPage.nextCursor].
   * Default: favourites and playlist contents via [favouriteSongs] / [fetchPage] (cursor = offset),
   * [LibraryCollections.PLAYLISTS] via [playlists] in one page; the library:* collections throw.
   */
  suspend fun libraryPage(collection: String, cursor: String?, limit: Int): LibraryPage {
    val offset = cursor?.toIntOrNull() ?: 0
    val items = when (collection) {
      LibraryCollections.PLAYLISTS -> return LibraryPage(playlists())
      LibraryCollections.FAVOURITE_SONGS -> favouriteSongs(offset, limit)
      LibraryCollections.LIBRARY_SONGS, LibraryCollections.LIBRARY_ALBUMS, LibraryCollections.LIBRARY_ARTISTS -> unsupported()
      else -> fetchPage(collection, offset, limit).trackList.map { it.toLibraryItem() }
    }
    return LibraryPage(items, nextCursor = if (items.size < limit) null else (offset + items.size).toString())
  }

  /**
   * Fingerprints of the favourites, the playlist list, and every playlist (keyed by playlist link).
   * Default: one first-page read per collection, hashed ([CollectionFingerprint.exact] only when the
   * page held the whole collection). Override when the backend reports counts / timestamps.
   */
  suspend fun libraryFingerprints(): Map<String, CollectionFingerprint> = buildMap {
    // ponytail: one first-page read per playlist, N+2 requests; backends with counts/etags in their listing override.
    put(LibraryCollections.FAVOURITE_SONGS, firstPageFingerprint(LibraryCollections.FAVOURITE_SONGS))
    val lists = playlists()
    put(LibraryCollections.PLAYLISTS, fingerprintOf(lists, complete = true, titled = true))
    lists.forEach { put(it.link, firstPageFingerprint(it.link)) }
  }

  /**
   * A stronger (first-page hash) fingerprint of one collection, for when [libraryFingerprints] is
   * cheaper but coarser (e.g. YouTube Music derives playlist fingerprints from the listing alone).
   */
  suspend fun collectionFingerprint(collection: String): CollectionFingerprint = firstPageFingerprint(collection)

  /**
   * Items newer than the first of [knownNewestLinks] found, for a newest-first collection, stopping
   * at it instead of reading the rest. Pass a few newest known links, not one, so un-liking the
   * newest item does not force a full read. See [LibraryChanges] for what the result guarantees.
   */
  suspend fun changesSince(collection: String, knownNewestLinks: List<String>, pageSize: Int = 50): LibraryChanges {
    val known = knownNewestLinks.toSet()
    val added = mutableListOf<SearchItem>()
    var cursor: String? = null
    do {
      val page = libraryPage(collection, cursor, pageSize)
      for (item in page.items) {
        if (item.link in known) return LibraryChanges(added, boundaryFound = true)
        added += item
      }
      cursor = page.nextCursor
    } while (cursor != null)
    return LibraryChanges(added, boundaryFound = false)
  }

  /**
   * The link this library would hold for each local song, index-aligned with [tracks]; null when it cannot
   * hold it (the "Not on <source>" list). Batch so ISRC / MBID-keyed libraries (`LibrarySyncSpec.keying`) can
   * look foreign songs up in one go. Default: the song's own link when [isLinkSupported] says it is ours.
   */
  suspend fun matchForLibrary(tracks: List<LibraryTrackRef>): List<String?> =
    tracks.map { t -> t.trackUrl.takeIf { isLinkSupported(it) } }

  /**
   * Does each of [links] (this library's own song links) still exist on the service, index-aligned: true = it does
   * (also when it exists but is restricted), false = gone (deleted / never existed), null = unknown (no check, timeout,
   * transport error). Hosts call it before writing a link that did not come from this library (a link service's or a
   * matcher's answer) and treat null as "try the write". Default: unknown for all.
   */
  suspend fun verifyExists(links: List<String>): List<Boolean?> = links.map { null }

  /**
   * Same contract as `Provider.isLinkSupported` (which re-declares it abstract); declared here so
   * [matchForLibrary]'s default can use it. The default only serves host-side wrappers that are not providers.
   */
  fun isLinkSupported(URL: String): Boolean = false
}

/** A local song as [ServerLibrary.matchForLibrary] sees it. */
@Immutable
@Serializable
data class LibraryTrackRef(
  val trackUrl: String,
  val title: String,
  val artists: List<String>,
  val durationSec: Long,
  val isrc: String? = null,
  val mbid: String? = null,
)

private const val FINGERPRINT_PAGE = 100

private suspend fun ServerLibrary.firstPageFingerprint(collection: String): CollectionFingerprint {
  val page = libraryPage(collection, null, FINGERPRINT_PAGE)
  return fingerprintOf(page.items, complete = page.nextCursor == null)
}

/**
 * Fingerprint of a first page: exact only when [complete] (the page is the whole collection).
 * [titled]: hash titles too. Use it for the playlist list, where a rename on the service changes no link and would
 * otherwise never be pulled.
 */
fun fingerprintOf(items: List<SearchItem>, complete: Boolean, newestFirst: Boolean = false, titled: Boolean = false): CollectionFingerprint =
  CollectionFingerprint(
    count = if (complete) items.size else null,
    newestLink = items.firstOrNull()?.link,
    version = libraryVersionOf(if (titled) items.flatMap { listOf(it.link, it.title) } else items.map { it.link }),
    newestFirst = newestFirst,
    exact = complete,
  )

private fun unsupported(): Nothing = throw UnsupportedOperationException("Not supported by this source")

@Immutable
@Serializable
data class DeviceLogin(
  /** Opaque, passed back to [ServerLibrary.pollDeviceLogin]. */
  val handle: String,
  /** What the user enters / confirms (QuickConnect code, Plex PIN). */
  val code: String,
  /** Where the user approves it; may already carry the code. */
  val verificationUrl: String,
  val pollIntervalSec: Int = 3,
  val expiresInSec: Int = 300,
)

@Immutable
@Serializable
data class DeviceLoginState(val status: Status, val message: String = "") {
  @Serializable
  enum class Status { PENDING, APPROVED, EXPIRED, FAILED }
}

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
