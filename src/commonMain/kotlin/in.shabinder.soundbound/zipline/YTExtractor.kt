package `in`.shabinder.soundbound.zipline

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.PoTokenResult

/*
* We desperately want this to be in our yt-extension core,
* making this multiplatform, and independent of the app.
* something to pick when have more bandwidth...
* */
interface YTExtractor : ZiplineService {
  suspend fun getSignatureTimestamp(videoId: String): Int
  suspend fun getStreamUrl(signatureCipher: String, videoId: String): String
  suspend fun getWebClientPoTokenOrNull(videoId: String, sessionId: String?): PoTokenResult?
}
