package `in`.shabinder.soundbound.providers.source

import androidx.compose.runtime.Immutable
import `in`.shabinder.soundbound.models.SourceModel
import `in`.shabinder.soundbound.providers.DownloadableProvider
import `in`.shabinder.soundbound.providers.Provider
import `in`.shabinder.soundbound.providers.QueryableProvider
import `in`.shabinder.soundbound.providers.auth.AuthHandler
import `in`.shabinder.soundbound.providers.library.LibraryCollections
import kotlinx.serialization.Serializable

/**
 * What a source is and what it can do, declared by the extension so the app never names a source.
 * Every label, icon, sync row, library chip, search tile and identity rule the app shows for a source
 * comes from here (see SoundBound `Docs/PLANS/source-agnostic-extensions.md`).
 *
 * Every field has a default, so adding one never breaks an older bundle or host. Static by intent: the
 * dynamic state (signed in, server reachable) stays on the existing probes (`isAuthenticated`,
 * `isServerLibraryAvailable`, `testConnection`).
 */
@Immutable
@Serializable
data class SourceDescriptor(
  /** "YouTube Music", "Navidrome", "ListenBrainz". */
  val displayName: String,
  /** For chips and badges, e.g. "YTM". */
  val shortName: String = displayName,
  /** Square icon, `https://soundbound.app/assets/ic_<name>.svg`. */
  val iconUrl: String? = null,
  /** ARGB. The UI may only tint with it (the app's own brand stays black / transparent). */
  val brandColor: Long? = null,
  val capabilities: Set<SourceCapability> = emptySet(),
  /** Non-null iff [SourceCapability.LIBRARY_SYNC]. */
  val library: LibrarySyncSpec? = null,
  /** Non-null iff [SourceCapability.ACCOUNT_LOGIN]: which sign-in UI the app opens. */
  val login: LoginKind? = null,
  val syncDefaults: SyncDefaults = SyncDefaults(),
  val identity: IdentitySpec = IdentitySpec(),
  val search: SearchDefaults = SearchDefaults(),
  /** Self-hosted and multi-instance (one loaded copy per configured server). */
  val isServer: Boolean = false,
  /** Per-instance label for a server: its configured name, else "<kind> · <host>". */
  val instanceLabel: String? = null,
  /** Link prefixes this loaded copy owns (`navidrome://`, `navidrome+2://`, `https://music.youtube.com/`). */
  val linkPrefixes: List<String> = emptyList(),
) {
  /** The label the app shows for this loaded copy. */
  val label: String get() = instanceLabel ?: displayName

  fun has(capability: SourceCapability): Boolean = capability in capabilities
}

@Serializable
enum class SourceCapability {
  CATALOGUE, PLAYBACK, DOWNLOAD, LYRICS, RECOGNITION, ISRC_LOOKUP,
  LIBRARY_SYNC, SCROBBLE, PRESENCE, STATS, ACCOUNT_LOGIN, SERVER_INSTANCE,
}

/**
 * Which sign-in the app opens. All map onto the existing auth members:
 * COOKIE = `CookieAuthAvailable`; WEBVIEW_JS = `CookieAuthAvailable` with `localStorageKeys`;
 * DEVICE_CODE = `ServerLibrary.startDeviceLogin`; TOKEN = a `ConfigInputType.SECRET` config key.
 */
@Serializable
enum class LoginKind { COOKIE, WEBVIEW_JS, TOKEN, DEVICE_CODE }

/** How the extension's library is keyed and what the sync engine may read and write. */
@Immutable
@Serializable
data class LibrarySyncSpec(
  /** [LibraryCollections] ids the engine may read. */
  val readable: Set<String> = setOf(LibraryCollections.FAVOURITE_SONGS, LibraryCollections.PLAYLISTS),
  /**
   * [LibraryCollections] ids the engine may write. Declare only writes that work.
   * [LibraryCollections.PLAYLISTS] here means the user's OWNED / COLLABORATIVE playlists are editable (with the
   * ops in [playlistEditOps]); FOLLOWED and PLATFORM_GENERATED ones (see `SearchItem.ownership`) never are.
   */
  val writable: Set<String> = readable,
  /** True when `libraryFingerprints()` are exact, so the engine may skip periodic full reads. */
  val exactFingerprints: Boolean = false,
  /** LINK: a remote song is its own link. ISRC / MBID: foreign songs are matched by `ServerLibrary.matchForLibrary`. */
  val keying: Keying = Keying.LINK,
  /** Regexes over playlist links the user cannot edit (auto mixes, system lists): pulled read-only, never pushed. */
  val readOnlyPlaylists: List<String> = emptyList(),
  /** `ServerLibrary.setPlaylistFollowed` works: the engine may follow / unfollow other users' playlists. */
  val followablePlaylists: Boolean = false,
  /**
   * Which playlist edits work when [LibraryCollections.PLAYLISTS] is [writable]. Default: every op when playlists
   * are writable ([PlaylistEditOp.FOLLOW] only with [followablePlaylists]), none otherwise. Add-only backends
   * (Apple Music public API) declare `setOf(CREATE, ADD)`.
   */
  val playlistEditOps: Set<PlaylistEditOp> = defaultPlaylistEditOps(writable, followablePlaylists),
) {
  @Serializable
  enum class Keying { LINK, ISRC, MBID }

  fun canEditPlaylists(op: PlaylistEditOp): Boolean = op in playlistEditOps
}

