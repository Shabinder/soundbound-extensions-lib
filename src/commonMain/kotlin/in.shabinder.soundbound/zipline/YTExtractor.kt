package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService

interface YTExtractor : ZiplineService {
  suspend fun getSignatureTimestamp(videoId: String): Int
  suspend fun getStreamUrl(signatureCipher: String, videoId: String): String
}
