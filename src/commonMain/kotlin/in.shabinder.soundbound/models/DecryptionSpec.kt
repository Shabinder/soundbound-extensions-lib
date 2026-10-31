package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Declares how the app-side downloader must decrypt a stream after fetching its bytes.
 * Extensions never see the bytes, so they only attach this spec to a [Request]; the
 * DownloadManager runs the matching cipher.
 *
 * Open by design: [algorithm] is a free string token and [params] a string map, so a new
 * provider that reuses a known cipher (or the same cipher with different chunking/IV) needs
 * NO change to this library — only data. A genuinely new cipher still needs an app-side
 * handler (native crypto cannot run in the extension sandbox), but the contract is stable.
 * An unknown [algorithm] must fail the download explicitly, never ship ciphertext as-is.
 *
 * Worked example — Deezer: every 3rd 2048-byte chunk is Blowfish/CBC/NoPadding encrypted:
 * `DecryptionSpec("blowfish-cbc", key = <hex>, params = { ivHex: "0001020304050607",
 * chunkSizeBytes: "2048", encryptEveryNthChunk: "3" })`.
 */
@Immutable
@Serializable
data class DecryptionSpec(
    val algorithm: String,
    val key: String,
    val params: Map<String, String> = emptyMap(),
) {
    fun param(key: String): String? = params[key]

    companion object {
        const val BLOWFISH_CBC = "blowfish-cbc"

        /**
         * CENC / ISO Common Encryption, AES-128-CTR (`cenc` scheme). The host demuxes the
         * fragmented-MP4, CTR-decrypts each sample with [key] (hex content key the extension
         * obtained — e.g. via a Widevine license exchange), strips protection boxes and remuxes.
         * Runs in commonMain (cryptography-kotlin) on every platform, needing no OS CDM.
         */
        const val CENC = "cenc"

        const val PARAM_IV_HEX = "ivHex"
        const val PARAM_CHUNK_SIZE_BYTES = "chunkSizeBytes"
        const val PARAM_ENCRYPT_EVERY_NTH_CHUNK = "encryptEveryNthChunk"
    }
}
