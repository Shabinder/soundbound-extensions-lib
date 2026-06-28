package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface Crypto : ZiplineService {
  fun desECBDecrypt(key: String, input: String): String

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
