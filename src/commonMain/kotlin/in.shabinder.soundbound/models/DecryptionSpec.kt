package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * The lib 0.40 way to ask the host to decrypt: a cipher id plus params. Still accepted; since 0.41 each id is a preset
 * that the host compiles to a [ProcessingSpec] (see [Request.processing], the open form). An unknown [algorithm] fails
 * playback and download explicitly; the host never plays or saves ciphertext.
 *
 * | [algorithm] | same as |
 * |---|---|
 * | [AES_128_CTR] | `ProcessingSpec.aesCtr(key, ivHex)` |
 * | [AES_128_CBC] | `ProcessingSpec.aesCbc(key, ivHex, pkcs7 = padding == "pkcs7")` |
 * | [BLOWFISH_CBC_STRIPE] / [BLOWFISH_CBC] | `ProcessingSpec.blowfishStripe(key, ivHex, chunkSizeBytes, encryptEveryNthChunk)` |
 *
 * [PARAM_SKIP_PLAIN_BYTES] adds a `ProcessOp.Skip`. [CLEARKEY_CENC] is reserved.
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
        /** Lib 0.40 id of [BLOWFISH_CBC_STRIPE]; still accepted. */
        const val BLOWFISH_CBC = "blowfish-cbc"
        const val BLOWFISH_CBC_STRIPE = "blowfish-cbc-stripe"
        const val AES_128_CTR = "aes-128-ctr"
        const val AES_128_CBC = "aes-128-cbc"

        /** Reserved for fragmented-MP4 Common Encryption with a known key. Not implemented by any host yet. */
        const val CLEARKEY_CENC = "clearkey-cenc"

        const val PARAM_IV_HEX = "ivHex"
        const val PARAM_CHUNK_SIZE_BYTES = "chunkSizeBytes"
        const val PARAM_ENCRYPT_EVERY_NTH_CHUNK = "encryptEveryNthChunk"
        const val PARAM_PADDING = "padding"
        const val PARAM_SKIP_PLAIN_BYTES = "skipPlainBytes"
    }
}
