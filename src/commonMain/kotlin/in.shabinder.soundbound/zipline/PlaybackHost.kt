package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

/**
 * Since [in.shabinder.soundbound.providers.HostApi.LEVEL] 41: what this host's playback can do, so an extension picks
 * the best [in.shabinder.soundbound.providers.PlaybackSourceDescriptor] the running app supports and degrades on older
 * ones (for example: host-side `aes-128-ctr` when listed, else decrypt in the extension and return a `Stream`).
 *
 * A host before 41 does not bind this service; the extension-side guard then reports the empty [PlaybackCapabilities],
 * meaning "DirectHttp and Stream only, no host ciphers, no refresh".
 */
interface PlaybackHost : ZiplineService {
  suspend fun playbackCapabilities(): PlaybackCapabilities
}

@Serializable
data class PlaybackCapabilities(
  /** Descriptor kinds the host plays: [KIND_DIRECT_HTTP], [KIND_STREAM], [KIND_SEGMENTS], [KIND_HLS], [KIND_DASH]. */
  val sourceKinds: Set<String> = emptySet(),
  /** `DecryptionSpec.algorithm` ids the host decrypts while playing and downloading. */
  val ciphers: Set<String> = emptySet(),
  /** Container MIME types the platform's player decodes ("audio/mpeg", "audio/mp4", "audio/flac", ...). */
  val containers: Set<String> = emptySet(),
  /** The host calls `ExtensionPlaybackSource.refresh()` on refusal / expiry. */
  val refresh: Boolean = false,
  /** Request headers reach the player on this platform (through the host, never the player itself). */
  val headers: Boolean = false,
  /** Extension sources the host keeps open at once (current + next for gapless). */
  val maxLiveSources: Int = 1,
) {
  companion object {
    const val KIND_DIRECT_HTTP = "direct-http"
    const val KIND_STREAM = "stream"
    /** `DirectHttp` whose `Request.segments` / `initSegment` are played in order. */
    const val KIND_SEGMENTS = "segments"
    const val KIND_HLS = "manifest-hls"
    const val KIND_DASH = "manifest-dash"
  }
}

/** What an extension sees on a host that has no [PlaybackHost]. */
object UnavailablePlaybackHost : PlaybackHost {
  override suspend fun playbackCapabilities() = PlaybackCapabilities()
}
