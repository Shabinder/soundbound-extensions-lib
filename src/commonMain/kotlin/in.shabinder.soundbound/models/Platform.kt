package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Enum representing all supported music streaming platforms.
 * Used for cross-platform link resolution and metadata providers.
 *
 * Note: Named MusicPlatform to avoid conflict with AppInfo.Platform (device platforms).
 */
@Immutable
@Serializable
enum class MusicPlatform(
  val displayName: String,
  val urlPatterns: List<String>,
) {
  SPOTIFY("Spotify", listOf("spotify.com", "open.spotify.com")),
  APPLE_MUSIC("Apple Music", listOf("music.apple.com", "itunes.apple.com")),
  YOUTUBE_MUSIC("YouTube Music", listOf("music.youtube.com")),
  YOUTUBE("YouTube", listOf("youtube.com", "youtu.be")),
  DEEZER("Deezer", listOf("deezer.com")),
  TIDAL("Tidal", listOf("tidal.com", "listen.tidal.com")),
  SOUNDCLOUD("SoundCloud", listOf("soundcloud.com", "on.soundcloud.com")),
  AMAZON_MUSIC("Amazon Music", listOf("music.amazon.com", "amazon.com/music")),
  PANDORA("Pandora", listOf("pandora.com")),
  AUDIOMACK("Audiomack", listOf("audiomack.com")),
  ANGHAMI("Anghami", listOf("anghami.com")),
  YANDEX("Yandex Music", listOf("music.yandex.com", "music.yandex.ru")),
  BANDCAMP("Bandcamp", listOf("bandcamp.com")),
  BOOMPLAY("Boomplay", listOf("boomplay.com")),
  NAPSTER("Napster", listOf("napster.com")),
  AUDIUS("Audius", listOf("audius.co")),
  GAANA("Gaana", listOf("gaana.com")),
  JIOSAAVN("JioSaavn", listOf("jiosaavn.com", "saavn.com")),
  SHAZAM("Shazam", listOf("shazam.com")),
  TIKTOK("TikTok", listOf("tiktok.com")),
  INSTAGRAM("Instagram", listOf("instagram.com")),
  QQ_MUSIC("QQ Music", listOf("y.qq.com", "qq.com/musicalbum")),
  MUSICBRAINZ("MusicBrainz", listOf("musicbrainz.org")),
  LASTFM("Last.fm", listOf("last.fm", "lastfm.com")),
  UNKNOWN("Unknown", emptyList());

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
