package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable


import kotlinx.serialization.Serializable


/**
 * A search / browse / library row.
 *
 * Library playlist rows (`ServerLibrary.libraryPage(LibraryCollections.PLAYLISTS, ...)`) also say who owns the
 * playlist: [ownership], [editable] and [ownerName]. Every other row (and every bundle built before these existed)
 * leaves them at their defaults ([PlaylistOwnership.UNKNOWN], not editable), so hosts must treat UNKNOWN as
 * "do not push edits unless the source's `LibrarySyncSpec.writable` says playlists are writable".
 */
@Immutable
@Serializable
open class SearchItem(
    val title: String,
    val link: String,
    val albumArtURL: String,
    val type: Type = Type.Song,
    val subtitle: String = type.name,
    /** Playlist rows: whose playlist this is. [PlaylistOwnership.UNKNOWN] for non-playlists and older bundles. */
    val ownership: PlaylistOwnership = PlaylistOwnership.UNKNOWN,
    /**
     * True when the signed-in user may change this playlist's contents (add / remove / move / rename).
     * Defaults to [PlaylistOwnership.OWNED] or [PlaylistOwnership.COLLABORATIVE].
     */
    val editable: Boolean = ownership == PlaylistOwnership.OWNED || ownership == PlaylistOwnership.COLLABORATIVE,
    /** Owner's display name when the source reports it ("Spotify", a friend's name); null when unknown. */
    val ownerName: String? = null,
) {

    @Immutable
    @Serializable
    enum class Type {
        Song, Album, Playlist, Artist, AutoComplete, All // All doesn't include autocomplete
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchItem) return false

        if (link != other.link) return false
        if (type != other.type) return false
        if (title != other.title) return false
        if (albumArtURL != other.albumArtURL) return false
        if (subtitle != other.subtitle) return false
        if (ownership != other.ownership) return false
        if (editable != other.editable) return false
        if (ownerName != other.ownerName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = link.hashCode()
        result = 31 * result + albumArtURL.hashCode()
        result = 31 * result + (type.hashCode())
        result = 31 * result + title.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + ownership.hashCode()
        result = 31 * result + editable.hashCode()
        result = 31 * result + (ownerName?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SearchItem(title='$title' ,link='$link', albumArtURL='$albumArtURL', type='$type', subtitle='$subtitle', ownership=$ownership, editable=$editable)"
    }

    fun copy(
        title: String = this.title,
        link: String = this.link,
        albumArtURL: String = this.albumArtURL,
        type: Type = this.type,
        subtitle: String = this.subtitle,
        ownership: PlaylistOwnership = this.ownership,
        editable: Boolean = this.editable,
        ownerName: String? = this.ownerName,
    ): SearchItem {
        return SearchItem(title, link, albumArtURL, type, subtitle, ownership, editable, ownerName)
    }
}

/**
 * Who a library playlist belongs to, from the signed-in user's point of view.
 *
 * - [OWNED]: the user created it; every edit is allowed.
 * - [COLLABORATIVE]: someone else's playlist the user may edit (Spotify collaborative playlists).
 * - [FOLLOWED]: another user's / curator's playlist saved to the library; read-only, can be unfollowed
 *   (`ServerLibrary.setPlaylistFollowed`).
 * - [PLATFORM_GENERATED]: made by the service for the user (Liked Music, Episodes, Radio / Daily Mix,
 *   "Made for you", editorial); read-only.
 * - [UNKNOWN]: the source did not say (non-playlist rows, bundles built before this existed).
 */
@Serializable
enum class PlaylistOwnership { OWNED, COLLABORATIVE, FOLLOWED, PLATFORM_GENERATED, UNKNOWN }
