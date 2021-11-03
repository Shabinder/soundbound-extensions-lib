package `in`.shabinder.soundbound.providers

import `in`.shabinder.soundbound.models.QueryParams
import `in`.shabinder.soundbound.models.TrackDetails

interface QueryableProvider<TrackEntity> : Provider {

    /*
    * Search this SOURCE for matches based on QueryParams and return found matches.
    *  - will return empty list if no matches found
    * */
    suspend fun search(queryParams: QueryParams): List<TrackEntity>

    /*
    * Return The Best Match from Provided TrackEntity
    *   Will throw `ProviderException.NoMatchFound` if queryResults is empty
    * */
    suspend fun sortByBestMatch(
        queryResults: List<TrackEntity>,
        queryParams: QueryParams
    ): TrackEntity

    /*
    * Function to map TrackEntity -> TrackDetails.
    * */
    fun TrackEntity.toTrackDetails(): TrackDetails
}