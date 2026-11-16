package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.ProcessingCapabilities
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
  /** 0.40-style `DecryptionSpec.algorithm` preset ids the host accepts. */
  val ciphers: Set<String> = emptySet(),
  /** The native op pipeline (`Request.processing`): ops, algorithms, modes, IV specs, limits. */
  val processing: ProcessingCapabilities = ProcessingCapabilities(),
  /** Container MIME types the platform's player decodes ("audio/mpeg", "audio/mp4", "audio/flac", ...). */
  val containers: Set<String> = emptySet(),
  /** The host calls `ExtensionPlaybackSource.refresh()` on refusal / expiry. */
  val refresh: Boolean = false,
  /**
   * Since 42. Content-protection schemes the device plays through its own DRM module for a
   * `PlaybackSourceDescriptor.DrmManifest` (e.g. "widevine"); empty on a host without system DRM or before 42.
   */
  val drmSchemes: Set<String> = emptySet(),
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
    /** A `DrmManifest` played through the device's own DRM module (since 42). */
    const val KIND_DRM_MANIFEST = "drm-manifest"
  }
}

/** What an extension sees on a host that has no [PlaybackHost]. */
object UnavailablePlaybackHost : PlaybackHost {
  override suspend fun playbackCapabilities() = PlaybackCapabilities()
}
