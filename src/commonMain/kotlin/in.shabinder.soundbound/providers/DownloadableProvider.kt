package `in`.shabinder.soundbound.providers

import app.cash.zipline.ZiplineService
import `in`.shabinder.soundbound.models.DownloadQueryResult
import `in`.shabinder.soundbound.models.DownloadQueryResults
import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.SongModel

interface DownloadableProvider : QueryableProvider, ZiplineService {

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
    suspend fun findBestMatchURL(songModel: SongModel): DownloadQueryResults =
        findBestMatchURL(songModel.makeQueryParams())
}