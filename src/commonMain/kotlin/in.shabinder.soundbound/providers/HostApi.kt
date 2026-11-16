package `in`.shabinder.soundbound.providers

/**
 * The host API level this lib defines: an integer that goes up whenever a lib release adds or changes a host
 * service an extension can call. An extension built against this lib declares it as `min_host_api` in its repository
 * listing when it needs a service that older hosts lack; a host whose [LEVEL] is lower skips that extension and
 * reports it as needing a newer app instead of failing at call time.
 *
 * Levels: 40 = lib 0.40 (first level; source-neutral names, LyricsText, granular HostPermission). 41 = lib 0.41
 * (open playback: `PlaybackSourceDescriptor.Manifest`, `DirectHttp` expiry/container, `ExtensionPlaybackSource.refresh`,
 * host stream ciphers, `PlaybackHost.playbackCapabilities`, `DownloadRequest.processing` / `ProcessingSpec`,
 * `PlaybackCapabilities.processing`, `ExtensionPlaybackSource.transform`). Older hosts ignore `processing` and use the
 * raw bytes, so an extension that sets it MUST declare `min_host_api` >= 41 (see `ProcessingSpec`). Hosts before 40
 * do not read `min_host_api` and treat every listing as compatible. 42 = lib 0.42 (`PlaybackSourceDescriptor.DrmManifest`
 * played through the device's own DRM module, `PlaybackCapabilities.drmSchemes`); an extension that returns a
 * `DrmManifest` MUST declare `min_host_api` >= 42 and check `drmSchemes` is non-empty, else fall back.
 */
object HostApi {
    const val LEVEL: Int = 42
}
