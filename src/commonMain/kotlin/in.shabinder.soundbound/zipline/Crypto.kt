package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface Crypto : ZiplineService {
    fun desECBDecrypt(key: String, input: String): String

    fun encodeBase64(input: String): String

    fun decodeBase64(input: String): String

    fun sha1Hex(input: String): String

    fun urlEncode(input: String): String

    fun urlDecode(input: String): String
}