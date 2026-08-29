package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.AudioQuality
import `in`.shabinder.soundbound.models.DownloadQueryResults
import `in`.shabinder.soundbound.models.QualityOption
import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.SongModel

interface DownloadableProvider : QueryableProvider, ZiplineService {

    /**
     * Opens an ephemeral source for playback when the provider needs to retain transport state.
     *
     * Returning null preserves the existing [findBestMatchURL] path for providers that only
     * expose serializable download requests.
     */
    suspend fun openPlaybackSource(songModel: SongModel): ExtensionPlaybackSource? = null

    /*
    * The quality tiers this provider can deliver, best-first. The app shows these in the
    * per-download picker and passes the chosen one back via QueryParams.preferredQuality.
    * Default = the legacy lossy tiers, so existing providers need no change.
    * */
    suspend fun qualityOptions(): List<QualityOption> = listOf(
        QualityOption(AudioQuality.KBPS320, "320 kbps"),
        QualityOption(AudioQuality.KBPS256, "256 kbps"),
        QualityOption(AudioQuality.KBPS192, "192 kbps"),
        QualityOption(AudioQuality.KBPS128, "128 kbps"),
    )

    /*
    * The Provider Guarantees that TrackEntity has a method to return a download Link,
    *   so one can be assured tracks from this `Source` can be DOWNLOADED
    *
    * Can Throw DownloadLinkFetchFailed.
    * */
    // suspend fun getDownloadLink(entity: TrackEntity): DownloadQueryResult

    /*
    * Search and find the closest match for provided QueryParams
    * */
    suspend fun findBestMatchURL(
        queryParams: QueryParams
    ): DownloadQueryResults /*= sortByBestMatch(
        search(queryParams),
        queryParams
    ).let { bestMatch ->
        getDownloadLink(bestMatch)
    }*/

    /*
    * Search and find the closest match for provided TrackDetails
    * */
    suspend fun findBestMatchURL(songModel: SongModel) =
        findBestMatchURL(songModel.makeQueryParams())
}
