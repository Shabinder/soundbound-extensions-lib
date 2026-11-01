package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.DownloadRequest
import `in`.shabinder.soundbound.providers.ExtensionPlaybackSource

/**
 * Host-owned content-decryption capability. Scheme identifiers are DRM-system UUIDs.
 * Availability means an explicitly configured backend exists, not that a service will
 * accept its identity or grant a license. Credentials, backend paths and keys stay host-side.
 *
 * **Flat (session-id) surface, deliberately.** Every call is a method on this one bound service and
 * a session is an opaque [String] id — NOT a returned child [ZiplineService]. A child service
 * returned from `open()` and then called back from inside the extension's in-flight `openPlaybackSource`
 * deadlocks the engine's single-thread Zipline dispatcher (the thread is busy awaiting the very
 * result the child call must produce). This mirrors the working [HeadlessBrowser] shape
 * (`newSession` -> id, `evaluate(id, ...)`).
 */
interface ContentDecryption : ZiplineService {
    suspend fun supportedSchemes(): List<String>

    /** Opens a session for [scheme]; returns an opaque session id, or null when unsupported. */
    suspend fun open(scheme: String): String?

    /** Optional service certificate; null resets it. The backend validates certificates. */
    suspend fun setServiceCertificate(sessionId: String, certificate: ByteArray?)

    suspend fun challenge(sessionId: String, initializationData: ByteArray): ByteArray
    suspend fun installLicense(sessionId: String, response: ByteArray)
    suspend fun isLicensed(sessionId: String): Boolean

    /**
     * Opens media using a host-held key selected by its public key identifier. The backend validates
     * supported containers and returns the existing generic source. Releasing the session
     * invalidates all its sources. No key bytes are returned.
     */
    suspend fun openMedia(
        sessionId: String,
        request: DownloadRequest,
        keyId: String,
        durationMs: Long,
        mimeType: String,
    ): ExtensionPlaybackSource

    /** Release a session and its sources. */
    suspend fun release(sessionId: String)
}

/** Default for hosts without an explicitly configured backend. */
object UnavailableContentDecryption : ContentDecryption {
    override suspend fun supportedSchemes(): List<String> = emptyList()
    override suspend fun open(scheme: String): String? = null
    override suspend fun setServiceCertificate(sessionId: String, certificate: ByteArray?) = Unit
    override suspend fun challenge(sessionId: String, initializationData: ByteArray): ByteArray =
        throw IllegalStateException("No content-decryption backend")
    override suspend fun installLicense(sessionId: String, response: ByteArray) =
        throw IllegalStateException("No content-decryption backend")
    override suspend fun isLicensed(sessionId: String): Boolean = false
    override suspend fun openMedia(
        sessionId: String,
        request: DownloadRequest,
        keyId: String,
        durationMs: Long,
        mimeType: String,
    ): ExtensionPlaybackSource = throw IllegalStateException("No content-decryption backend")
    override suspend fun release(sessionId: String) = Unit
}
