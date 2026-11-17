package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.DownloadRequest
import kotlinx.serialization.Serializable

/**
 * Ephemeral, provider-neutral media source opened by an extension for one playback attempt.
 *
 * The app owns the native player. The extension describes the bytes ([descriptor]) and, for [PlaybackSourceDescriptor.Stream],
 * produces them ([read]). Everything the host can do natively (ranged HTTP, headers, decryption, manifests, URL refresh)
 * stays data in the descriptor, so a new source needs an extension update, not an app release.
 *
 * Callers must close the service when playback is replaced, fails, completes, or the provider is unloaded.
 */
interface ExtensionPlaybackSource : ZiplineService {
    suspend fun descriptor(): PlaybackSourceDescriptor

    /** Reads at most [maxBytes] from the absolute byte [offset]. Used only for [PlaybackSourceDescriptor.Stream]. */
    suspend fun read(offset: Long, maxBytes: Int): ByteArray

    /** Hints that the next reads will correspond to [positionMs]. Used only for seekable streams. */
    suspend fun seek(positionMs: Long)

    /**
     * Since [HostApi.LEVEL] 41. The host calls this when an upstream request is refused (HTTP 401/403/410) or the
     * descriptor's `expiresAtEpochMs` has passed. Return a fresh descriptor for the SAME file (same bytes, same length,
     * new signature/token), or null when it cannot be renewed. The host calls it at most once per failed request and
     * fails the read if the renewed URL is refused too. Hosts before 41 never call it.
     */
    suspend fun refresh(): PlaybackSourceDescriptor? = null

    /**
     * Since [HostApi.LEVEL] 41. Called for each chunk of a `ProcessOp.ExtensionTransform(chunkSize, id)` in the request's
     * processing pipeline: return the processed bytes for chunk [chunkIndex] (any length; the host concatenates). The host
     * calls it off the main thread, in chunk order per read, with a timeout and a few chunks of read-ahead. It runs in
     * playback and progressive (`DirectHttp`) downloads; a DASH download with a transform fails cleanly. Hosts before 41
     * never call it.
     */
    suspend fun transform(id: String, chunkIndex: Long, bytes: ByteArray): ByteArray =
        throw UnsupportedOperationException("transform '$id' not implemented")
}

@Serializable
sealed interface PlaybackSourceDescriptor {

    /**
     * A progressive file over HTTP(S). The host honours [DownloadRequest.headers] on every platform, runs
     * [DownloadRequest.processing] (or the 0.40 [DownloadRequest.decryption] preset) while streaming, seekable where the
     * ops allow, and plays [DownloadRequest.segments] in order.
     *
     * @param expiresAtEpochMs when the URL stops working; the host calls [ExtensionPlaybackSource.refresh] before
     *   that (since 41). Null = unknown, refresh only on refusal.
     * @param container MIME type hint ("audio/flac", "audio/mpeg", "audio/mp4", "audio/ogg", ...). The host still
     *   sniffs the bytes; this only avoids a wrong guess when the server's Content-Type is generic.
     */
    @Serializable
    data class DirectHttp(
        val request: DownloadRequest,
        val expiresAtEpochMs: Long? = null,
        val container: String? = null,
    ) : PlaybackSourceDescriptor

    /** The app must expose this extension-owned byte stream through its local HTTP adapter. */
    @Serializable
    data class Stream(
        val contentLength: Long,
        val durationMs: Long,
        val mimeType: String,
        val seekable: Boolean = true,
    ) : PlaybackSourceDescriptor

