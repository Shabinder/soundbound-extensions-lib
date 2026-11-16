package `in`.shabinder.soundbound.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Declares how the host must decrypt a stream it fetches. Extensions never see the bytes, so they only attach this
 * spec to a [Request]; the host runs the matching cipher natively, for playback (seekable, while streaming) and for
 * downloads.
 *
 * Open by design: [algorithm] is a free string token and [params] a string map, so a provider that reuses a known
 * cipher (or the same cipher with different chunking/IV) needs NO change to this library, only data. Check
 * `PlaybackCapabilities.ciphers` for what the running host supports. An unknown [algorithm] fails playback and
 * download explicitly; the host never plays or saves ciphertext.
 *
 * | [algorithm] | [key] | [params] |
 * |---|---|---|
 * | [AES_128_CTR] | 16-byte key, hex | [PARAM_IV_HEX] = initial 16-byte counter block (default all zero) |
 * | [AES_128_CBC] | 16-byte key, hex | [PARAM_IV_HEX] (default all zero), [PARAM_PADDING] = `none` (default) or `pkcs7` |
 * | [BLOWFISH_CBC_STRIPE] (alias [BLOWFISH_CBC]) | Blowfish key, hex | [PARAM_IV_HEX] (8 bytes), [PARAM_CHUNK_SIZE_BYTES] (default 2048), [PARAM_ENCRYPT_EVERY_NTH_CHUNK] (default 1) |
 * | [CLEARKEY_CENC] | reserved: no host implements it yet | |
 *
 * Every algorithm also takes [PARAM_SKIP_PLAIN_BYTES]: bytes dropped from the start of the decrypted output (a
 * vendor header in front of the real container).
 *
 * Example, a stripe cipher where every 3rd 2048-byte chunk is Blowfish/CBC/NoPadding encrypted with a fixed IV:
 * `DecryptionSpec("blowfish-cbc-stripe", key = <hex>, params = mapOf("ivHex" to "0001020304050607",
 * "chunkSizeBytes" to "2048", "encryptEveryNthChunk" to "3"))`.
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
