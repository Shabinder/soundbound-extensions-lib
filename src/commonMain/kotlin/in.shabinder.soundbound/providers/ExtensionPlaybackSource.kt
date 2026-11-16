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
     * calls it off the main thread, in chunk order per read, with a timeout and a few chunks of read-ahead.
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
}

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
