package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Enum representing all supported music streaming platforms.
 * Used for cross-platform link resolution and metadata providers.
 *
 * Note: Named MusicPlatform to avoid conflict with AppInfo.Platform (device platforms).
 *
 * @param downloadPriority Priority for download resolution (lower = higher priority).
 *        Platforms with supported download providers should have lower values.
 *        Default is 50 for platforms without direct download support.
 */
@Immutable
@Serializable
enum class MusicPlatform(
  val displayName: String,
  val urlPatterns: List<String>,
  val downloadPriority: Int = 50, // Default priority for unsupported platforms
) {
  // High priority - platforms with reliable download support
  JIOSAAVN("JioSaavn", listOf("jiosaavn.com", "saavn.com"), 1),
  GAANA("Gaana", listOf("gaana.com"), 2),
  YOUTUBE_MUSIC("YouTube Music", listOf("music.youtube.com"), 3),
  YOUTUBE("YouTube", listOf("youtube.com", "youtu.be"), 4),
  SOUNDCLOUD("SoundCloud", listOf("soundcloud.com", "on.soundcloud.com"), 5),

  // Medium priority - platforms that may have download support
  SPOTIFY("Spotify", listOf("spotify.com", "open.spotify.com"), 10),
  APPLE_MUSIC("Apple Music", listOf("music.apple.com", "itunes.apple.com"), 11),
  DEEZER("Deezer", listOf("deezer.com"), 12),
  TIDAL("Tidal", listOf("tidal.com", "listen.tidal.com"), 13),
  AMAZON_MUSIC("Amazon Music", listOf("music.amazon.com", "amazon.com/music"), 14),
  YANDEX("Yandex Music", listOf("music.yandex.com", "music.yandex.ru"), 15),

  // Lower priority - limited or no download support
  BANDCAMP("Bandcamp", listOf("bandcamp.com"), 20),
  AUDIOMACK("Audiomack", listOf("audiomack.com"), 21),
  BOOMPLAY("Boomplay", listOf("boomplay.com"), 22),
  NAPSTER("Napster", listOf("napster.com"), 23),
  AUDIUS("Audius", listOf("audius.co"), 24),
  ANGHAMI("Anghami", listOf("anghami.com"), 25),
  PANDORA("Pandora", listOf("pandora.com"), 26),

  // Metadata-only platforms (no download support)
  SHAZAM("Shazam", listOf("shazam.com"), 90),
  TIKTOK("TikTok", listOf("tiktok.com"), 91),
  INSTAGRAM("Instagram", listOf("instagram.com"), 92),
  QQ_MUSIC("QQ Music", listOf("y.qq.com", "qq.com/musicalbum"), 93),
  MUSICBRAINZ("MusicBrainz", listOf("musicbrainz.org"), 94),
  LASTFM("Last.fm", listOf("last.fm", "lastfm.com"), 95),
  UNKNOWN("Unknown", emptyList(), 99);

  companion object {
    /**
     * Determines the platform from a given URL.
     * @param url The URL to analyze
     * @return The matching MusicPlatform, or UNKNOWN if no match found
     */
    fun fromUrl(url: String): MusicPlatform {
      val lowercaseUrl = url.lowercase()
      return entries.find { platform ->
        platform.urlPatterns.any { pattern ->
          lowercaseUrl.contains(pattern)
        }
      } ?: UNKNOWN
    }

    /**
     * Get platform by its name (case-insensitive).
     * Useful for parsing API responses with platform names as strings.
     */
    fun fromName(name: String): MusicPlatform {
      val lowercaseName = name.lowercase().replace("_", "").replace(" ", "")
      return entries.find { platform ->
        platform.name.lowercase().replace("_", "") == lowercaseName ||
          platform.displayName.lowercase().replace(" ", "") == lowercaseName
      } ?: UNKNOWN
    }
  }
}
