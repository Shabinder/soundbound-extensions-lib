package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.TrackDetails
import `in`.shabinder.soundbound.models.makeQueryParams

abstract class DownloadableProvider<TrackEntity>(dependencies: Dependencies) : QueryableProvider<TrackEntity>(dependencies) {

    /*
    * The Provider Guarantees that TrackEntity has a method to return a download Link,
    *   so one can be assured tracks from this `Source` can be DOWNLOADED
    *
    * Can Throw DownloadLinkFetchFailed.
    * */
    abstract suspend fun TrackEntity.getDownloadLink(): TrackDetails.DownloadQueryResult

    /*
    * Search and find the closest match for provided QueryParams
    * */
    open suspend fun findBestMatchURL(
        queryParams: QueryParams
    ): TrackDetails.DownloadQueryResult = sortByBestMatch(
        search(queryParams),
        queryParams
    ).getDownloadLink()

    /*
    * Search and find the closest match for provided TrackDetails
    * */
    suspend fun findBestMatchURL(trackDetails: TrackDetails) = findBestMatchURL(trackDetails.makeQueryParams())
}