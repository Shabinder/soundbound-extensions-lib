package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Represents metadata for a track resolved from a music platform URL.
 * Contains cross-platform links for the same track on different services.
 *
 * This is the response model from MetadataProvider implementations.
 */
@Immutable
@Serializable
data class TrackMetadata(
  /**
   * Track title
   */
  val title: String,

  /**
   * Track duration in milliseconds, null if unknown
   */
  val durationMs: Long? = null,

  /**
   * List of artists for this track
   */
  val artists: List<Artist> = emptyList(),

  /**
   * Album name, null if single or unknown
   */
  val albumName: String? = null,

  /**
   * URL to thumbnail/cover art image
   */
  val thumbnailUrl: String? = null,

  /**
   * International Standard Recording Code (ISRC)
   * Unique identifier for the recording across platforms
   */
  val isrc: String? = null,

  /**
   * Release year
   */
  val year: Int? = null,

  /**
   * Track number in album
   */
  val trackNumber: Int? = null,

  /**
   * Genre
   */
  val genre: String? = null,

  /**
   * Cross-platform links for this track
   */
  val platformLinks: List<PlatformLink> = emptyList(),

  /**
   * URL to the song.link/odesli page for this track
   */
  val pageUrl: String? = null,
) {
  /**
   * Get the platform link for a specific platform.
   */
  fun getLinkForPlatform(platform: MusicPlatform): PlatformLink? {
    return platformLinks.find { it.platform == platform }
  }

  /**
   * Get all platform links excluding the specified platform.
   * Useful when you have a track from one platform and want links to others.
   */
  fun getLinksExcluding(platform: MusicPlatform): List<PlatformLink> {
    return platformLinks.filter { it.platform != platform }
  }

  /**
   * Get the source platform (the first platform in the links, typically the original URL's platform).
   */
  val sourcePlatform: MusicPlatform?
    get() = platformLinks.firstOrNull()?.platform

  /**
   * Artists as comma-separated string
   */
  val artistsString: String
    get() = artists.joinToString(", ") { it.name }
}

/**
 * Represents a link to a track on a specific music streaming platform.
 */
@Immutable
@Serializable
data class PlatformLink(
  /**
   * The music platform this link is for
   */
  val platform: MusicPlatform,

  /**
   * Web URL to the track on this platform
   */
  val url: String,

  /**
   * Native app URI for mobile (e.g., "spotify:track:xxx")
   */
  val nativeAppUriMobile: String? = null,

  /**
   * Native app URI for desktop
   */
  val nativeAppUriDesktop: String? = null,

  /**
   * Country/region code this link is valid for (e.g., "US", "IN")
   */
  val country: String? = null,

  /**
   * Platform-specific unique ID for this track
   */
  val trackId: String? = null,
) {
  /**
   * Get the best URI for opening the track.
   * Prefers native app URI on mobile, falls back to web URL.
   */
  fun getBestUri(preferNativeApp: Boolean = true): String {
    return if (preferNativeApp) {
      nativeAppUriMobile ?: nativeAppUriDesktop ?: url
    } else {
      url
    }
  }
}

/**
 * Result wrapper for metadata resolution that includes error information.
 */
@Immutable
@Serializable
sealed class MetadataResult {
  @Serializable
  data class Success(val metadata: TrackMetadata) : MetadataResult()

  @Serializable
  data class Error(
    val message: String,
    val code: String? = null,
  ) : MetadataResult()

  val isSuccess: Boolean get() = this is Success
  val isError: Boolean get() = this is Error

  fun getOrNull(): TrackMetadata? = (this as? Success)?.metadata
}
