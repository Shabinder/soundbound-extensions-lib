package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.DownloadRequest
import kotlinx.serialization.Serializable

/**
 * Ephemeral, provider-neutral media source opened by an extension for one playback attempt.
 *
 * The app owns the native player and adapts [Stream] sources to local HTTP. The extension owns
 * every detail required to produce the bytes. Callers must close the service when playback is
 * replaced, fails, completes, or the provider is unloaded.
 */
interface ExtensionPlaybackSource : ZiplineService {
    suspend fun descriptor(): PlaybackSourceDescriptor

    /** Reads at most [maxBytes] from the absolute byte [offset]. Used only for [Stream]. */
    suspend fun read(offset: Long, maxBytes: Int): ByteArray

    /** Hints that the next reads will correspond to [positionMs]. Used only for seekable streams. */
    suspend fun seek(positionMs: Long)
}

@Serializable
sealed interface PlaybackSourceDescriptor {

    /** The native player can consume this request without an extension-driven byte stream. */
    @Serializable
    data class DirectHttp(
        val request: DownloadRequest,
    ) : PlaybackSourceDescriptor

    /** The app must expose this extension-owned byte stream through its local HTTP adapter. */
    @Serializable
    data class Stream(
        val contentLength: Long,
        val durationMs: Long,
        val mimeType: String,
        val seekable: Boolean = true,
    ) : PlaybackSourceDescriptor
}
