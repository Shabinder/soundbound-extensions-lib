package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface Crypto : ZiplineService {
  fun desECBDecrypt(key: String, input: String): String

  /**
   * AES/CBC/PKCS5Padding decrypt of a base64 [input], returning the UTF-8 plaintext.
   *
   * [key] and [iv] are used as their raw UTF-8 bytes, so a 16-character key/IV is AES-128 - this
   * matches how providers ship them (as ASCII string literals in their web bundles) rather than as
   * hex.
   */
  fun aesCBCDecrypt(key: String, iv: String, input: String): String

  /**
   * AES/ECB/NoPadding decrypt of a **hex-encoded** [input], returning UTF-8 plaintext.
   *
   * [key] is used as its raw UTF-8 bytes (a 16-char key is AES-128). Unlike [aesCBCDecrypt] this
   * takes hex (not base64) and applies no padding, so [input] must be block-aligned; any trailing
   * padding is the caller's to trim.
   */
  fun aesECBDecrypt(key: String, input: String): String

  fun encodeBase64(input: String): String

  fun decodeBase64(input: String): String

  fun sha1Hex(input: String): String

  /** MD5 of the UTF-8 bytes of [input], lower-case hex. */
  fun md5Hex(input: String): String

  /** SHA-256 of the UTF-8 bytes of [input], lower-case hex. */
  fun sha256Hex(input: String): String

  /** HMAC-SHA1 of [message] under [key] (both UTF-8 bytes), lower-case hex. */
  fun hmacSha1Hex(key: String, message: String): String

  /** HMAC-SHA256 of [message] under [key] (both UTF-8 bytes), lower-case hex. */
  fun hmacSha256Hex(key: String, message: String): String

  fun urlEncode(input: String): String

  fun urlDecode(input: String): String

  fun generateTotp(
    timeStamp: Long,
    secret: String,
  ): String
}