    /**
     * Since [HostApi.LEVEL] 41. An HLS playlist (media or master) or a DASH MPD at [request]'s URL. The host fetches the
     * manifest and its children with [headerPolicy], so native players never need the headers themselves.
     *
     * HLS `#EXT-X-KEY:METHOD=AES-128` keys are fetched from their URI as usual. When the extension already holds the
     * key, set [DownloadRequest.decryption] to `aes-128-cbc` (or [DownloadRequest.processing] to `ProcessingSpec.aesCbc`)
     * with that key and the host answers key requests with it.
     *
     * DASH: the host plays one audio representation (the highest bandwidth) from a `SegmentTemplate` (with or
     * without `SegmentTimeline`) or a `SegmentList`. Check [PlaybackCapabilities.sourceKinds] first; a host before 41
     * fails to decode this type and falls back to the provider's plain download request.
     */
    @Serializable
    data class Manifest(
        val request: DownloadRequest,
        val kind: ManifestKind,
        val headerPolicy: HeaderPolicy = HeaderPolicy.SAME_HOST,
        val expiresAtEpochMs: Long? = null,
    ) : PlaybackSourceDescriptor

    /**
     * Since [HostApi.LEVEL] 42. A DRM-protected HLS playlist or DASH MPD whose content key stays inside the
     * platform's own content-protection module (Android `MediaDrm`): the extension supplies the manifest and the
     * license acquisition, and the host plays it through the device DRM. The decrypted key never reaches the host
     * or the extension — the host only relays the opaque license challenge/response. There is no host-side cipher
     * and no bytes pass through the loopback bridge.
     *
     * Use this only for a source the user is entitled to under their own account on the provider's own service:
     * [licenseRequest] carries that account's tokens (e.g. an Apple Music `media-user-token` + developer JWT) to
     * the provider's own [DrmLicense.serverUrl]. The host does not provision or bundle any module; a device without
     * [scheme] support fails to decode and the extension must fall back (check [PlaybackCapabilities.drmSchemes]).
     *
     * @param request the manifest URL (+ headers the host applies per [headerPolicy]) for the protected stream.
     * @param license how to obtain the content licence from the provider.
     * @param scheme the content-protection scheme the stream uses; see [DrmScheme].
     * @param container when non-null (e.g. "audio/mp4"), [request]'s URL is NOT a manifest but a single bare
     *   CENC-protected progressive file of this MIME type, whose scheme init data (the Widevine `pssh` box) is already
     *   inside the file's own `moov`. The host plays it as a progressive source and lets the device DRM read that
     *   in-stream PSSH — [kind] is ignored. Null = the HLS/DASH manifest at [request] (the original behaviour). This
     *   covers providers (e.g. Spotify) that serve a fragmented-MP4 with an embedded `pssh` and no HLS/DASH wrapper.
     */
    @Serializable
    data class DrmManifest(
        val request: DownloadRequest,
        val kind: ManifestKind,
        val license: DrmLicense,
        val scheme: DrmScheme = DrmScheme.WIDEVINE,
        val headerPolicy: HeaderPolicy = HeaderPolicy.SAME_HOST,
        val expiresAtEpochMs: Long? = null,
        val container: String? = null,
    ) : PlaybackSourceDescriptor
}

/**
 * How the host obtains a DRM content licence for a [PlaybackSourceDescriptor.DrmManifest]. The host POSTs the
 * platform-generated, opaque key request to [serverUrl] with [headers] and returns the opaque response to the
 * platform DRM; it never inspects or stores the key. [requestTransform] names an optional
 * [ExtensionPlaybackSource.transform] id for providers that wrap the challenge body (the host passes the raw
 * challenge as the chunk bytes and sends back what the extension returns).
 */
@Serializable
data class DrmLicense(
    val serverUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val requestTransform: String? = null,
    val responseTransform: String? = null,
)

@Serializable
enum class DrmScheme { WIDEVINE, PLAYREADY, CLEARKEY }

@Serializable
enum class ManifestKind { HLS, DASH }

/** Which of a manifest's requests carry [DownloadRequest.headers]. */
@Serializable
enum class HeaderPolicy {
    /** Only requests to the manifest's own host (playlists, keys); third-party segment CDNs never see auth headers. */
    SAME_HOST,

    /** Every request, including segments on other hosts (CDNs that need the same auth). */
    ALL_REQUESTS,
}
