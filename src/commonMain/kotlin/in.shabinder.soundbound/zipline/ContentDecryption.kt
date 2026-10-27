package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.DownloadRequest
import `in`.shabinder.soundbound.providers.ExtensionPlaybackSource

/**
 * Host-owned content-decryption capability. Scheme identifiers are DRM-system UUIDs.
 * Availability means an explicitly configured backend exists, not that a service will
 * accept its identity or grant a license. Credentials, backend paths and keys stay host-side.
 */
interface ContentDecryption : ZiplineService {
    suspend fun supportedSchemes(): List<String>

    /** Returns null when this host cannot open the requested scheme. */
    suspend fun open(scheme: String): ContentDecryptionSession?
}

/** One host session; license HTTP and provider authorization remain extension-owned. */
interface ContentDecryptionSession : ZiplineService {
    /** Optional service certificate; null resets it. The backend validates certificates. */
    suspend fun setServiceCertificate(certificate: ByteArray?)

    suspend fun challenge(initializationData: ByteArray): ByteArray
    suspend fun installLicense(response: ByteArray)
    suspend fun isLicensed(): Boolean

    /**
     * Opens media using a host-held key selected by its public key identifier.
     * The backend validates supported containers and returns the existing generic source.
     * Closing/releasing the session invalidates all its sources. No key bytes are returned.
     */
    suspend fun openMedia(
        request: DownloadRequest,
        keyId: String,
        durationMs: Long,
        mimeType: String,
    ): ExtensionPlaybackSource

    /** Await release when possible; synchronous close also schedules host-owned cleanup. */
    suspend fun release()
}

/** Default for hosts without an explicitly configured backend. */
object UnavailableContentDecryption : ContentDecryption {
    override suspend fun supportedSchemes(): List<String> = emptyList()
    override suspend fun open(scheme: String): ContentDecryptionSession? = null
}