/**
 * One playlist write, mapped onto ServerLibrary: CREATE = `createPlaylist`, ADD = `addToPlaylist`,
 * REMOVE = `removeFromPlaylist`, MOVE = `movePlaylistItem`, RENAME = `renamePlaylist`, DELETE = `deletePlaylist`,
 * FOLLOW = `setPlaylistFollowed`.
 */
@Serializable
enum class PlaylistEditOp { CREATE, ADD, REMOVE, MOVE, RENAME, DELETE, FOLLOW }

/** [LibrarySyncSpec.playlistEditOps]'s default. */
fun defaultPlaylistEditOps(writable: Set<String>, followablePlaylists: Boolean): Set<PlaylistEditOp> =
  if (LibraryCollections.PLAYLISTS !in writable) {
    if (followablePlaylists) setOf(PlaylistEditOp.FOLLOW) else emptySet()
  } else {
    PlaylistEditOp.entries.filter { it != PlaylistEditOp.FOLLOW || followablePlaylists }.toSet()
  }

@Immutable
@Serializable
data class SyncDefaults(
  /** On by default once signed in, and the source of the Home sign-in nudge. At most one shipped extension sets it. */
  val defaultTarget: Boolean = false,
  val privatePlaylistsSupported: Boolean = false,
  /** Likes are visible to others: the app shows a "public" warning and makes syncing them opt-in. */
  val likesPublic: Boolean = false,
)

@Immutable
@Serializable
data class IdentitySpec(
  /** Rank as an Odesli input, lower is better. Null = weak input: swap it for a store URL first. */
  val odesliInputRank: Int? = null,
  /** Searched in parallel by the ISRC waterfall / store search. */
  val catalogStore: Boolean = false,
  /** Duration tolerance when matching this source's tracks, seconds. */
  val durationSlackSec: Int = 3,
  /** Pure rewrite that makes equal recordings equal keys without a Zipline call. */
  val canonicalLink: LinkRule? = null,
)

/** `Regex(pattern).replace(link, replacement)`; `$1` refers to a group. */
@Immutable
@Serializable
data class LinkRule(val pattern: String, val replacement: String)

@Immutable
@Serializable
data class SearchDefaults(
  /** Never gated by source policy and always has a search tile. */
  val ungated: Boolean = false,
  /** Selected in search on a fresh install. */
  val enabledByDefault: Boolean = false,
  /** Tile order among remote sources, lower first. Local is always first (app-owned). */
  val tileRank: Int = 100,
  /** No user chip: rides along only while every chip is on. */
  val chipless: Boolean = false,
)

/**
 * A plain descriptor from facts every bundle already exposes. Used by [defaultDescriptor] inside the bundle,
 * and by the host loader for bundles built before [Provider.descriptor] existed.
 */
fun fallbackDescriptor(
  sourceName: String,
  iconUrl: String?,
  capabilities: Set<SourceCapability>,
  login: LoginKind? = null,
): SourceDescriptor = SourceDescriptor(
  displayName = SourceModel.baseSourceName(sourceName).substringAfterLast('.'),
  iconUrl = iconUrl,
  capabilities = capabilities,
  library = if (SourceCapability.LIBRARY_SYNC in capabilities) LibrarySyncSpec() else null,
  login = login.takeIf { SourceCapability.ACCOUNT_LOGIN in capabilities },
)

/** The kind of sign-in [authMethod] (plus device login) implies, or null for none. */
fun loginKindOf(authMethod: AuthHandler.AuthMethod, deviceLogin: Boolean): LoginKind? = when {
  authMethod is AuthHandler.AuthMethod.CookieAuthAvailable ->
    if (authMethod.localStorageKeys.isNotEmpty()) LoginKind.WEBVIEW_JS else LoginKind.COOKIE
  deviceLogin -> LoginKind.DEVICE_CODE
  else -> null
}

/** Descriptor derived from the provider's existing members. Extensions start from it and `copy()` what they know. */
fun Provider.defaultDescriptor(): SourceDescriptor {
  val login = loginKindOf(authMethodType, isDeviceLoginAvailable)
  val caps = buildSet {
    if (isCatalogueAvailable) add(SourceCapability.CATALOGUE)
    if (this@defaultDescriptor is DownloadableProvider) { add(SourceCapability.PLAYBACK); add(SourceCapability.DOWNLOAD) }
    if (isLyricsAvailable) add(SourceCapability.LYRICS)
    if (isSongRecognitionAvailable) add(SourceCapability.RECOGNITION)
    if ((this@defaultDescriptor as? QueryableProvider)?.isISRCSupported == true) add(SourceCapability.ISRC_LOOKUP)
    if (isServerLibraryAvailable) add(SourceCapability.LIBRARY_SYNC)
    if (isScrobbleAvailable) add(SourceCapability.SCROBBLE)
    if (isPresenceAvailable) add(SourceCapability.PRESENCE)
    if (isStatsAvailable) add(SourceCapability.STATS)
    if (login != null) add(SourceCapability.ACCOUNT_LOGIN)
  }
  return fallbackDescriptor(source.sourceName, sourceLogoURL, caps, login)
}
