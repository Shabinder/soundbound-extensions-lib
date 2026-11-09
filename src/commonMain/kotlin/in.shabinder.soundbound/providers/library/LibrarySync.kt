package `in`.shabinder.soundbound.providers.library

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.SearchItem
import `in`.shabinder.soundbound.models.SongModel
import kotlinx.serialization.Serializable

/**
 * Collection ids for [ServerLibrary.libraryPage], [ServerLibrary.collectionFingerprint] and
 * [ServerLibrary.changesSince]. Any other id is a playlist link (its contents).
 */
object LibraryCollections {
  /** Liked / starred / favourited songs. */
  const val FAVOURITE_SONGS = "favourites"
  /** Songs saved to the library (YouTube Music "Save to library"; servers: not separate from favourites). */
  const val LIBRARY_SONGS = "library:songs"
  const val LIBRARY_ALBUMS = "library:albums"
  const val LIBRARY_ARTISTS = "library:artists"
  /** The user's playlists themselves (one [SearchItem] per playlist). */
  const val PLAYLISTS = "playlists"
}

/**
 * Cheap change marker for one collection. The sync engine stores it after a read and re-reads the
 * collection only when a new fingerprint differs ([sameAs]).
 */
@Immutable
@Serializable
data class CollectionFingerprint(
  /** Item count when the backend reports it cheaply, else null. */
  val count: Int? = null,
  /** Link of the first item in the backend's order (the newest one when [newestFirst]). */
  val newestLink: String? = null,
  /** Opaque version: etag, last-modified, or a hash of the first page. Compare for equality only. */
  val version: String = "",
  /** Items are ordered newest first, so [ServerLibrary.changesSince] can stop at the first known item. */
  val newestFirst: Boolean = false,
  /**
   * True when an equal fingerprint guarantees equal contents (e.g. the whole collection fit in the
   * hashed page). False means heuristic: some edits (reorder or remove+add past the first page with
   * the same count) can leave it unchanged. Engines should still do an occasional full read.
   */
  val exact: Boolean = false,
) {
  fun sameAs(other: CollectionFingerprint?): Boolean =
    other != null && count == other.count && newestLink == other.newestLink && version == other.version
}

/** One page of a collection; pass [nextCursor] back to get the next page, null = last page. */
@Immutable
@Serializable
data class LibraryPage(
  val items: List<SearchItem>,
  val nextCursor: String? = null,
)

/**
 * Result of [ServerLibrary.changesSince].
 * [boundaryFound] true: [added] are exactly the items newer than the first known one, newest first.
 * false: no known item was found, so [added] is the whole collection (treat it as a full read).
 * Removals are not reported: compare the new [CollectionFingerprint.count] with known + added.
 */
@Immutable
@Serializable
data class LibraryChanges(
  val added: List<SearchItem>,
  val boundaryFound: Boolean,
)

@Serializable
enum class PlaylistPrivacy { PRIVATE, UNLISTED, PUBLIC }

/** Stable 64-bit FNV-1a hex of [parts]; the same on every platform (unlike hashCode of collections). */
fun libraryVersionOf(parts: List<String>): String {
  var h = -0x340d631b7bdddcdbL // FNV offset basis 0xcbf29ce484222325
  for (part in parts) {
    for (c in part) {
      h = (h xor c.code.toLong()) * 0x100000001b3L
    }
    h = (h xor 0x1fL) * 0x100000001b3L // separator, so ["ab","c"] != ["a","bc"]
  }
  return h.toULong().toString(16)
}

internal fun SongModel.toLibraryItem() = SearchItem(
  title = title,
  link = trackURL,
  albumArtURL = albumArtURL.orEmpty(),
  type = SearchItem.Type.Song,
  subtitle = artists.joinToString(", ") { it.name },
)
